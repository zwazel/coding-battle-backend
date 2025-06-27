package dev.zwazel.service;

import dev.zwazel.api.model.DTO.CompileResultDTO;
import dev.zwazel.domain.Bot;
import dev.zwazel.domain.User;
import dev.zwazel.exception.BotNameAlreadyExistsException;
import dev.zwazel.exception.BotNotFoundException;
import dev.zwazel.exception.UserNotFoundException;
import dev.zwazel.language.Language;
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class BotService {
    private final BotRepository botRepository;
    private final UserRepository userRepository;

    private final StorageService storageService;

    public Iterable<Bot> getBotsByUserId(UUID userId) {
        log.info("Fetching bots for user with ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new UserNotFoundException(userId);
                });
        return botRepository.findAllByOwner(user);
    }

    public ResponseEntity<CreateBotResponse> createBot(String botName, Language language, MultipartFile sourceFile, UUID userId) throws IOException {
        log.info("Creating bot '{}' for user '{}'", botName, userId);

        if (botName.isBlank()) {
            log.warn("Bot name is blank");
            return ResponseEntity.badRequest().build();
        }

        // Sanitize the bot name
        botName = botName.trim().replaceAll("[^a-zA-Z0-9_\\-]", "");
        log.debug("Sanitized bot name: '{}'", botName);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new UserNotFoundException(userId);
                });

        // Check if the bot name is already taken
        if (botRepository.existsByNameIgnoreCaseAndOwner(botName.toLowerCase(), user)) {
            log.warn("Bot name '{}' already exists for user '{}'", botName, userId);
            throw new BotNameAlreadyExistsException(botName);
        }

        Path savedSourcePath = storageService.saveSource(userId, botName, language, sourceFile);
        log.info("Saved source file for bot '{}' to '{}'", botName, savedSourcePath);

        // Compile the source file
        // Path to the DIRECTORY of the compiled WASM file
        Path compiledDir = storageService.compiledDir(userId, botName);
        CompileResultDTO compileResult = null;
        try {
            // Create directories for compiled output
            Files.createDirectories(compiledDir);
            log.info("Compiling bot '{}'", botName);
            compileResult = language.compile(botName, compiledDir, sourceFile);
        } catch (Exception e) {
            log.error("Failed to compile bot '{}'", botName, e);
            return ResponseEntity.badRequest().body(new CreateBotResponse(null, compileResult)); // Return null ID on failure
        }

        if (Objects.requireNonNull(compileResult.status()) == HttpStatus.BAD_REQUEST) {
            log.warn("Compilation failed for bot '{}'", botName);
            throw new IllegalArgumentException("Compilation failed: " + compileResult.buildLog());
        }
        log.info("Successfully compiled bot '{}'", botName);

        // Create and save the bot entity
        Bot bot = Bot.builder()
                .name(botName)
                .language(language)
                .sourcePath(savedSourcePath.toString()) // TODO: Do we need to store the source path?
                .owner(user)
                .wasmPath(compileResult.wasmPath()) // TODO: Do we need to store the WASM path?
                .build();

        bot = botRepository.save(bot);
        log.info("Saved bot '{}' with ID '{}' to the database", bot.getName(), bot.getId());

        return ResponseEntity.ok(new CreateBotResponse(bot.getId(), compileResult));
    }

    public ResponseEntity<CompileResultDTO> updateBotSource(@NonNull UUID botId, @NonNull MultipartFile sourceFile, UUID userId) throws IOException {
        log.info("Updating source for bot '{}' for user '{}'", botId, userId);
        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> {
                    log.error("Bot not found with ID: {}", botId);
                    return new BotNotFoundException(botId);
                });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new UserNotFoundException(userId);
                });

        // Check if the user is the owner of the bot
        if (!bot.getOwner().getId().equals(user.getId())) {
            log.warn("User '{}' is not the owner of bot '{}'", userId, botId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Save the new source file
        Path savedSourcePath;
        try {
            savedSourcePath = storageService.replaceSource(bot, sourceFile);
            log.info("Replaced source file for bot '{}'", botId);
        } catch (IOException e) {
            log.error("Failed to save source file for bot '{}'", botId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CompileResultDTO(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save source file", null, null));
        }

        // Compile the new source file
        Path compiledDir = storageService.compiledDir(userId, bot.getName());
        CompileResultDTO compileResult;
        try {
            Files.createDirectories(compiledDir);
            log.info("Compiling bot '{}'", bot.getName());
            compileResult = bot.getLanguage().compile(bot.getName(), compiledDir, sourceFile);
        } catch (Exception e) {
            log.error("Failed to compile bot '{}'", bot.getName(), e);
            return ResponseEntity.badRequest().body(new CompileResultDTO(HttpStatus.BAD_REQUEST, "Compilation failed: " + e.getMessage(), null, null));
        }

        if (Objects.requireNonNull(compileResult.status()) == HttpStatus.BAD_REQUEST) {
            log.warn("Compilation failed for bot '{}'", bot.getName());
            return ResponseEntity.badRequest().body(compileResult);
        }
        log.info("Successfully compiled bot '{}'", bot.getName());

        // Update the bot's source path and WASM path
        bot.setSourcePath(savedSourcePath.toString());
        bot.setWasmPath(compileResult.wasmPath());
        botRepository.save(bot);
        log.info("Updated bot '{}' in the database", bot.getName());

        return ResponseEntity.ok(compileResult);
    }

    public ResponseEntity<Resource> getBotSource(@NonNull UUID botId, UUID loggedInUserId) throws MalformedURLException {
        log.info("Getting source for bot '{}' for user '{}'", botId, loggedInUserId);
        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> {
                    log.error("Bot not found with ID: {}", botId);
                    return new EntityNotFoundException("Bot not found with ID: " + botId);
                });

        // Check if the user is the owner of the bot
        if (!bot.getOwner().getId().equals(loggedInUserId)) {
            log.warn("User '{}' is not the owner of bot '{}'", loggedInUserId, botId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Path sourcePath = Path.of(bot.getSourcePath());
        if (!Files.exists(sourcePath)) {
            log.error("Source file not found for bot '{}' at path '{}'", botId, sourcePath);
            return ResponseEntity.notFound().build();
        }

        Resource file = new UrlResource(sourcePath.toUri());

        if (!file.exists() || !file.isReadable()) {
            log.error("Source file not readable for bot '{}' at path '{}'", botId, sourcePath);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        log.info("Returning source for bot '{}'", botId);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + sourcePath.getFileName() + "\"")
                .body(file);
    }

    public Bot getBotById(@NonNull UUID botId) {
        log.info("Fetching bot with ID: {}", botId);
        return botRepository.findById(botId)
                .orElseThrow(() -> {
                    log.error("Bot not found with ID: {}", botId);
                    return new BotNotFoundException(botId);
                });
    }


    // TODO: replace with hateoas response model
    public record CreateBotResponse(UUID id, CompileResultDTO compileResult) {
    }
}
