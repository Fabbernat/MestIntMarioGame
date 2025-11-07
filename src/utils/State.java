package utils;

import game.mario.Direction;
import game.mario.utils.Mario;
import game.mario.utils.MarioState;

public class State extends MarioState {
  public State(int[][] map, Mario mario) {
    super(map, mario);
  }

  public State(MarioState state) {
    super(state);
  }

  public boolean apply(Direction action2) {
    return false;
  }
}
