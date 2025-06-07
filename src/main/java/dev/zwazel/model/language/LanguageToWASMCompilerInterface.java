package dev.zwazel.model.language;

@FunctionalInterface
public interface LanguageToWASMCompilerInterface {
    /**
     * Compiles the given code to WebAssembly (WASM).
     *
     * @param code The source code to compile.
     * @return The compiled WebAssembly binary as a byte array.
     */
    byte[] compile(String code);
}
