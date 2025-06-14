package dev.zwazel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class Lobby {
    private final String name;
    private final List<LobbyUser> players;
    private final List<LobbyUser> spectators = List.of();
    private int maxPlayers;
    private boolean spectatorsAllowed;

    @JsonIgnore
    private GameState gameState;
}