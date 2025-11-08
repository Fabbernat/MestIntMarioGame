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

  // Tunable parameters
  private static final int HOLE_LOOKAHEAD = 2;
  private static final int OBSTACLE_LOOKAHEAD = 1;
  private static final int COIN_SEARCH_RADIUS = 5;
  private static final int STUCK_HISTORY = 16;
  private static final double STUCK_REGION_SIZE = 6.0;

  private final Queue<Double> lastPositions = new LinkedList<>();
  private boolean escapeLeft = false;
  private boolean jumpPhase = false;
  private double lastX = -1;
  private int samePosCounter = 0;
  private int escapeTicks = 0; // controls duration of "escape mode"

  // New fields for the left movement mechanic
  private int moveCounter = 0;
  private int leftMovesRemaining = 0;

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

    // --- NEW: Handle forced left movements after every 10th move ---
    if (leftMovesRemaining > 0) {
      leftMovesRemaining--;
      return new Direction(MarioGame.LEFT);
    }

    // Increment move counter and check if we should trigger left movements
    moveCounter++;
    if (moveCounter >= 10) {
      moveCounter = 0;
      leftMovesRemaining = 4;
      return new Direction(MarioGame.LEFT);
    }

    // --- 1️⃣ Stuck detection ---
    if (isStuck()) {
      escapeLeft = true;
      jumpPhase = true;
      escapeTicks = 12; // stay in escape mode for a few frames
      lastPositions.clear();
    }

    // --- 2️⃣ Escape mode ---
    if (escapeLeft) {
      if (jumpPhase) {
        jumpPhase = false;
        return new Direction(MarioGame.UP);
      }

      // Try to move left if possible
      if (!isHoleBehind(map, row, col)) {
        escapeTicks--;
        if (escapeTicks <= 0) escapeLeft = false;
        return new Direction(MarioGame.LEFT);
      } else {
        // If hole behind, stop escaping
        escapeLeft = false;
      }
    }

    // --- 3️⃣ Check for holes ---
    if (isHoleAhead(map, row, col)) {
      // If hole is very close and ground below is missing → jump
      if (!hasGroundBelow(map, row, col + 1)) {
        return new Direction(MarioGame.UP);
      }
      // If still on solid ground, stop or jump cautiously
      return new Direction(MarioGame.UP);
    }

    // --- 4️⃣ Check for obstacles ---
    if (isObstacleAhead(map, row, col)) {
      return new Direction(MarioGame.UP);
    }

    // --- 5️⃣ Coin search ---
    Direction coinDir = moveTowardCoin(map, row, col);
    if (coinDir != null) {
      state.apply(coinDir);
      return coinDir;
    }

    // --- 6️⃣ Surprise blocks above ---
    if (isSurpriseAbove(map, row, col)) {
      Direction up = new Direction(MarioGame.UP);
      state.apply(up);
      return up;
    }

    // --- 7️⃣ Regular movement ---
    if (!isHoleAhead(map, row, col)) {
      if (hasGroundBelow(map, row, col)) {
        Direction right = new Direction(MarioGame.RIGHT);
        state.apply(right);
        return right;
      } else {
        // avoid walking mid-air
        return new Direction(MarioGame.UP);
      }
    } else {
      return new Direction(MarioGame.UP);
    }
  }

  // --- Utility methods ---

  private void updatePositionHistory(Mario mario) {
    lastPositions.add(mario.j);
    if (lastPositions.size() > STUCK_HISTORY) {
      lastPositions.poll();
    }

    // Detect staying in the same position
    if (Math.abs(mario.j - lastX) < 0.05) {
      samePosCounter++;
    } else {
      samePosCounter = 0;
    }
    lastX = mario.j;

    if (samePosCounter > 8) {
      escapeLeft = true;
      jumpPhase = true;
      samePosCounter = 0;
      escapeTicks = 10;
    }
  }

  private boolean isHoleAhead(int[][] map, int row, int col) {
    for (int d = 1; d <= HOLE_LOOKAHEAD; d++) {
      int c = col + d;
      if (c >= map[0].length) break;
      if (!hasGroundBelow(map, row, c)) return true;
    }
    return false;
  }

  private boolean isHoleBehind(int[][] map, int row, int col) {
    for (int d = 1; d <= HOLE_LOOKAHEAD; d++) {
      int c = col - d;
      if (c < 0) break;
      if (!hasGroundBelow(map, row, c)) return true;
    }
    return false;
  }

  private boolean hasGroundBelow(int[][] map, int row, int col) {
    int r = row + 1;
    return (r < map.length && map[r][col] != MarioGame.EMPTY);
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