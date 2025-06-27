package dev.zwazel.service;

import dev.zwazel.domain.Bot;
import dev.zwazel.language.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {
    @Value("${user.code.storage.base}")
    private Path baseDir;

    /**
     * <base>/<user>/<bot>/compiled
     */
    public Path compiledDir(UUID userId, String botName) throws IOException {
        log.debug("Getting compiled directory for user '{}' and bot '{}'", userId, botName);
        Path dir = baseDir.resolve(userId.toString())
                .resolve(botName.toLowerCase())
                .resolve("compiled");
        Files.createDirectories(dir);
        return dir;
    }

    /**
     * <base>/<user>/<bot>/source/<bot>.{ext}
     */
    public Path sourcePath(UUID userId, String botName, Language ext) throws IOException {
        log.debug("Getting source path for user '{}' and bot '{}'", userId, botName);
        Path dir = baseDir.resolve(userId.toString())
                .resolve(botName.toLowerCase())
                .resolve("source");
        Files.createDirectories(dir);
        return dir.resolve(botName.toLowerCase() + "." + ext.getFileExtension());
    }

    /**
     * Save the uploaded source file under a deterministic name.
     *
     * @return the final path of the stored source
     */
    public Path saveSource(UUID userId,
                           String botName,
                           Language extension,
                           MultipartFile upload) throws IOException {
        log.info("Saving source for user '{}' and bot '{}'", userId, botName);

        Path dest = sourcePath(userId, botName, extension);
        upload.transferTo(dest);
        log.info("Saved source to '{}'", dest);
        return dest;
    }

    /**
     * Replace an existing source file with a new one (already on disk),
     * keeping the same deterministic filename.
     *
     * @return the final path (unchanged except contents)
     */
    public Path replaceSource(Bot bot, Path newFile) throws IOException {
        log.info("Replacing source for bot '{}' with new file '{}'", bot.getName(), newFile);
        Path old = Path.of(bot.getSourcePath());
        Files.deleteIfExists(old);
        log.debug("Deleted old source file at '{}'", old);
        Path newPath = Files.move(newFile, old, StandardCopyOption.REPLACE_EXISTING);
        log.info("Moved new source file to '{}'", newPath);
        return newPath;
    }

    /**
     * Replace an existing source file with a new one (provided as a MultipartFile),
     * renaming it to the same deterministic filename.
     */
    public Path replaceSource(Bot bot, MultipartFile newFile) throws IOException {
        log.info("Replacing source for bot '{}' with new multipart file", bot.getName());
        Path old = Path.of(bot.getSourcePath());
        Files.deleteIfExists(old);
        log.debug("Deleted old source file at '{}'", old);
        Path newPath = old.getParent().resolve(old.getFileName());
        newFile.transferTo(newPath);
        log.info("Saved new source file to '{}'", newPath);
        return newPath;
    }
}
