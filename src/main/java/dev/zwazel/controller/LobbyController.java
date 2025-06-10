package dev.zwazel.controller;

import dev.zwazel.DTO.AllLobbiesDTO;
import dev.zwazel.DTO.CreateLobbyRequestDTO;
import dev.zwazel.model.Lobby;
import dev.zwazel.model.LobbyEvent;
import dev.zwazel.repository.UserRepository;
import dev.zwazel.security.CustomUserPrincipal;
import dev.zwazel.service.LobbyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/lobbies")
@Slf4j
@RequiredArgsConstructor
public class LobbyController {

    private final LobbyService lobbyService;
    private final UserRepository userRepository;

    /**
     * Create a new lobby, providing a list of players in the request body.
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Lobby createLobby(@RequestBody CreateLobbyRequestDTO request, @AuthenticationPrincipal CustomUserPrincipal loggedInUser) {
        return lobbyService.createLobby(request, loggedInUser);
    }

    /**
     * Get info about a specific lobby (players, etc.).
     */
    @GetMapping("/{lobbyId}")
    public Lobby getLobby(@PathVariable String lobbyId) {
        Lobby lobby = lobbyService.getLobby(lobbyId);
        if (lobby == null) {
            throw new IllegalArgumentException("Lobby not found: " + lobbyId);
        }
        return lobby;
    }

    /**
     * (Optional) List all lobbies
     */
    @GetMapping
    public List<AllLobbiesDTO> getAllLobbies() {
        return AllLobbiesDTO.from(lobbyService.getAllLobbies());
    }

    /**
     * SSE endpoint that merges both "lobby" + "simulation" events.
     */
    @GetMapping(value = "/{lobbyId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<LobbyEvent> subscribeToLobbyEvents(@PathVariable String lobbyId) {
        var sink = lobbyService.getSink(lobbyId);
        if (sink == null) {
            return Flux.error(new IllegalArgumentException("Lobby not found: " + lobbyId));
        }
        return sink.asFlux();
    }
}
