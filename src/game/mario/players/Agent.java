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
    return new Direction(MarioGame.LEFT);
  }

  public static void main(String[] args) {

  }
}