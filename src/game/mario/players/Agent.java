package game.mario.players;

import game.mario.Direction;
import game.mario.MarioGame;
import game.mario.MarioPlayer;
import game.mario.utils.Mario;
import game.mario.utils.MarioState;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Agent extends MarioPlayer {

  // Finomhangolható paraméterek
  private static final int HOLE_LOOKAHEAD = 2;
  private static final int OBSTACLE_LOOKAHEAD = 1;
  private static final int COIN_SEARCH_RADIUS = 5;
  private static final int STUCK_HISTORY = 16; // hány pozíciót tartson meg
  private static final double STUCK_REGION_SIZE = 6.0; // kb. hány cellányi helyen belül van "beragadva"

  private final Queue<Double> lastPositions = new LinkedList<>();
  private boolean escapeLeft = false; // balra menekülés állapota
  private boolean jumpPhase = false;  // ha épp az "UP" fázisban van
  private double lastX = -1;
  private int samePosCounter = 0;

  public Agent(int color, Random random, MarioState marioState) {
    super(color, random, marioState);
  }

  @Override
  public Direction getDirection(long remainingTime) {
    Mario mario = state.mario;
    int[][] map = state.map;
    int row = (int) mario.i;
    int col = (int) mario.j;

    updatePositionHistory(mario);

    // --- 1️⃣ Stuck detection ---
    if (isStuck()) {
      escapeLeft = true;
      jumpPhase = true;
      lastPositions.clear(); // reset history to prevent loops
    }

    // --- 2️⃣ Ha menekül balra ---
    if (escapeLeft) {
      // először próbál ugrani
      if (jumpPhase) {
        jumpPhase = false;
        return new Direction(MarioGame.UP);
      }

      // Step 2: Move left safely (avoid holes)
      if (!isHoleBehind(map, row, col)) {
        return new Direction(MarioGame.LEFT);
      } else {
        // If there's a hole behind, abort left escape
        escapeLeft = false;
      }
    }

    // --- 3️⃣ Lyuk előtte? ---
    if (isHoleAhead(map, row, col)) {
      // If Mario is right before a hole, jump instead of walking forward
      return jumpIfPossible();
    }

    // --- 4️⃣ Akadály előtte? ---
    if (isObstacleAhead(map, row, col)) {
      return jumpIfPossible();
    }

    // --- 5️⃣ Érme keresés ---
    Direction coinDir = moveTowardCoin(map, row, col);
    if (coinDir != null) {
      state.apply(coinDir);
      return coinDir;
    }

    // --- 6️⃣ Meglepetésdoboz felett ---
    if (isSurpriseAbove(map, row, col)) {
      Direction up = new Direction(MarioGame.UP);
      state.apply(up);
      return up;
    }

    // --- 7️⃣ Normal movement (avoid walking into holes) ---
    if (!isHoleAhead(map, row, col)) {
      Direction right = new Direction(MarioGame.RIGHT);
      state.apply(right);
      return right;
    } else {
      // If there’s a hole, jump to clear it
      return new Direction(MarioGame.UP);
    }
  }

  // --- Utility Methods ---

  private void updatePositionHistory(Mario mario) {
    // Track Mario's horizontal position
    lastPositions.add(mario.j);
    if (lastPositions.size() > STUCK_HISTORY) {
      lastPositions.poll();
    }

    // Detect completely identical positions (like pressed against a wall)
    if (Math.abs(mario.j - lastX) < 0.05) {
      samePosCounter++;
    } else {
      samePosCounter = 0;
    }
    lastX = mario.j;

    // Emergency trigger: if stuck 8 frames in exactly same position
    if (samePosCounter > 8) {
      escapeLeft = true;
      jumpPhase = true;
      samePosCounter = 0;
    }
  }

  private boolean isHoleAhead(int[][] map, int row, int col) {
    for (int d = 1; d <= HOLE_LOOKAHEAD; d++) {
      int c = col + d;
      if (c >= map[0].length) break;
      if (map[map.length - 1][c] == MarioGame.EMPTY) return true;
    }
    return false;
  }

  private boolean isHoleBehind(int[][] map, int row, int col) {
    for (int d = 1; d <= HOLE_LOOKAHEAD; d++) {
      int c = col - d;
      if (c < 0) break;
      if (map[map.length - 1][c] == MarioGame.EMPTY) return true;
    }
    return false;
  }

  private boolean isObstacleAhead(int[][] map, int row, int col) {
    for (int d = 1; d <= OBSTACLE_LOOKAHEAD; d++) {
      int c = col + d;
      if (c >= map[0].length) break;
      for (int dr = 0; dr <= 1; dr++) {
        int r = row + dr;
        if (r >= 0 && r < map.length) {
          int cell = map[r][c];
          if (cell == MarioGame.WALL || cell == MarioGame.PIPE) return true;
        }
      }
    }
    return false;
  }

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

  private boolean isSurpriseAbove(int[][] map, int row, int col) {
    for (int d = 1; d <= 2; d++) {
      if (row - d >= 0 && map[row - d][col] == MarioGame.SURPRISE) return true;
    }
    return false;
  }

  private Direction jumpIfPossible() {
    return new Direction(MarioGame.UP);
  }

  private boolean isStuck() {
    if (lastPositions.size() < STUCK_HISTORY) return false;

    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;
    for (double x : lastPositions) {
      min = Math.min(min, x);
      max = Math.max(max, x);
    }
    return (max - min) <= STUCK_REGION_SIZE;
  }
}
