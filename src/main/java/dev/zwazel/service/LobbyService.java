package dev.zwazel.service;

import dev.zwazel.api.model.DTO.CreateLobbyRequestDTO;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class LobbyService {

    private final UserRepository userRepository;   // ⚡ reactive
    private final BotRepository botRepository;     // ⚡ reactive

    private final Map<String, LobbyData> lobbies = new ConcurrentHashMap<>();

    @Value("${lobby.players.max}")
    private int maxPlayers;

    @Value("${lobby.players.min}")
    private int minPlayers;

    /*─────────────────────────────────────────────────────
     *  CREATE LOBBY  –  returns Mono<Lobby>
     *────────────────────────────────────────────────────*/
    public Mono<Lobby> createLobby(CreateLobbyRequestDTO req,
                                   CustomUserPrincipal loggedInUser) {

        final String key = (req.lobbyname() == null) ? "" : req.lobbyname().toLowerCase();

        /* Stage 1 – cheap in‑memory validations */
        Mono<CreateLobbyRequestDTO> validated = Mono.defer(() -> {
            if (key.isBlank())
                return Mono.error(new IllegalArgumentException("Lobby name cannot be empty"));
            if (req.maxPlayers() < minPlayers || req.maxPlayers() > maxPlayers)
                return Mono.error(new IllegalArgumentException(
                        "Max players must be between %d and %d".formatted(minPlayers, maxPlayers)));
            if (lobbies.containsKey(key))
                return Mono.error(new IllegalArgumentException("Lobby name already exists"));
            return Mono.just(req);           // pass downstream
        });

        /* Stage 2 – reactive repository checks */
        return validated
                .flatMap(r -> userRepository.existsById(loggedInUser.getId())
                        .filter(Boolean::booleanValue)
                        .switchIfEmpty(Mono.error(
                                new EntityNotFoundException("User does not exist")))
                        .thenReturn(r))
                .flatMap(r -> {
                    if (r.selectedBotId() == null) return Mono.just(r);
                    return botRepository.existsById(r.selectedBotId())
                            .filter(Boolean::booleanValue)
                            .switchIfEmpty(Mono.error(
                                    new EntityNotFoundException("Bot does not exist")))
                            .thenReturn(r);
                })
                /* Stage 3 – build lobby & register in map (synchronous, CPU‑cheap) */
                .map(r -> {
                    LobbyUser host = LobbyUser.builder()
                            .userId(loggedInUser.getId())
                            .username(loggedInUser.getUsername())
                            .isHost(true)
                            .selectedBotId(r.selectedBotId())
                            .build();

                    Lobby lobby = Lobby.builder()
                            .name(r.lobbyname())
                            .maxPlayers(r.maxPlayers())
                            .players(List.of(host))
                            .spectatorsAllowed(r.spectatorsAllowed())
                            .build();

                    Sinks.Many<LobbyEvent> sink = Sinks.many().replay().latest();
                    sink.tryEmitNext(new LobbyEvent(
                            LobbyEventType.WAITING,
                            "Lobby created, waiting for simulation to start"));

                    lobbies.put(key, new LobbyData(lobby, sink));
                    return lobby;
                });
    }

    /*─────────────────────────────────────────────────────
     *  QUERY HELPERS
     *────────────────────────────────────────────────────*/
    public Mono<Lobby> getLobby(String lobbyId) {
        return Mono.justOrEmpty(lobbies.get(lobbyId.toLowerCase()))
                .map(ld -> ld.lobby);
    }

    public Mono<Sinks.Many<LobbyEvent>> getSink(String lobbyId) {
        return Mono.justOrEmpty(lobbies.get(lobbyId.toLowerCase()))
                .map(ld -> ld.sink);
    }

    public Flux<Lobby> getAllLobbies() {
        return Flux.fromIterable(lobbies.values())
                .map(ld -> ld.lobby);
    }

    public Mono<Void> removeLobby(String lobbyId) {
        return Mono.fromRunnable(() -> lobbies.remove(lobbyId.toLowerCase()));
    }

    /*─────────────────────────────────────────────────────
     *  INTERNAL DATA HOLDER
     *────────────────────────────────────────────────────*/
    static class LobbyData {
        final Lobby lobby;
        final Sinks.Many<LobbyEvent> sink;

        LobbyData(Lobby lobby, Sinks.Many<LobbyEvent> sink) {
            this.lobby = lobby;
            this.sink = sink;
        }
    }
}
