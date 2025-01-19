package dev.zwazel.service;

import dev.zwazel.model.GameState;
import dev.zwazel.model.Lobby;
import dev.zwazel.model.LobbyEvent;
import dev.zwazel.model.LobbyEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class SimulationService {

    private final LobbyService lobbyService;

    // Keep track of running "turn loops" so we don't start duplicates
    private final Map<String, Disposable> runningSimulations = new ConcurrentHashMap<>();

    /**
     * Start the simulation for the given lobby if not already started.
     * We'll run a turn loop every second until turn >= 5.
     */
    public void startSimulation(String lobbyId) {
        if (runningSimulations.containsKey(lobbyId)) {
            // Already running
            return;
        }

        Lobby lobby = lobbyService.getLobby(lobbyId);
        if (lobby == null) {
            throw new IllegalArgumentException("No such lobby");
        }

        var sink = lobbyService.getSink(lobbyId);
        if (sink == null) {
            throw new IllegalArgumentException("No sink found for lobby");
        }

        // First, send SIMULATION_STARTED
        sink.tryEmitNext(new LobbyEvent(LobbyEventType.SIMULATION_STARTED,
                "Simulation has started"));

        // Create a GameState
        GameState state = new GameState(lobbyId, lobby.getPlayers());

        // Start a "turn loop" that ticks every 1 second
        Disposable disposable = Flux.interval(Duration.ofSeconds(1))
                .subscribe(tick -> {
                    updateGameState(state, sink);
                });

        // Keep reference to dispose it later, if needed
        runningSimulations.put(lobbyId, disposable);
    }

    private void updateGameState(GameState state, Sinks.Many<LobbyEvent> sink) {
        if (state.isFinished()) {
            return; // no further updates
        }

        state.setTurn(state.getTurn() + 1);

        // Emit TURN_UPDATE
        sink.tryEmitNext(new LobbyEvent(LobbyEventType.TURN_UPDATE, state));

        if (state.getTurn() >= 5) {
            state.setFinished(true);

            // Emit SIMULATION_FINISHED
            sink.tryEmitNext(new LobbyEvent(LobbyEventType.SIMULATION_FINISHED, state));

            // (Optional) Stop the turn loop by disposing
            Disposable d = runningSimulations.get(state.getLobbyId());
            if (d != null) {
                d.dispose();
                runningSimulations.remove(state.getLobbyId());
            }
        }
    }
}