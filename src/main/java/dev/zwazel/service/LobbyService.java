package dev.zwazel.service;

import dev.zwazel.model.GameState;
import dev.zwazel.model.Player;
import org.springframework.stereotype.Service;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LobbyService {

    // Holds the "hot flux" + the current game state for each lobby
    static class LobbyInfo {
        GameState gameState;
        ConnectableFlux<GameState> flux;

        LobbyInfo(GameState gameState, ConnectableFlux<GameState> flux) {
            this.gameState = gameState;
            this.flux = flux;
        }
    }

    private final Map<String, LobbyInfo> lobbies = new ConcurrentHashMap<>();

    /**
     * Create a new lobby with a generated ID and a list of players.
     * This method also starts the simulation "hot flux" so it continues running
     * regardless of subscriber count.
     */
    public String createLobby(List<Player> players) {
        String lobbyId = UUID.randomUUID().toString();
        GameState initialState = new GameState(lobbyId, players);

        // Create a "hot" flux that updates the GameState every second until finished
        // replay(1) ensures new subscribers see the *latest* GameState immediately
        ConnectableFlux<GameState> connectableFlux = Flux.interval(Duration.ofSeconds(1))
                .map(tick -> {
                    updateGameState(initialState);
                    return initialState;
                })
                // Optionally stop if "finished" is true
                .takeUntil(GameState::isFinished)
                .replay(1);

        // Start emitting immediately
        connectableFlux.connect();

        // Put in map
        lobbies.put(lobbyId, new LobbyInfo(initialState, connectableFlux));
        return lobbyId;
    }

    /**
     * Return the flux for a given lobby. If it doesn't exist, return null or throw an exception.
     */
    public Flux<GameState> getLobbyFlux(String lobbyId) {
        LobbyInfo info = lobbies.get(lobbyId);
        if (info == null) return null;

        // Return the flux (new subscribers get the current/last state + subsequent)
        return info.flux;
    }

    /**
     * Return the *current* full state for a given lobby (for example,
     * if you want to show the entire state on connect before streaming).
     */
    public GameState getLobbyCurrentState(String lobbyId) {
        LobbyInfo info = lobbies.get(lobbyId);
        if (info == null) return null;
        return info.gameState;
    }

    /**
     * Simple example logic that increments the turn count each tick
     * and marks the game finished if turn reaches 5, for instance.
     */
    private void updateGameState(GameState state) {
        if (state.isFinished()) {
            // No-op if already finished
            return;
        }
        state.setTurn(state.getTurn() + 1);

        // Example condition to stop
        if (state.getTurn() >= 5) {
            state.setFinished(true);
        }
    }
}
