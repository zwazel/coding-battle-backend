package dev.zwazel.controller;

import dev.zwazel.DTO.CompileResultDTO;
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
    public ResponseEntity<CompileResultDTO> uploadCodeAndCompileToWasm(@RequestParam Language language, @RequestPart MultipartFile sourceFile) throws IOException, InterruptedException {
        CompileResultDTO response = userCodeService.compile(language, sourceFile);
        return ResponseEntity.status(response.status()).body(response);
    }
}
