package dev.zwazel.model.language;

import dev.zwazel.DTO.CompileResponse;
import dev.zwazel.model.language.compilers.RustToWasmCompiler;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public enum Language {
    RUST(new RustToWasmCompiler());

    private final LanguageToWASMCompilerInterface compiler;

    Language(LanguageToWASMCompilerInterface compiler) {
        this.compiler = compiler;
    }

    public CompileResponse compile(MultipartFile sourceFile) throws IOException, InterruptedException {
        return compiler.compile(sourceFile);
    }
}
