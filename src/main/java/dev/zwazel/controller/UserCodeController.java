package dev.zwazel.controller;

import dev.zwazel.DTO.CompileResponse;
import dev.zwazel.model.language.Language;
import dev.zwazel.service.UserCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/usercode")
public class UserCodeController {
    private final UserCodeService userCodeService;

    @PostMapping
    public ResponseEntity<CompileResponse> uploadCodeAndCompileToWasm(@RequestParam Language language, @RequestPart MultipartFile sourceFile) throws IOException, InterruptedException {
        CompileResponse response = userCodeService.compile(language, sourceFile);
        return ResponseEntity.status(response.status()).body(response);
    }
}
