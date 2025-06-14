package dev.zwazel.service;

import dev.zwazel.DTO.CompileResultDTO;
import dev.zwazel.domain.Bot;
import dev.zwazel.domain.User;
import dev.zwazel.model.language.Language;
import dev.zwazel.repository.BotRepository;
import dev.zwazel.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class BotService {
    private final BotRepository botRepository;
    private final UserRepository userRepository;

    private final StorageService storageService;

    public ResponseEntity<CreateBotResponse> createBot(String botName, Language language, MultipartFile sourceFile, UUID userId) throws IOException {
        if (botName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Sanitize the bot name
        botName = botName.trim().replaceAll("[^a-zA-Z0-9_\\-]", "");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Check if the bot name is already taken
        if (botRepository.existsByNameIgnoreCaseAndOwner(botName.toLowerCase(), user)) {
            throw new IllegalArgumentException("Bot name already exists for this user");
        }

        Path savedSourcePath = storageService.saveSource(userId, botName, language, sourceFile);

        // Compile the source file
        // Path to the DIRECTORY of the compiled WASM file
        Path compiledDir = storageService.compiledDir(userId, botName);
        CompileResultDTO compileResult = null;
        try {
            // Create directories for compiled output
            Files.createDirectories(compiledDir);
            compileResult = language.compile(botName, compiledDir, sourceFile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new CreateBotResponse(null, compileResult)); // Return null ID on failure
        }

        if (Objects.requireNonNull(compileResult.status()) == HttpStatus.BAD_REQUEST) {
            throw new IllegalArgumentException("Compilation failed: " + compileResult.buildLog());
        }

        // Create and save the bot entity
        Bot bot = Bot.builder()
                .name(botName)
                .language(language)
                .sourcePath(savedSourcePath.toString()) // TODO: Do we need to store the source path?
                .owner(user)
                .wasmPath(compileResult.wasmPath()) // TODO: Do we need to store the WASM path?
                .build();

        bot = botRepository.save(bot);

        return ResponseEntity.ok(new CreateBotResponse(bot.getId(), compileResult));
    }

    public ResponseEntity<CompileResultDTO> updateBotSource(@NonNull UUID botId, @NonNull MultipartFile sourceFile, UUID userId) throws IOException {
        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> new EntityNotFoundException("Bot not found with ID: " + botId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        // Check if the user is the owner of the bot
        if (!bot.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Save the new source file
        Path savedSourcePath;
        try {
            savedSourcePath = storageService.replaceSource(bot, sourceFile);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CompileResultDTO(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save source file", null, null));
        }

        // Compile the new source file
        Path compiledDir = storageService.compiledDir(userId, bot.getName());
        CompileResultDTO compileResult;
        try {
            Files.createDirectories(compiledDir);
            compileResult = bot.getLanguage().compile(bot.getName(), compiledDir, sourceFile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new CompileResultDTO(HttpStatus.BAD_REQUEST, "Compilation failed: " + e.getMessage(), null, null));
        }

        if (Objects.requireNonNull(compileResult.status()) == HttpStatus.BAD_REQUEST) {
            return ResponseEntity.badRequest().body(compileResult);
        }

        // Update the bot's source path and WASM path
        bot.setSourcePath(savedSourcePath.toString());
        bot.setWasmPath(compileResult.wasmPath());
        botRepository.save(bot);

        return ResponseEntity.ok(compileResult);
    }

    // TODO: Update the bot's source file service, and recompile it

    public record CreateBotResponse(UUID id, CompileResultDTO compileResult) {

    }
}
