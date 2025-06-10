package dev.zwazel.model.language;

import dev.zwazel.DTO.CompileResultDTO;
import dev.zwazel.model.language.compilers.RustToWasmCompiler;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

@Getter
public enum Language {
    RUST(new RustToWasmCompiler(), "rs"),
    ;

    private final LanguageToWASMCompilerInterface compiler;

    /// The file extension used for this language's source files
    /// For example, "rs" for Rust
    private final String fileExtension;

    Language(LanguageToWASMCompilerInterface compiler, String fileExtension) {
        this.compiler = compiler;
        this.fileExtension = fileExtension;
    }

    public CompileResultDTO compile(String botName, Path storagePath, MultipartFile sourceFile) throws IOException, InterruptedException {
        return compiler.compile(botName, storagePath, sourceFile);
    }
}
