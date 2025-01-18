package dev.zwazel;

public class GameState {
    private int round;
    private int bot1Score;
    private int bot2Score;

    public GameState() {
        this.round = 0;
        this.bot1Score = 0;
        this.bot2Score = 0;
    }

    public int getRound() {
        return round;
    }

    public int getBot1Score() {
        return bot1Score;
    }

    public int getBot2Score() {
        return bot2Score;
    }

    public void incrementRound() {
        round++;
    }

    public void incrementBot1Score() {
        bot1Score++;
    }

    public void incrementBot2Score() {
        bot2Score++;
    }
}