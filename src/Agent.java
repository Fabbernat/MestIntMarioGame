import game.mario.Direction;
import game.mario.MarioGame;
import utils.State;

import java.util.Random;


public class Agent implements IAgent {

  @Override
  public Direction getDirection(long remainingTime) {
    System.out.println("Hello World");


    Random random = new Random();
    Direction action = new Direction(MarioGame.DIRECTIONS[random.nextInt(MarioGame.DIRECTIONS.length)]);
    Direction action2 = new Direction(0);
    State state = new State();
    state.apply(action2);
    return action;
  }
}
