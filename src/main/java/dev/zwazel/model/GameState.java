package dev.zwazel.model;

import lombok.Data;

import java.util.List;

@Data
public class GameState {
    private String lobbyId;
    private int turn;
    private boolean finished;

    public GameState(String lobbyId) {
        this.lobbyId = lobbyId;
        this.turn = 0;
        this.finished = false;
    }
}
