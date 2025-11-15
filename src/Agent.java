import game.mario.Direction;
import game.mario.MarioGame;
import game.mario.MarioPlayer;
import game.mario.utils.Mario;
import game.mario.utils.MarioState;

import java.util.Random;

public class Agent extends MarioPlayer {

  public Agent(int color, Random random, MarioState marioState) {
    super(color, random, marioState);
  }

  @Override
  public Direction getDirection(long remainingTime) {
    Mario mario = state.mario; // aktuális Mario állapot
    int[][] map = state.map; // 13x100-as pálya
    int marioRow = (int) mario.i;
    int marioCol = (int) mario.j;

    // Elsődleges cél: jobbra haladás, kerülve a lyukakat
    if (isHoleAhead(map, marioRow, marioCol)) {
      // ha lyuk van előtte, ugorjunk
      return jumpIfPossible();
    }

    // Másodlagos cél: ha érme van jobbra, menj felé
    Direction coinDir = moveTowardCoin(map, marioRow, marioCol);
    if (coinDir != null) {
      state.apply(coinDir);
      return coinDir;
    }

    // Ha meglepetésdoboz van felette, próbáljuk meg kiütni
    if (isSurpriseAbove(map, marioRow, marioCol)) {
      Direction up = new Direction(MarioGame.UP);
      state.apply(up);
      return up;
    }

    // Alap viselkedés: menj jobbra
    Direction right = new Direction(MarioGame.RIGHT);
    state.apply(right);
    return right;
  }

  /**
   * Megnézi, hogy van-e lyuk Mario előtt 1-2 cellányira.
   */
  private boolean isHoleAhead(int[][] map, int row, int col) {
    // Pálya határok védelme
    if (col + 1 >= map[0].length) return false;

    for (int lookahead = 1; lookahead <= 2; lookahead++) {
      int checkCol = col + lookahead;
      // az alsó sorban üres hely lyuknak számít
      if (checkCol < map[0].length && map[map.length - 1][checkCol] == MarioGame.EMPTY) {
        return true;
      }
    }
    return false;
  }

  /**
   * Ha érmét lát a közelben, kiválasztja az irányt, amerre érdemes menni.
   */
  private Direction moveTowardCoin(int[][] map, int row, int col) {
    int searchRadius = 4; // néhány cellát előre nézünk

    for (int dx = 1; dx <= searchRadius && col + dx < map[0].length; dx++) {
      for (int dy = -2; dy <= 2; dy++) {
        int targetRow = row + dy;
        if (targetRow >= 0 && targetRow < map.length) {
          if (map[targetRow][col + dx] == MarioGame.COIN) {
            if (dy < 0) {
              // érme fent -> ugrás
              return new Direction(MarioGame.UP);
            } else {
              // érme vízszintesen -> jobbra mozgás
              return new Direction(MarioGame.RIGHT);
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Megnézi, hogy van-e meglepetésdoboz Mario felett.
   */
  private boolean isSurpriseAbove(int[][] map, int row, int col) {
    if (row - 1 >= 0 && map[row - 1][col] == MarioGame.SURPRISE) {
      return true;
    }
    if (row - 2 >= 0 && map[row - 2][col] == MarioGame.SURPRISE) {
      return true;
    }
    return false;
  }

  /**
   * Megpróbál ugrani (pl. ha lyuk van előtte vagy akadály felette).
   */
  private Direction jumpIfPossible() {
    return new Direction(MarioGame.UP);
  }

}
