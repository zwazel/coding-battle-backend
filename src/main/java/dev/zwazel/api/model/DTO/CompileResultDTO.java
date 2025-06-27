package dev.zwazel.api.model.DTO;

import org.springframework.http.HttpStatus;

public record CompileResultDTO(
        HttpStatus status,
        String message,
        String wasmPath,
        String buildLog
) {
}
