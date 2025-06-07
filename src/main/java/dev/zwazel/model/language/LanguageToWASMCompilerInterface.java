package dev.zwazel.model.language;

import dev.zwazel.DTO.CompileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@FunctionalInterface
public interface LanguageToWASMCompilerInterface {
    static String truncate(String s) {
        return s.length() > 4096 ? s.substring(0, 4096) + "\n…truncated…" : s;
    }

    /**
     * Compiles the given code to WebAssembly (WASM).
     *
     * @param file The source file containing the code to compile.
     * @return The compiled WebAssembly binary as a byte array.
     */
    CompileResponse compile(MultipartFile file) throws IOException, InterruptedException;
}
