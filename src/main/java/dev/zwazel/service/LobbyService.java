package dev.zwazel.service;

import dev.zwazel.DTO.CreateLobbyRequestDTO;
import dev.zwazel.model.Lobby;
import dev.zwazel.model.LobbyEvent;
import dev.zwazel.model.LobbyEventType;
import dev.zwazel.model.LobbyUser;
import dev.zwazel.repository.UserRepository;
import dev.zwazel.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class LobbyService {
    private final UserRepository userRepository;

    // A simple in-memory map of lobbyId -> Lobby
    private final Map<String, LobbyData> lobbies = new ConcurrentHashMap<>();

    /**
     * Create a new lobby with a generated ID.
     */
    public Lobby createLobby(CreateLobbyRequestDTO request, CustomUserPrincipal loggedInUser) {
        LobbyUser hostUser = LobbyUser.builder()
                .userId(loggedInUser.getId())
                .username(loggedInUser.getUsername())
                .isHost(true)
                .build();

        Lobby lobby = Lobby.builder()
                .name(request.lobbyname())
                .maxPlayers(request.maxPlayers())
                .maxSpectators(request.maxSpectators())
                .players(List.of(hostUser))
                .build();

        // Create a sink that replays the last event for new subscribers
        Sinks.Many<LobbyEvent> sink = Sinks.many().replay().latest();

        // Emit an initial WAITING event
        sink.tryEmitNext(new LobbyEvent(LobbyEventType.WAITING,
                "Lobby created, waiting for simulation to start"));

        // TODO: ensure lobbyname is unique, maybe throw an exception if it already exists

        lobbies.put(request.lobbyname(), new LobbyData(lobby, sink));

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

    public void removeLobby(String lobbyId) {
        lobbies.remove(lobbyId);
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
