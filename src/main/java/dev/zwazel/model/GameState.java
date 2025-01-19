package dev.zwazel.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GameState {
    private String lobbyId;
    private int turn;
    private boolean finished;
    private List<Player> players;

    public GameState(String lobbyId, List<Player> players) {
        this.lobbyId = lobbyId;
        this.players = players;
        this.turn = 0;
        this.finished = false;
    }
}
