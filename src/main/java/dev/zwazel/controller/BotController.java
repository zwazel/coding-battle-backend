package dev.zwazel.controller;

import dev.zwazel.DTO.CompileResultDTO;
import dev.zwazel.model.language.Language;
import dev.zwazel.security.CustomUserPrincipal;
import dev.zwazel.service.BotService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.UUID;

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
    ) throws IOException {
        return botService.createBot(botName, language, sourceFile, loggedInUser.getId());
    }

    /**
     * Updates the source code of an existing bot and recompiles it.
     *
     * @param botId        The ID of the bot to update.
     * @param sourceFile   The new source file to upload.
     * @param loggedInUser The currently authenticated user. Automatically injected by Spring Security.
     * @return A ResponseEntity containing the CompileResultDTO with the compilation result.
     * @throws IOException If an error occurs while reading the source file or during compilation.
     */
    @PutMapping("/{botId}/source")
    public ResponseEntity<CompileResultDTO> updateBotSource(
            @PathVariable @NonNull UUID botId,
            @RequestPart @NonNull MultipartFile sourceFile,
            @AuthenticationPrincipal CustomUserPrincipal loggedInUser
    ) throws IOException {
        return botService.updateBotSource(botId, sourceFile, loggedInUser.getId());
    }

    @GetMapping("/{botId}/source")
    public ResponseEntity<Resource> getBotSource(
            @PathVariable @NonNull UUID botId,
            @AuthenticationPrincipal CustomUserPrincipal loggedInUser
    ) {
        try {
            return botService.getBotSource(botId, loggedInUser.getId());
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
