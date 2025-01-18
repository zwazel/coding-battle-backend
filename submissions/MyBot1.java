import dev.zwazel.BotInterface;
import dev.zwazel.GameState;
import dev.zwazel.Move;

import java.util.Random;

public class MyBot1 implements BotInterface {

    private final Random random = new Random();

    @Override
    public Move makeMove(GameState state) {
        // Simple random strategy: pick among ROCK, PAPER, SCISSORS with equal probability
        int choice = random.nextInt(3);

        switch (choice) {
            case 0:
                return Move.ROCK;
            case 1:
                return Move.PAPER;
            default:
                return Move.SCISSORS;
        }
    }
}