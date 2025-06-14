package dev.zwazel.model.language;

import dev.zwazel.DTO.CompileResultDTO;
import dev.zwazel.model.language.compilers.RustToWasmCompiler;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

@Getter
public enum Language {
    RUST(
            // create a *fresh* compiler for every invocation (safer if it holds state)
            new RustToWasmCompiler(),
            "rs"
    );

    /**
     * Warp‑compiler function (a tri‑argument lambda).
     */
    private final LanguageToWASMCompilerInterface compiler;

    /**
     * File extension for nosy man‑things to recognise source files.
     */
    private final String fileExtension;

    Language(LanguageToWASMCompilerInterface compiler, String fileExtension) {
        this.compiler = compiler;
        this.fileExtension = fileExtension;
    }

    /**
     * Delegates to the captured lambda.
     */
    public CompileResultDTO compile(String botName,
                                    Path storagePath,
                                    MultipartFile sourceFile)
            throws IOException, InterruptedException {
        return compiler.compile(botName, storagePath, sourceFile);
    }
}
