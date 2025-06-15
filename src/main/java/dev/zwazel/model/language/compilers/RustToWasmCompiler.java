package dev.zwazel.model.language.compilers;

import dev.zwazel.api.model.DTO.CompileResultDTO;
import dev.zwazel.model.language.LanguageToWASMCompilerInterface;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

public class RustToWasmCompiler implements LanguageToWASMCompilerInterface {
    @Override
    public CompileResultDTO compile(
            String botName,
            Path storagePath, // Where the compiled WASM will be stored, is expected to already exist
            MultipartFile file
    ) throws IOException, InterruptedException {
        // 1. Temp workspace
        Path workDir = Files.createTempDirectory("bot-" + botName + "-");
        Path srcDir = workDir.resolve("src");
        Files.createDirectories(srcDir);

        // 2. Write Cargo template --------------------------------------
        Files.writeString(workDir.resolve("Cargo.toml"), cargoToml(botName));
        Path libPath = srcDir.resolve("lib.rs");
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, libPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // 3. Docker build ---------------------------------------------
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "run", "--rm",
                "-v", workDir.toAbsolutePath() + ":/code",
                "-w", "/code",
                "rustlang/rust:nightly-alpine",
                "sh", "-c",
                "rustup target add wasm32-unknown-unknown && " +
                        "cargo build --release --target wasm32-unknown-unknown --offline"
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String buildLog = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        boolean ok = p.waitFor(120, TimeUnit.SECONDS) && p.exitValue() == 0;

        // 4. Move artifact if success ---------------------------------
        if (ok) {
            // Cargo names the file after the crate (snake-case). Reconstruct the path:
            Path wasmSrc = workDir.resolve(
                    "target/wasm32-unknown-unknown/release/"
                            + botName.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase()
                            + ".wasm");

            // destination = <storagePath>/<botname-lower>.wasm
            String sanitized = botName.toLowerCase().replaceAll("[^a-z0-9]", "_");
            Path wasmDest = storagePath.resolve(sanitized + ".wasm");

            Files.move(wasmSrc, wasmDest, StandardCopyOption.REPLACE_EXISTING);

            return new CompileResultDTO(HttpStatus.OK,
                    "Success—warp-lightning ready!",
                    wasmDest.toString(),
                    LanguageToWASMCompilerInterface.truncate(buildLog));
        }

        return new CompileResultDTO(HttpStatus.BAD_REQUEST,
                "Compilation failed, man-thing!",
                null, LanguageToWASMCompilerInterface.truncate(buildLog));
    }

    private String cargoToml(String botName) {
        return """
                [package]
                name = "%s"
                version = "0.1.0"
                edition = "2021"
                
                [lib]
                crate-type = ["cdylib"]
                
                [dependencies]
                """.formatted(botName.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_"));
    }
}
