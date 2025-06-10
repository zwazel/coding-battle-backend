package dev.zwazel.DTO;

import dev.zwazel.model.Lobby;
import lombok.Builder;

import java.util.List;

@Builder
public record AllLobbiesDTO(
        String name,
        int maxPlayers,
        int maxSpectators,
        int currentPlayers,
        int currentSpectators
) {
    public static List<AllLobbiesDTO> from(List<Lobby> lobbies) {
        return lobbies.stream()
                .map(lobby ->
                        AllLobbiesDTO.builder()
                                .name(lobby.getName())
                                .maxPlayers(lobby.getMaxPlayers())
                                .maxSpectators(lobby.getMaxSpectators())
                                .currentPlayers(lobby.getPlayers().size())
                                .currentSpectators(lobby.getSpectators().size())
                                .build()
                )
                .toList();
    }
}
