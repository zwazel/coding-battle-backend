package dev.zwazel.controller;

import dev.zwazel.model.GameState;
import dev.zwazel.model.Player;
import dev.zwazel.service.LobbyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/lobbies")
@Slf4j
public class LobbyController {

    private final LobbyService lobbyService;

    public LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    /**
     * Create a new lobby. Accept a list of players in the request.
     * Return the new lobby ID.
     */
    @PostMapping
    public String createLobby(@RequestBody List<Player> players) {
        return lobbyService.createLobby(players);
    }

    /**
     * Stream the simulation events (the full GameState each "turn").
     */
    @GetMapping(value = "/{lobbyId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<GameState> streamLobby(@PathVariable String lobbyId) {
        // Optionally, you could fetch the current state and send it once
        // before returning the flux. But SSE generally is continuous streaming.
        Flux<GameState> flux = lobbyService.getLobbyFlux(lobbyId);
        // In a real app, you'd throw a 404 or similar
        return Objects.requireNonNullElseGet(flux, () -> Flux.error(new IllegalArgumentException("No such lobby")));
    }

    /**
     * (Optional) Show the current full state if you want an immediate snapshot
     * without streaming.
     */
    @GetMapping("/{lobbyId}/state")
    public GameState getCurrentState(@PathVariable String lobbyId) {
        GameState state = lobbyService.getLobbyCurrentState(lobbyId);
        if (state == null) {
            throw new IllegalArgumentException("No such lobby");
        }
        return state;
    }
}
