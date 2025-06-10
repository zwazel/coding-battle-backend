package dev.zwazel.DTO;

public record CreateLobbyRequestDTO(
        String lobbyname,
        int maxPlayers,
        int maxSpectators
) {
}
