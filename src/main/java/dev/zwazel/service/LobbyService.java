package dev.zwazel.service;

import dev.zwazel.DTO.CreateLobbyRequestDTO;
import dev.zwazel.model.Lobby;
import dev.zwazel.model.LobbyEvent;
import dev.zwazel.model.LobbyEventType;
import dev.zwazel.model.LobbyUser;
import dev.zwazel.repository.BotRepository;
import dev.zwazel.repository.UserRepository;
import dev.zwazel.security.CustomUserPrincipal;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final BotRepository botRepository;

    // A simple in-memory map of lobbyId -> Lobby
    private final Map<String, LobbyData> lobbies = new ConcurrentHashMap<>();

    @Value("${lobby.players.max}")
    private int maxPlayers;

    @Value("${lobby.players.min}")
    private int minPlayers;

    /**
     * Create a new lobby with a generated ID.
     */
    public Lobby createLobby(CreateLobbyRequestDTO request, CustomUserPrincipal loggedInUser) {
        // Validate the request
        if (request.lobbyname() == null || request.lobbyname().isEmpty()) {
            throw new IllegalArgumentException("Lobby name cannot be empty");
        }

        if (request.maxPlayers() < minPlayers || request.maxPlayers() > maxPlayers) {
            throw new IllegalArgumentException("Max players must be between " + minPlayers + " and " + maxPlayers);
        }

        // Check if the user exists in the repository
        if (!userRepository.existsById(loggedInUser.getId())) {
            throw new EntityNotFoundException("User does not exist");
        }

        // Check if the bot exists
        if (request.selectedBotId() != null && !botRepository.existsById(request.selectedBotId())) {
            throw new EntityNotFoundException("Bot does not exist");
        }

        // Check if the lobby name already exists (ignoring case)
        if (lobbies.containsKey(request.lobbyname().toLowerCase())) {
            throw new IllegalArgumentException("Lobby name already exists");
        }

        LobbyUser hostUser = LobbyUser.builder()
                .userId(loggedInUser.getId())
                .username(loggedInUser.getUsername())
                .isHost(true)
                .selectedBotId(request.selectedBotId())
                .build();

        Lobby lobby = Lobby.builder()
                .name(request.lobbyname())
                .maxPlayers(request.maxPlayers())
                .players(List.of(hostUser))
                .spectatorsAllowed(request.spectatorsAllowed())
                .build();

        // Create a sink that replays the last event for new subscribers
        Sinks.Many<LobbyEvent> sink = Sinks.many().replay().latest();

        // Emit an initial WAITING event
        sink.tryEmitNext(new LobbyEvent(LobbyEventType.WAITING,
                "Lobby created, waiting for simulation to start"));

        lobbies.put(request.lobbyname().toLowerCase(), new LobbyData(lobby, sink));

        return lobby;
    }

    /**
     * Get an existing lobby by ID (or null if not found).
     */
    public Lobby getLobby(String lobbyId) {
        LobbyData data = lobbies.get(lobbyId.toLowerCase());
        return (data != null) ? data.lobby : null;
    }

    public Sinks.Many<LobbyEvent> getSink(String lobbyId) {
        LobbyData data = lobbies.get(lobbyId.toLowerCase());
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
        lobbies.remove(lobbyId.toLowerCase());
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
