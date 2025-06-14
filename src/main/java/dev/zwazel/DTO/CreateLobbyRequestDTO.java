package dev.zwazel.DTO;

import java.util.UUID;

public record CreateLobbyRequestDTO(
        String lobbyname,
        UUID selectedBotId,
        int maxPlayers,
        boolean spectatorsAllowed
) {
}
