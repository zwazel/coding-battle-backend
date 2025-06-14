package dev.zwazel.service;

import dev.zwazel.DTO.CompileResultDTO;
import dev.zwazel.domain.Bot;
import dev.zwazel.domain.User;
import dev.zwazel.model.language.Language;
import dev.zwazel.repository.BotRepository;
import dev.zwazel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class BotService {
    private final BotRepository botRepository;
    private final UserRepository userRepository;

    @Value("${user.code.storage}")
    private String userCodeStorage;

    public ResponseEntity<CreateBotResponse> createBot(String botName, Language language, MultipartFile sourceFile, UUID userId) {
        if (botName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Sanitize the bot name
        botName = botName.trim().replaceAll("[^a-zA-Z0-9_\\-]", "");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Check if the bot name is already taken
        if (botRepository.existsByNameLowerAndOwner(botName.toLowerCase(), user)) {
            throw new IllegalArgumentException("Bot name already exists for this user");
        }

        Path botDirPath = Path.of(userCodeStorage, "bots", userId.toString(), botName.toLowerCase());
        String botDir = botDirPath.toString();

        Path source = Path.of(botDir, "source", botName.toLowerCase() + "." + language.getFileExtension());
        try {
            // Create directories if they do not exist
            Files.createDirectories(source.getParent());
            sourceFile.transferTo(source);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save bot source file", e);
        }

        // Compile the source file
        // Path to the DIRECTORY of the compiled WASM file
        Path compiledDir = Path.of(botDir, "compiled");
        CompileResultDTO compileResult;
        try {
            // Create directories for compiled output
            Files.createDirectories(compiledDir);
            compileResult = language.compile(botName, compiledDir, sourceFile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new CreateBotResponse(null)); // Return null ID on failure
        }

        if (Objects.requireNonNull(compileResult.status()) == HttpStatus.BAD_REQUEST) {
            throw new IllegalArgumentException("Compilation failed: " + compileResult.buildLog());
        }

        // Create and save the bot entity
        Bot bot = Bot.builder()
                .name(botName)
                .language(language)
                .sourcePath(source.toString()) // TODO: Do we need to store the source path?
                .owner(user)
                .wasmPath(compileResult.wasmPath()) // TODO: Do we need to store the WASM path?
                .build();

        bot = botRepository.save(bot);

        return ResponseEntity.ok(new CreateBotResponse(bot.getId()));
    }

    public record CreateBotResponse(UUID id) {
    }
}
