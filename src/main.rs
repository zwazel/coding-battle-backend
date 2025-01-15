#[macro_use]
extern crate rocket;

use rocket::{fs::TempFile, serde::json::Json};
use serde::Serialize;
use std::process::{Command, Stdio};

#[derive(Debug, Serialize)]
struct GameState {
    tick: u32,
    player_position: (i32, i32),
}

#[derive(Debug, Serialize)]
struct SimulationResult {
    final_state: GameState,
    error: Option<String>,
}

#[post("/simulate", data = "<file>")]
async fn simulate_game(mut file: TempFile<'_>) -> Json<SimulationResult> {
    // Save the uploaded Python file to a temporary location
    let temp_dir = std::env::temp_dir();
    let file_path = temp_dir.join("uploaded_script.py");
    if let Err(_) = file.persist_to(&file_path).await {
        return Json(SimulationResult {
            final_state: GameState {
                tick: 0,
                player_position: (0, 0),
            },
            error: Some("Failed to save the Python file".to_string()),
        });
    }

    // Initialize game state
    let mut game_state = GameState {
        tick: 0,
        player_position: (0, 0),
    };

    let num_ticks = 10; // Number of ticks to simulate
    for tick in 1..=num_ticks {
        game_state.tick = tick;

        // Build the Python script to call the bot's `play` function
        let python_script = format!(
            r#"
import importlib.util

spec = importlib.util.spec_from_file_location("bot", "{}")
bot = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bot)

action = bot.play({{"tick": {}, "player_position": {}}})
print(action)
"#,
            file_path.to_string_lossy(),
            game_state.tick,
            format!("{:?}", game_state.player_position)
        );

        // Execute the Python script
        let output = Command::new("python")
            .arg("-c")
            .arg(python_script)
            .stdout(Stdio::piped())
            .stderr(Stdio::piped())
            .output();

        match output {
            Ok(output) => {
                let stdout = String::from_utf8_lossy(&output.stdout);
                println!("Output: {}", stdout);
                if let Ok((move_x, move_y)) = parse_action(&stdout) {
                    game_state.player_position.0 += move_x;
                    game_state.player_position.1 += move_y;
                } else {
                    return Json(SimulationResult {
                        final_state: game_state,
                        error: Some(format!("Invalid bot output: {}", stdout.trim())),
                    });
                }
            }
            Err(_) => {
                return Json(SimulationResult {
                    final_state: game_state,
                    error: Some("Failed to run the Python script".to_string()),
                });
            }
        }
    }

    // Return the final game state after all ticks
    Json(SimulationResult {
        final_state: game_state,
        error: None,
    })
}

// Parse the Python bot's action output (expects "(x, y)" format)
fn parse_action(output: &str) -> Result<(i32, i32), ()> {
    let stripped = output.trim();
    if let Some((x, y)) = stripped
        .strip_prefix("(")
        .and_then(|s| s.strip_suffix(")"))
        .and_then(|s| {
            let parts: Vec<&str> = s.split(',').collect();
            if parts.len() == 2 {
                Some((parts[0].trim().parse().ok()?, parts[1].trim().parse().ok()?))
            } else {
                None
            }
        })
    {
        Ok((x, y))
    } else {
        Err(())
    }
}

#[launch]
fn rocket() -> _ {
    rocket::build().mount("/", routes![simulate_game])
}
