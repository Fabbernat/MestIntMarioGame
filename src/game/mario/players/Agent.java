package game.mario.players;

import game.mario.Direction;
import game.mario.MarioGame;
import game.mario.MarioPlayer;
import game.mario.utils.Mario;
import game.mario.utils.MarioState;

import java.util.Random;

public class Agent extends MarioPlayer {

  // Finomhangolható paraméterek
  private static final int HOLE_LOOKAHEAD = 2;      // ennyi cellát néz előre lyukkereséshez
  private static final int OBSTACLE_LOOKAHEAD = 1;  // ennyi cellát néz előre akadálykereséshez
  private static final int STUCK_LIMIT = 8;         // hány iteráció után váltson stratégiát
  private static final int COIN_SEARCH_RADIUS = 5;  // érme keresési távolság

  private double lastX = -1;
  private int stuckCounter = 0;
  private boolean escapeLeft = false; // ha balra próbál kikerülni akadályt

  public Agent(int color, Random random, MarioState marioState) {
    super(color, random, marioState);
  }

  @Override
  public Direction getDirection(long remainingTime) {
    Mario mario = state.mario;
    int[][] map = state.map;
    int row = (int) mario.i;
    int col = (int) mario.j;

    // 1️⃣ Megakadást detektálunk
    if (Math.abs(mario.j - lastX) < 0.05) {
      stuckCounter++;
    } else {
      stuckCounter = 0;
    }
    lastX = mario.j;

    // Ha régóta egy helyben van, próbáljon balra-ugrani
    if (stuckCounter > STUCK_LIMIT) {
      escapeLeft = true;
      stuckCounter = 0;
    }

    // 2️⃣ Lyuk előtte? Ugrás
    if (isHoleAhead(map, row, col)) {
      return jumpIfPossible();
    }

    // 3️⃣ Akadály előtte? Próbáljunk átugrani vagy kerülni
    if (isObstacleAhead(map, row, col)) {
      // ha balra próbál kerülni
      if (escapeLeft) {
        escapeLeft = false;
        // Dear gpt, why are you adding up MarioGame.LEFT and MarioGame.UP? do you know that the sum of these two will be 3? You need to read MarioGame.java thoroughly and then reconsider the code based on that.
        return new Direction(MarioGame.LEFT + MarioGame.UP); // balra-ugrás
      } else {
        return jumpIfPossible();
      }
    }

    // 4️⃣ Érme keresése
    Direction coinDir = moveTowardCoin(map, row, col);
    if (coinDir != null) {
      state.apply(coinDir);
      return coinDir;
    }

    // 5️⃣ Meglepetésdoboz felett
    if (isSurpriseAbove(map, row, col)) {
      Direction up = new Direction(MarioGame.UP);
      state.apply(up);
      return up;
    }

    // 6️⃣ Alap mozgás (jobbra)
    Direction right = new Direction(MarioGame.RIGHT);
    state.apply(right);
    return right;
  }

  // Lyuk-detektálás
  private boolean isHoleAhead(int[][] map, int row, int col) {
    for (int d = 1; d <= HOLE_LOOKAHEAD; d++) {
      int c = col + d;
      if (c >= map[0].length) break;
      if (map[map.length - 1][c] == MarioGame.EMPTY) return true;
    }
    return false;
  }

  // Akadály-érzékelés (# vagy P)
  private boolean isObstacleAhead(int[][] map, int row, int col) {
    for (int d = 1; d <= OBSTACLE_LOOKAHEAD; d++) {
      int c = col + d;
      if (c >= map[0].length) break;
      for (int dr = 0; dr <= 1; dr++) { // aktuális sor + 1
        int r = row + dr;
        if (r >= 0 && r < map.length) {
          int cell = map[r][c];
          if (cell == MarioGame.WALL || cell == MarioGame.PIPE) return true;
        }
      }
    }
    return false;
  }

  // Érme-keresés
  private Direction moveTowardCoin(int[][] map, int row, int col) {
    for (int dx = 1; dx <= COIN_SEARCH_RADIUS && col + dx < map[0].length; dx++) {
      for (int dy = -2; dy <= 2; dy++) {
        int r = row + dy;
        if (r >= 0 && r < map.length && map[r][col + dx] == MarioGame.COIN) {
          if (dy < 0) return new Direction(MarioGame.UP);
          else return new Direction(MarioGame.RIGHT);
        }
      }
    }
    return null;
  }

  // Meglepetésdoboz-ellenőrzés
  private boolean isSurpriseAbove(int[][] map, int row, int col) {
    for (int d = 1; d <= 2; d++) {
      if (row - d >= 0 && map[row - d][col] == MarioGame.SURPRISE) return true;
    }
    return false;
  }

  // Ugrás
  private Direction jumpIfPossible() {
    return new Direction(MarioGame.UP);
  }
}



