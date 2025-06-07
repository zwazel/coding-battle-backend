package dev.zwazel.service;

import dev.zwazel.DTO.CompileResponse;
import dev.zwazel.model.language.Language;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

@Service
@Slf4j
public class UserCodeService {
    public static final Path ARTIFACTS = Path.of("artifacts");

    public CompileResponse compile(Language language, MultipartFile file) throws IOException, InterruptedException {
        return language.compile(file);
    }
}
