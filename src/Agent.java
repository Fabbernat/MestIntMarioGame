import game.mario.Direction;
import game.mario.MarioGame;
import game.mario.MarioPlayer;
import game.mario.utils.Mario;
import game.mario.utils.MarioState;

import java.util.*;

public class Agent extends MarioPlayer {
  ArrayList<Integer> lyukak = new ArrayList<>();

  public Agent(int color, Random random, MarioState marioState) {
    super(color, random, marioState);
  }

  @Override
  public Direction getDirection(long remainingTime) {

    double rowD = state.mario.i;
    double colD = state.mario.j;

    // -- sor és oszlop változókba elmentve
    int row = (int) Math.round(rowD);
    int col = (int) Math.round(colD);

    // --- Környezeti blokkok változókba elmentve---
    //leftek
    int left = getSafeBlock(row, col - 1);
    int leftBelow = getSafeBlock(row - 1, col - 1);
    int leftAbove =  getSafeBlock(row + 1, col - 1);
    //rightok
    int right = getSafeBlock(row, col + 1);
    int rightBelow = getSafeBlock(row - 1, col + 1);
    int rightAbove = getSafeBlock(row + 1, col + 1);
    //le
    int below = getSafeBlock(row - 1, col);
    //fel
    int above = getSafeBlock(row + 1, col);
    //tavolabbiak
    int farLeft = getSafeBlock(row, col - 2);
    int farRight = getSafeBlock(row, col + 2);
    int farBelow = getSafeBlock(row - 2, col);
    int farAbove = getSafeBlock(row + 2, col);

    //amiket nehezebb elnevezni
    int EggyelLejjebbEsKettovelJobbrabb = getSafeBlock(row - 1, col + 2);
    int KettovelLejjebbEsKettovelJobbrabb = getSafeBlock(row - 2, col + 2);
    int KettovelLejjebbEsEggyelJobbrabb = getSafeBlock(row - 2, col + 1);

    int[] veszelyesPoziciok = new int[]{
            rightBelow, below, EggyelLejjebbEsKettovelJobbrabb, KettovelLejjebbEsKettovelJobbrabb, KettovelLejjebbEsEggyelJobbrabb, farBelow
    };
    // =======================
    // 1) Lyuk
    // =======================
    for (int aktVeszPoz : veszelyesPoziciok) {
      if (aktVeszPoz == MarioGame.EMPTY) {
        Direction jump = new Direction(MarioGame.UP);
        lyukak.add(aktVeszPoz);
        if (state.isInAir){

        }
        state.apply(jump);
        return jump;
      }
    }

    // =======================
    // 2) Fal / Pipe
    // =======================
    if (right == MarioGame.WALL || right == MarioGame.PIPE) {
      Direction jump = new Direction(MarioGame.UP);
      state.apply(jump);
      return jump;
    }

    // =======================
    // 11) Alap mozgás véletlen alapján: általában fel, néha jobbra és csak ritkán balra
    // =======================

    // --- nyomi változóim ---
    int leftetEldontoBound = 5;
    int konkretLeftHatar = 1;

    int threshold = new Random().nextInt(leftetEldontoBound);
    if (threshold < konkretLeftHatar) {
      Direction goLeft =  new Direction(MarioGame.LEFT);
      state.apply(goLeft);
      return goLeft;
    } else {
      int felVagyJobbra = random.nextInt(3);
      if (felVagyJobbra < 2) {
        Direction jump = new Direction(MarioGame.UP);
        state.apply(jump);
        return jump;
      } else {
        Direction goRight = new Direction(MarioGame.RIGHT);
        state.apply(goRight);
        return goRight;
      }
    }
  }

  private int getSafeBlock(int row, int col) {
    if (row < 0 || col < 0) return MarioGame.EMPTY;
    if (row >= state.map.length) return MarioGame.EMPTY;
    if (col >= state.map[row].length) return MarioGame.EMPTY;
    return state.map[row][col];
  }
}