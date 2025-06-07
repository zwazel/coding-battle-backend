package dev.zwazel.controller;

import dev.zwazel.model.language.Language;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usercode")
public class UserCodeController {
    @PostMapping
    public ResponseEntity<String> uploadCodeAndCompileToWasm(@RequestBody Language language) {
        // This is a placeholder for the actual implementation.
        // In a real application, you would handle the code upload and compilation here.
        System.out.println("Received language code: " + language);
        byte[] wasmBinary = language.compile();
        // Here you would typically save the wasmBinary to a file or database,
        System.out.println("Compiled WASM binary size: " + wasmBinary.length);

        return ResponseEntity.ok("Code uploaded and compiled successfully.");
    }
}
