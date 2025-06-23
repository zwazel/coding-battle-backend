package dev.zwazel.language;

import dev.zwazel.api.model.DTO.CompileResultDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

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
    CompileResultDTO compile(
            String botName,
            Path storagePath,
            MultipartFile file
    ) throws IOException, InterruptedException;
}
