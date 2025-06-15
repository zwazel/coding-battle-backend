package dev.zwazel.api.model.DTO;

import dev.zwazel.model.Lobby;
import lombok.Builder;

import java.util.List;

@Builder
public record AllLobbiesDTO(
        String name,
        int maxPlayers,
        int currentPlayers,
        int currentSpectators
) {
    public static List<AllLobbiesDTO> from(List<Lobby> lobbies) {
        return lobbies.stream()
                .map(lobby ->
                        AllLobbiesDTO.builder()
                                .name(lobby.getName())
                                .maxPlayers(lobby.getMaxPlayers())
                                .currentPlayers(lobby.getPlayers().size())
                                .currentSpectators(lobby.getSpectators().size())
                                .build()
                )
                .toList();
    }
}
