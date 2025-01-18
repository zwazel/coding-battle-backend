import dev.zwazel.BotInterface;
import dev.zwazel.BotLoader;
import dev.zwazel.Move;
import dev.zwazel.RockPaperScissorsGame;

public static void main(String[] args) throws Exception {
    // Initialize the game
    RockPaperScissorsGame rpsGame = new RockPaperScissorsGame();

    // Load two bots
    BotInterface bot1 = BotLoader.loadBot("submissions/MyBot1.java");
    BotInterface bot2 = BotLoader.loadBot("submissions/MyBot2.java");

    // Run a simple 10-round match
    for(int round = 0; round < 10; round++) {
        Move move1 = bot1.makeMove(rpsGame.getState());
        Move move2 = bot2.makeMove(rpsGame.getState());
        rpsGame.playRound(move1, move2);
    }

    // Show results
    System.out.println("Final scores: " + rpsGame.getScores());
}