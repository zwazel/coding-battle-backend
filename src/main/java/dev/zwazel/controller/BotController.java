package dev.zwazel.controller;

import dev.zwazel.model.language.Language;
import dev.zwazel.security.CustomUserPrincipal;
import dev.zwazel.service.BotService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/bots")
class BotController {
    private final BotService botService;

    @PostMapping
    public ResponseEntity<BotService.CreateBotResponse> createBot(
            @RequestParam @NonNull String botName,
            @RequestParam @NonNull Language language,
            @RequestPart @NonNull MultipartFile sourceFile,
            @AuthenticationPrincipal CustomUserPrincipal loggedInUser
    ) {
        return botService.createBot(botName, language, sourceFile, loggedInUser.getId());
    }
}
