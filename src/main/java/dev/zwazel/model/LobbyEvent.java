package dev.zwazel.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LobbyEvent {
    private LobbyEventType type;

    // 'payload' can be a string message, a GameState, etc.
    private Object payload;
}
