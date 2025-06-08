package dev.zwazel.DTO;

import org.springframework.http.HttpStatus;

public record CompileResultDTO(
        HttpStatus status,
        String message,
        String wasmPath,
        String buildLog
) {
}
