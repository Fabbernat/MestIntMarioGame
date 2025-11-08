import game.mario.Direction;
import game.mario.MarioGame;
import game.mario.MarioPlayer;
import game.mario.utils.Mario;
import game.mario.utils.MarioState;
import utils.State;

import java.util.Random;



public class Agent extends MarioPlayer {
  private int[][] map;
  Mario mario;
  Random random = new Random();

  public Agent(int color, Random random, State marioState) {
    super(color, random, marioState);
    this.map = new int[13][100];
    this.mario = new Mario(6, 10);
  }
  @Override
  public Direction getDirection(long remainingTime) {
    System.out.println("Hello World");


    Direction action = new Direction(MarioGame.DIRECTIONS[random.nextInt(MarioGame.DIRECTIONS.length)]);

    State state = new State(map, mario);
    MarioState s0 = new MarioState(state);

    s0.apply(new Direction(MarioGame.DIRECTIONS[3]));

    // heurisztikát készítünk?
    double heur0 = s0.score;
    System.out.println(action);
    state.apply(action);

    // lefuttatjuk az A* algoritmust a Mario jelenlegi helyzete alapján
    action = AStarAlgorithm(mario);

    return action;
  }

  // Visszatér egy Directionnal a Mario vízszintes és függőleges sebessége alapján
  private Direction AStarAlgorithm(Mario mario) {
    return new Direction((int) (mario.vi + mario.vj));
  }
}
