package dev.zwazel;

public class RockPaperScissorsGame {

    private final GameState state;

    public RockPaperScissorsGame() {
        this.state = new GameState();
    }

    public GameState getState() {
        return state;
    }

    public void playRound(Move bot1Move, Move bot2Move) {
        System.out.println("Round " + state.getRound() + ": "
                + "Bot1=" + bot1Move + ", "
                + "Bot2=" + bot2Move);
        // Compare moves and update scores
        if (bot1Move == bot2Move) {
            // tie - no score change
            System.out.println("Round " + state.getRound() + " is a tie");
        } else if (
                (bot1Move == Move.ROCK && bot2Move == Move.SCISSORS) ||
                        (bot1Move == Move.PAPER && bot2Move == Move.ROCK) ||
                        (bot1Move == Move.SCISSORS && bot2Move == Move.PAPER)
        ) {
            state.incrementBot1Score();
            System.out.println("Bot1 wins round " + state.getRound());
        } else {
            state.incrementBot2Score();
            System.out.println("Bot2 wins round " + state.getRound());
        }

        // Increment the round count
        state.incrementRound();
    }

    public String getScores() {
        return "Round " + state.getRound() + ": "
                + "Bot1=" + state.getBot1Score() + ", "
                + "Bot2=" + state.getBot2Score();
    }
}