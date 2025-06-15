package dev.zwazel.api.model.DTO;

import dev.zwazel.model.Lobby;
import lombok.Builder;

@Builder
public record AllLobbiesDTO(
        String name,
        int maxPlayers,
        int currentPlayers,
        int currentSpectators
) {
    public static AllLobbiesDTO from(Lobby lobby) {
        return AllLobbiesDTO.builder()
                .name(lobby.getName())
                .maxPlayers(lobby.getMaxPlayers())
                .currentPlayers(lobby.getPlayers().size())
                .currentSpectators(lobby.getSpectators().size())
                .build();
    }
}
