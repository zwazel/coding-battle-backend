package dev.zwazel.DTO;

import org.springframework.http.HttpStatus;

public record CompileResponse(
        HttpStatus status,
        String message,
        String wasmPath,
        String buildLog
) {
}
