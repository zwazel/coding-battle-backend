package dev.zwazel.api.controller;

import dev.zwazel.api.model.DTO.CompileResultDTO;
import dev.zwazel.model.language.Language;
import dev.zwazel.security.CustomUserPrincipal;
import dev.zwazel.service.BotService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.util.UUID;

@RestController
@RequestMapping("/bots")
@RequiredArgsConstructor
@Slf4j
public class BotController {

    private final BotService botService;

    /*─────────────────────────────────────────────────────
     *  POST /bots  – create bot
     *────────────────────────────────────────────────────*/
    @PostMapping
    public Mono<ResponseEntity<BotService.CreateBotResponse>> createBot(
            @RequestParam @NonNull String botName,
            @RequestParam @NonNull Language language,
            @RequestPart @NonNull MultipartFile sourceFile,
            @AuthenticationPrincipal CustomUserPrincipal user) {
        log.info("Creating bot: name={}, language={}, user={}, userId={}",
                botName, language, user.getUsername(), user.getId());

        return botService.createBot(botName, language, sourceFile, user.getId())
                .doOnSuccess(response -> log.info("Bot created: {}", response.getBody() != null ? response.getBody().id() : "NOTHING CREATED"))
                .doOnError(err -> log.error("Failed to create bot", err))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Failed to create bot: " + botName)))
                ;
    }

    /*─────────────────────────────────────────────────────
     *  PUT /bots/{id}/source  – update & recompile
     *────────────────────────────────────────────────────*/
    @PutMapping("/{botId}/source")
    public Mono<ResponseEntity<CompileResultDTO>> updateBotSource(
            @PathVariable @NonNull UUID botId,
            @RequestPart @NonNull MultipartFile sourceFile,
            @AuthenticationPrincipal CustomUserPrincipal user) {

        return botService.updateBotSource(botId, sourceFile, user.getId());
    }

    /*─────────────────────────────────────────────────────
     *  GET /bots/{id}/source  – download source
     *────────────────────────────────────────────────────*/
    @GetMapping("/{botId}/source")
    public Mono<ResponseEntity<Resource>> getBotSource(
            @PathVariable @NonNull UUID botId,
            @AuthenticationPrincipal CustomUserPrincipal user) {

        return botService.getBotSource(botId, user.getId())
                /* translate malformed path into 400 */
                .onErrorResume(MalformedURLException.class,
                        err -> Mono.just(ResponseEntity.badRequest().build()));
    }
}
