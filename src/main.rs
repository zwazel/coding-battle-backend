#[macro_use]
extern crate rocket;

use rocket::form::Form;
use rocket::fs::TempFile;
use rocket::serde::json::Json;
use std::process::Command;

#[derive(FromForm)]
struct CodeUpload<'r> {
    file: TempFile<'r>, // Temporary file to store the uploaded script
}

#[post("/upload", data = "<code_upload>")]
async fn upload(mut code_upload: Form<CodeUpload<'_>>) -> Json<Result<String, String>> {
    // Save the file to a temp path
    let temp_dir = std::env::temp_dir();
    let file_path = temp_dir.join("uploaded_script.py");

    if let Err(err) = code_upload.file.persist_to(&file_path).await {
        return Json(Err(format!("Failed to save the file: {}", err)));
    }

    // Execute the uploaded file
    let output = Command::new("python").arg(&file_path).output();

    match output {
        Ok(output) => {
            let stdout = String::from_utf8_lossy(&output.stdout);
            let stderr = String::from_utf8_lossy(&output.stderr);

            if !stderr.is_empty() {
                Json(Err(format!("Error: {}", stderr)))
            } else {
                Json(Ok(stdout.to_string()))
            }
        }
        Err(_) => Json(Err("Failed to execute the file.".to_string())),
    }
}

#[launch]
fn rocket() -> _ {
    rocket::build().mount("/", routes![upload])
}
