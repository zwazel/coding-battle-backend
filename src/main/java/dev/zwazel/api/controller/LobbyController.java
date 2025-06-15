package dev.zwazel.api.controller;

import dev.zwazel.api.model.DTO.AllLobbiesDTO;
import dev.zwazel.api.model.DTO.CreateLobbyRequestDTO;
import dev.zwazel.model.Lobby;
import dev.zwazel.model.LobbyEvent;
import dev.zwazel.security.CustomUserPrincipal;
import dev.zwazel.service.LobbyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@RestController
@RequestMapping("/lobbies")
@Slf4j
@RequiredArgsConstructor
public class LobbyController {

    private final LobbyService lobbyService;

    /*─────────────────────────────────────────────────────
     *  POST /lobbies  – create lobby
     *────────────────────────────────────────────────────*/
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Mono<Lobby> createLobby(@RequestBody CreateLobbyRequestDTO request,
                                   @AuthenticationPrincipal CustomUserPrincipal user) {
        return lobbyService.createLobby(request, user);
    }

    /*─────────────────────────────────────────────────────
     *  GET /lobbies/{id}  – retrieve one
     *────────────────────────────────────────────────────*/
    @GetMapping("/{lobbyId}")
    public Mono<Lobby> getLobby(@PathVariable String lobbyId) {
        return lobbyService.getLobby(lobbyId)
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("Lobby not found: " + lobbyId)));
    }

    /*─────────────────────────────────────────────────────
     *  GET /lobbies  – list all (DTO)
     *────────────────────────────────────────────────────*/
    @GetMapping
    public Flux<AllLobbiesDTO> getAllLobbies() {
        return lobbyService.getAllLobbies()
                .map(AllLobbiesDTO::from);
    }

    /*─────────────────────────────────────────────────────
     *  SSE: /lobbies/{id}/events  – lobby+simulation stream
     *────────────────────────────────────────────────────*/
    @GetMapping(value = "/{lobbyId}/events",
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<LobbyEvent> subscribeToLobbyEvents(@PathVariable String lobbyId) {

        return lobbyService.getSink(lobbyId)
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("Lobby not found: " + lobbyId)))
                .flatMapMany(Sinks.Many::asFlux);
    }
}
