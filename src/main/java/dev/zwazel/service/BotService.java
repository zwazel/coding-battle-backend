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
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

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
     *  CREATE BOT  – fully reactive façade over blocking JPA
     *────────────────────────────────────────────────────*/
    public Mono<ResponseEntity<CreateBotResponse>> createBot(
            @NonNull String botName,
            @NonNull Language language,
            @NonNull MultipartFile sourceFile,
            @NonNull UUID userId) {

        log.info("Creating bot: name={}, language={}, userId={}",
                botName, language, userId);

        botName = botName.trim();
        if (botName.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        final String sanitized = botName.replaceAll("[^a-zA-Z0-9_\\-]", "");

        /* 1️⃣  Blocking JPA checks ➜ boundedElastic */
        Mono<User> ownerMono = Mono.fromCallable(() ->
                        userRepository.findById(userId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                        "User not found: " + userId)))
                .subscribeOn(Schedulers.boundedElastic());

        return ownerMono.flatMap(owner ->
                        Mono.fromCallable(() -> {
                                    boolean exists = botRepository
                                            .existsByNameIgnoreCaseAndOwner(sanitized.toLowerCase(), owner);
                                    if (exists) {
                                        throw new IllegalArgumentException("Bot name already exists for this user");
                                    }
                                    return owner;
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                )
                /* 2️⃣  Save source (disk IO) */
                .flatMap(owner ->
                        Mono.fromCallable(() ->
                                        storageService.saveSource(userId, sanitized, language, sourceFile))
                                .map(path -> new BotCreateCtx(owner, path)))
                /* 3️⃣  Compile (CPU/IO heavy) */
                .flatMap(ctx ->
                        Mono.fromCallable(() -> {
                                    Path compiledDir = storageService.compiledDir(userId, sanitized);
                                    Files.createDirectories(compiledDir);
                                    return language.compile(sanitized, compiledDir, sourceFile);
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .map(res -> new BotCompileCtx(ctx.user, ctx.sourcePath, res)))
                /* 4️⃣  Persist bot  */
                .flatMap(ctx -> {
                    CompileResultDTO res = ctx.result;
                    if (Objects.requireNonNull(res.status()) == HttpStatus.BAD_REQUEST) {
                        return Mono.error(new IllegalArgumentException(
                                "Compilation failed: " + res.buildLog()));
                    }
                    Bot toSave = Bot.builder()
                            .name(sanitized)
                            .language(language)
                            .sourcePath(ctx.sourcePath.toString())
                            .owner(ctx.user)
                            .wasmPath(res.wasmPath())
                            .build();

                    return Mono.fromCallable(() -> botRepository.save(toSave))
                            .subscribeOn(Schedulers.boundedElastic())
                            .map(saved -> ResponseEntity.ok(
                                    new CreateBotResponse(saved.getId(), res)));
                })
                /* 5️⃣  Map validation errors → 400 */
                .onErrorResume(err -> {
                    if (err instanceof IllegalArgumentException) {
                        return Mono.just(ResponseEntity.badRequest().body(
                                new CreateBotResponse(null,
                                        new CompileResultDTO(HttpStatus.BAD_REQUEST,
                                                err.getMessage(), null, null))));
                    }
                    return Mono.error(err);
                });
    }

    /*─────────────────────────────────────────────────────
     *  UPDATE BOT SOURCE
     *────────────────────────────────────────────────────*/
    public Mono<ResponseEntity<CompileResultDTO>> updateBotSource(
            @NonNull UUID botId,
            @NonNull MultipartFile sourceFile,
            @NonNull UUID userId) {

        /* blocking fetch ➜ boundedElastic */
        Mono<Tuple2<Bot, User>> fetchMono = Mono.fromCallable(() -> Tuples.of(
                        botRepository.findById(botId)
                                .orElseThrow(() -> new EntityNotFoundException("Bot not found: " + botId)),
                        userRepository.findById(userId)
                                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId))
                ))
                .subscribeOn(Schedulers.boundedElastic());

        return fetchMono.flatMap(tuple -> {
            Bot bot = tuple.getT1();
            User usr = tuple.getT2();

            if (!bot.getOwner().getId().equals(usr.getId())) {
                return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
            }

            return Mono.fromCallable(() -> storageService.replaceSource(bot, sourceFile))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(savedPath -> compileAndSave(bot, sourceFile, savedPath));
        });
    }

    /*─────────────────────────────────────────────────────
     *  compileAndSave – unchanged except for blocking save
     *────────────────────────────────────────────────────*/
    private Mono<ResponseEntity<CompileResultDTO>> compileAndSave(
            Bot bot,
            MultipartFile sourceFile,
            Path savedSourcePath) {

        UUID userId = bot.getOwner().getId();
        String name = bot.getName();

        return Mono.fromCallable(() -> {
                    Path compiledDir = storageService.compiledDir(userId, name);
                    Files.createDirectories(compiledDir);
                    return bot.getLanguage().compile(name, compiledDir, sourceFile);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(res -> {
                    if (Objects.requireNonNull(res.status()) == HttpStatus.BAD_REQUEST) {
                        return Mono.just(ResponseEntity.badRequest().body(res));
                    }
                    bot.setSourcePath(savedSourcePath.toString());
                    bot.setWasmPath(res.wasmPath());

                    return Mono.fromCallable(() -> botRepository.save(bot))
                            .subscribeOn(Schedulers.boundedElastic())
                            .thenReturn(ResponseEntity.ok(res));
                })
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.badRequest().body(
                                new CompileResultDTO(HttpStatus.BAD_REQUEST,
                                        "Compilation failed: " + e.getMessage(), null, null))));
    }

    /*─────────────────────────────────────────────────────
     *  GET BOT SOURCE
     *────────────────────────────────────────────────────*/
    public Mono<ResponseEntity<Resource>> getBotSource(UUID botId, UUID requesterId) {
        return Mono.fromCallable(() ->
                        botRepository.findById(botId)
                                .orElseThrow(() -> new EntityNotFoundException("Bot not found: " + botId)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(bot -> {
                    if (!bot.getOwner().getId().equals(requesterId)) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                    }
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
