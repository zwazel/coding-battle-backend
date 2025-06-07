package dev.zwazel.model.language;

import dev.zwazel.model.language.compilers.RustToWasmCompiler;
import lombok.NonNull;

public record Language(@NonNull String code, @NonNull LanguageType type) {
    public byte[] compile() {
        return type.compiler.compile(code);
    }

    private enum LanguageType {
        RUST(RustToWasmCompiler::compile);

        private final LanguageToWASMCompilerInterface compiler;

        LanguageType(LanguageToWASMCompilerInterface compiler) {
            this.compiler = compiler;
        }
    }
}
