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
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationService {

    private final LobbyService lobbyService;
    private final Map<String, Disposable> runningSimulations = new ConcurrentHashMap<>();

    public Mono<Void> startSimulation(String lobbyId) {
        if (runningSimulations.containsKey(lobbyId)) {
            return Mono.empty();
        }

        return lobbyService.getLobby(lobbyId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("No such lobby")))
                .flatMap(lobby -> {
                    if (lobby.getGameState() != null && lobby.getGameState().isFinished()) {
                        return Mono.error(new IllegalStateException(
                                "Simulation already finished for lobby: " + lobbyId));
                    }
                    return lobbyService.getSink(lobbyId)
                            .switchIfEmpty(Mono.error(new IllegalArgumentException("No sink found")))
                            .map(sink -> Tuples.of(lobby, sink));
                })
                .flatMap(tuple -> {

                    Lobby lobby = tuple.getT1();
                    Sinks.Many<LobbyEvent> s = tuple.getT2();

                    GameState state = new GameState(lobbyId);
                    lobby.setGameState(state);
                    s.tryEmitNext(new LobbyEvent(LobbyEventType.SIMULATION_STARTED, "Simulation has started"));

                    return Flux.interval(Duration.ofSeconds(1))
                            .flatMap(t -> updateTurn(state, s))
                            .takeUntil(Boolean::booleanValue)
                            .then(lobbyService.removeLobby(lobbyId))
                            .doOnSubscribe(sub -> {     // 🔥 add to map
                                Disposable d = sub::cancel;
                                runningSimulations.put(lobbyId, d);
                            })
                            .doFinally(sig ->           // 🧹 remove when done
                                    runningSimulations.remove(lobbyId));
                })
                .then();  // still returns Mono<Void>
    }

    private Mono<Boolean> updateTurn(GameState state, Sinks.Many<LobbyEvent> sink) {
        if (state.isFinished()) return Mono.just(true);

        state.setTurn(state.getTurn() + 1);
        sink.tryEmitNext(new LobbyEvent(LobbyEventType.TURN_UPDATE, state));

        if (state.getTurn() >= 5) {
            state.setFinished(true);
            sink.tryEmitNext(new LobbyEvent(LobbyEventType.SIMULATION_FINISHED, state));
            return Mono.just(true);
        }
        return Mono.just(false);
    }
}
