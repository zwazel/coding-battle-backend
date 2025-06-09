package dev.zwazel.DTO;

import dev.zwazel.model.Lobby;

import java.util.List;

public record AllLobbiesDTO(
        String name
) {
    public static List<AllLobbiesDTO> from(List<Lobby> lobbies) {
        return lobbies.stream()
                .map(lobby -> new AllLobbiesDTO(lobby.getName()))
                .toList();
    }
}
