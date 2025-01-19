package dev.zwazel.service;

import dev.zwazel.model.Lobby;
import dev.zwazel.model.LobbyEvent;
import dev.zwazel.model.LobbyEventType;
import dev.zwazel.model.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class LobbyService {
    // A simple in-memory map of lobbyId -> Lobby
    private final Map<String, LobbyData> lobbies = new ConcurrentHashMap<>();

    /**
     * Create a new lobby with a generated ID.
     */
    public Lobby createLobby(List<Player> players) {
        String lobbyId = UUID.randomUUID().toString();
        Lobby lobby = new Lobby(lobbyId, players);

        // Create a sink that replays the last event for new subscribers
        Sinks.Many<LobbyEvent> sink = Sinks.many().replay().latest();

        // Emit an initial WAITING event
        sink.tryEmitNext(new LobbyEvent(LobbyEventType.WAITING,
                "Lobby created, waiting for simulation to start"));

        lobbies.put(lobbyId, new LobbyData(lobby, sink));

        return lobby;
    }

    /**
     * Get an existing lobby by ID (or null if not found).
     */
    public Lobby getLobby(String lobbyId) {
        LobbyData data = lobbies.get(lobbyId);
        return (data != null) ? data.lobby : null;
    }

    public Sinks.Many<LobbyEvent> getSink(String lobbyId) {
        LobbyData data = lobbies.get(lobbyId);
        return (data != null) ? data.sink : null;
    }

    public List<Lobby> getAllLobbies() {
        List<Lobby> result = new ArrayList<>();
        for (LobbyData ld : lobbies.values()) {
            // TODO: Only return ID of the lobby, no additional data
            result.add(ld.lobby);
        }
        return result;
    }

    // We’ll store both the Lobby object and the SSE sink in one container
    static class LobbyData {
        Lobby lobby;
        Sinks.Many<LobbyEvent> sink;

        LobbyData(Lobby lobby, Sinks.Many<LobbyEvent> sink) {
            this.lobby = lobby;
            this.sink = sink;
        }
    }
}
