package dev.zwazel.api.controller;

import dev.zwazel.api.hal.assembler.BotModelAssembler;
import dev.zwazel.api.hal.model.BotModel;
import dev.zwazel.api.model.DTO.CompileResultDTO;
import dev.zwazel.domain.Bot;
import dev.zwazel.language.Language;
import dev.zwazel.security.CustomUserPrincipal;
import dev.zwazel.service.BotService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/bots")
public class BotController {
    private final BotService botService;

    private final BotModelAssembler botModelAssembler;

    @PostMapping
    public ResponseEntity<BotService.CreateBotResponse> createBot(
            @RequestParam @NonNull String botName,
            @RequestParam @NonNull Language language,
            @RequestPart @NonNull MultipartFile sourceFile,
            @AuthenticationPrincipal CustomUserPrincipal loggedInUser
    ) throws IOException {
        log.info("createBot called with botName: {}, language: {}", botName, language);
        return botService.createBot(botName, language, sourceFile, loggedInUser.getId());
    }

    @GetMapping("/{botId}")
    public EntityModel<BotModel> one(
            @PathVariable @NonNull UUID botId
    ) {
        log.info("one called with botId: {}", botId);
        Bot bot = botService.getBotById(botId);
        return EntityModel.of(
                botModelAssembler.toModel(bot)
        );
    }

    // Get all bots for a specific user
    @GetMapping("/user/{userId}")
    public CollectionModel<BotModel> getBotsByUserId(
            @PathVariable @NonNull UUID userId
    ) {
        log.info("getBotsByUserId called with userId: {}", userId);
        Iterable<Bot> bots = botService.getBotsByUserId(userId);
        CollectionModel<BotModel> collectionModel = botModelAssembler.toCollectionModel(bots);

        // Add self link to the collection
        collectionModel.add(linkTo(methodOn(BotController.class).getBotsByUserId(userId)).withSelfRel());
        
        return collectionModel;
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
        log.info("updateBotSource called with botId: {}", botId);
        return botService.updateBotSource(botId, sourceFile, loggedInUser.getId());
    }

    @GetMapping("/{botId}/source")
    public ResponseEntity<Resource> getBotSource(
            @PathVariable @NonNull UUID botId,
            @AuthenticationPrincipal CustomUserPrincipal loggedInUser
    ) {
        log.info("getBotSource called with botId: {}", botId);
        try {
            return botService.getBotSource(botId, loggedInUser.getId());
        } catch (MalformedURLException e) {
            log.error("getBotSource failed with MalformedURLException", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
