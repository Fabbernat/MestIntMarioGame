import game.mario.Direction;
import game.mario.MarioGame;
import game.mario.MarioPlayer;
import game.mario.utils.Mario;
import utils.State;

import java.util.Random;

public class Agent extends MarioPlayer {

  public Agent(int color, Random random, State marioState) {
    super(color, random, marioState);
  }
  @Override
  public Direction getDirection(long remainingTime) {
    Direction action = new Direction(MarioGame.DIRECTIONS[random.nextInt(MarioGame.DIRECTIONS.length-1)]);
    state.apply(action);
    return action;
  }

  // Visszatér egy Directionnal a Mario vízszintes és függőleges sebessége alapján
  private Direction AStarAlgorithm(Mario mario) {
    return new Direction((int) (mario.vi + mario.vj));
  }
}
