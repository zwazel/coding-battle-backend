package dev.zwazel.service;

import dev.zwazel.api.model.DTO.CompileResultDTO;
import dev.zwazel.domain.Bot;
import dev.zwazel.domain.User;
import dev.zwazel.model.language.Language;
import dev.zwazel.repository.BotRepository;
import dev.zwazel.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BotService {

    private final BotRepository botRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    /*─────────────────────────────────────────────────────
     *  CREATE BOT (reactive)
     *────────────────────────────────────────────────────*/
    public Mono<ResponseEntity<CreateBotResponse>> createBot(
            @NonNull String botName,
            @NonNull Language language,
            @NonNull MultipartFile sourceFile,
            @NonNull UUID userId) {

        /* quick non‑blocking checks */
        botName = botName.trim();
        if (botName.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        String sanitized = botName.replaceAll("[^a-zA-Z0-9_\\-]", "");

        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("User not found: " + userId)))
                .flatMap(user ->
                        botRepository.existsByNameIgnoreCaseAndOwner(sanitized.toLowerCase(), user)
                                .flatMap(exists -> exists
                                        ? Mono.error(new IllegalArgumentException("Bot name already exists for this user"))
                                        : Mono.just(user)))
                /* save source on boundedElastic (blocking IO) */
                .flatMap(user ->
                        Mono.fromCallable(() ->
                                        storageService.saveSource(userId, sanitized, language, sourceFile))
                                .subscribeOn(Schedulers.boundedElastic())
                                .map(path -> new BotCreateCtx(user, path)))
                /* compile source (CPU/IO heavy) */
                .flatMap(ctx ->
                        Mono.fromCallable(() -> {
                                    Path compiledDir = storageService.compiledDir(userId, sanitized);
                                    Files.createDirectories(compiledDir);
                                    return language.compile(sanitized, compiledDir, sourceFile);
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .map(res -> new BotCompileCtx(ctx.user, ctx.sourcePath, res)))
                /* validate compilation + persist bot */
                .flatMap(ctx -> {
                    CompileResultDTO res = ctx.result;
                    if (Objects.requireNonNull(res.status()) == HttpStatus.BAD_REQUEST) {
                        return Mono.error(new IllegalArgumentException("Compilation failed: " + res.buildLog()));
                    }
                    Bot toSave = Bot.builder()
                            .name(sanitized)
                            .language(language)
                            .sourcePath(ctx.sourcePath.toString())
                            .owner(ctx.user)
                            .wasmPath(res.wasmPath())
                            .build();
                    return botRepository.save(toSave)
                            .map(saved -> ResponseEntity.ok(new CreateBotResponse(saved.getId(), res)));
                })
                /* map domain errors to proper HTTP codes */
                .onErrorResume(err -> {
                    if (err instanceof IllegalArgumentException) {
                        return Mono.just(ResponseEntity.badRequest().body(
                                new CreateBotResponse(null,
                                        new CompileResultDTO(HttpStatus.BAD_REQUEST, err.getMessage(), null, null))));
                    }
                    return Mono.error(err);   // bubble others
                });
    }

    /*─────────────────────────────────────────────────────
     *  UPDATE BOT SOURCE
     *────────────────────────────────────────────────────*/
    public Mono<ResponseEntity<CompileResultDTO>> updateBotSource(
            @NonNull UUID botId,
            @NonNull MultipartFile sourceFile,
            @NonNull UUID userId) {

        return Mono.zip(botRepository.findById(botId),
                        userRepository.findById(userId))
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Bot or User not found")))
                .flatMap(tuple -> {
                    Bot bot = tuple.getT1();
                    User user = tuple.getT2();
                    if (!bot.getOwner().getId().equals(user.getId())) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                    }
                    return Mono.fromCallable(() -> storageService.replaceSource(bot, sourceFile))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(savedPath -> compileAndSave(bot, sourceFile, savedPath));
                });
    }

    private Mono<ResponseEntity<CompileResultDTO>> compileAndSave(
            Bot bot,
            MultipartFile sourceFile,
            Path savedSourcePath) {

        UUID userId = bot.getOwner().getId();
        String botName = bot.getName();

        return Mono.fromCallable(() -> {
                    Path compiledDir = storageService.compiledDir(userId, botName);
                    Files.createDirectories(compiledDir);
                    return bot.getLanguage().compile(botName, compiledDir, sourceFile);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(res -> {
                    if (Objects.requireNonNull(res.status()) == HttpStatus.BAD_REQUEST) {
                        return Mono.just(ResponseEntity.badRequest().body(res));
                    }
                    bot.setSourcePath(savedSourcePath.toString());
                    bot.setWasmPath(res.wasmPath());
                    return botRepository.save(bot)
                            .thenReturn(ResponseEntity.ok(res));
                })
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.badRequest()
                                .body(new CompileResultDTO(HttpStatus.BAD_REQUEST,
                                        "Compilation failed: " + e.getMessage(), null, null))));
    }

    /*─────────────────────────────────────────────────────
     *  GET BOT SOURCE FILE
     *────────────────────────────────────────────────────*/
    public Mono<ResponseEntity<Resource>> getBotSource(UUID botId, UUID requesterId) {
        return botRepository.findById(botId)
                // 404 if bot missing
                .switchIfEmpty(Mono.error(
                        new EntityNotFoundException("Bot not found: " + botId)))

                // single flatMap keeps type stable
                .flatMap(bot -> {
                    // 403 if wrong owner
                    if (!bot.getOwner().getId().equals(requesterId)) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                    }
                    // happy path
                    return fileResponse(Path.of(bot.getSourcePath()));
                });
    }

    private Mono<ResponseEntity<Resource>> fileResponse(Path path) {
        if (!Files.exists(path)) {
            return Mono.just(ResponseEntity.notFound().build());
        }

        return Mono.fromCallable(() -> new UrlResource(path.toUri()))
                .subscribeOn(Schedulers.boundedElastic())
                .map(res -> {
                    if (!res.exists() || !res.isReadable()) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                    return ResponseEntity.ok()
                            .contentType(MediaType.TEXT_PLAIN)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "inline; filename=\"" + path.getFileName() + "\"")
                            .body(res);
                });
    }

    /*─────────────────────────────────────────────────────
     *  PRIVATE HELPER RECORDS
     *────────────────────────────────────────────────────*/
    private record BotCreateCtx(User user, Path sourcePath) {
    }

    private record BotCompileCtx(User user, Path sourcePath, CompileResultDTO result) {
    }

    /*─────────────────────────────────────────────────────
     *  RESPONSE DTO
     *────────────────────────────────────────────────────*/
    public record CreateBotResponse(UUID id, CompileResultDTO compileResult) {
    }
}
