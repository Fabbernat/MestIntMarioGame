import java.util.Random;

import game.mario.Direction;
import game.mario.MarioGame;
import game.mario.MarioPlayer;
import game.mario.utils.MarioState;

public class SamplePlayer extends MarioPlayer {

  public SamplePlayer(int color, Random random, MarioState state) {
    super(color, random, state);
  }

  @Override
  public Direction getDirection(long remainingTime) {
    System.out.println("Hello World");


    Direction action = new Direction(MarioGame.DIRECTIONS[random.nextInt(MarioGame.DIRECTIONS.length)]);
    Direction action2 = new Direction(0);
    state.apply(action2);
    return action;
  }
}