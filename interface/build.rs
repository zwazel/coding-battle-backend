use std::process::Command;

fn main() {
    // Generate host bindings (Rust) for the `.wit` interface
    Command::new("wit-bindgen")
        .args(&[
            "rust",
            "--import",
            "interface/my_interface.wit",
            "--out-dir",
            "src/bindings_host",
        ])
        .status()
        .unwrap();

    // Generate guest bindings (Rust) for the `.wit` interface
    Command::new("wit-bindgen")
        .args(&[
            "rust",
            "--export",
            "interface/my_interface.wit",
            "--out-dir",
            "src/bindings_guest",
        ])
        .status()
        .unwrap();

    // Because `build.rs` can re-run multiple times, you might want
    // to watch changes in your `.wit` file:
    println!("cargo:rerun-if-changed=my_interface.wit");
}
