import game.mario.Direction;
import game.mario.MarioGame;
import game.mario.MarioPlayer;
import game.mario.utils.Mario;
import game.mario.utils.MarioState;

import java.util.*;

public class Agent extends MarioPlayer {
  Direction jump = new Direction(MarioGame.UP);
  Direction goLeft = new Direction(MarioGame.LEFT);
  Direction goRight = new Direction(MarioGame.RIGHT);
  Direction dir;

  ArrayList<Integer> lyukak = new ArrayList<>();
  private boolean valtottUgrasJobbra = false;

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
    int leftAbove = getSafeBlock(row + 1, col - 1);
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
    int eggyelLejjebbEsKettovelJobbrabb = getSafeBlock(row - 1, col + 2);
    int kettovelLejjebbEsKettovelJobbrabb = getSafeBlock(row - 2, col + 2);
    int kettovelLejjebbEsEggyelJobbrabb = getSafeBlock(row - 2, col + 1);

    int[] veszelyesPoziciok = new int[]{
            rightBelow, below, eggyelLejjebbEsKettovelJobbrabb, kettovelLejjebbEsKettovelJobbrabb, kettovelLejjebbEsEggyelJobbrabb, farBelow
    };
    // =======================
    // 1/A) Lyuk hozzáadása
    // =======================
    lyukak.clear();
    for (int aktVeszPoz : veszelyesPoziciok) {
      if (aktVeszPoz == MarioGame.EMPTY) {
        lyukak.add(1);  // csak egy flag-szerű érték, hogy van lyuk
      }
    }

    // =======================
    // 1/B – Lyuk törlése
    // =======================
    // Nem kell semmit törölni egyenként, mert fent clear-eltük.


    /**
     * Ha van lyuk a lyukakban, azaz van veszelyesPozicio a közelben, akkor
     * Ha a below lyuk, akkor
     * - ha a rightBelow is lyuk, akkor balra megyünk
     * - ha a rightBelow nem lyuk, akkor jobbra megyünk.
     *
     * Ha a farBelow lyuk, akkor minden esetben jobbra tartunk. Ez azért lehetséges, mert a játékban garantált, hogy csak legfeljebb 2 nagyságú lyukak lesznek, és a lyuk végén nem lesz fal.
     *
     * - Ha a rightBelow, vagy a kettovelLejjebbEsEggyelJobbrabb nem lyuk, akkor jobbra megyünk.  (legyen egy // Ha a rightBelow, vagy a kettovelLejjebbEsEggyelJobbrabb nem lyuk, akkor jobbra megyünk. komment ott!).
     *
     * - Minden egyéb esetben felváltva UP és RIGHT műveleteket adunk ki ( jobbrafelé ugrunk).
     */

    // =======================
    // LYUK LOGIKA A KOMMENT ALAPJÁN
    // =======================
    // Ha van bármilyen lyuk (veszélyesPozíció) a listában:
    if (!lyukak.isEmpty()) {

      // Ha a below lyuk
      if (below == MarioGame.EMPTY && isLowest(below)) {

        // - ha a rightBelow is lyuk → menjünk BALRA
        if (rightBelow == MarioGame.EMPTY && isLowest(rightBelow)) {
          return goLeft;
        }

        // - ha a rightBelow nem lyuk → doABaseMovement
        else {
          dir = doABaseMovement();
          return dir;
        }
      }

      // Ha a farBelow lyuk → doABaseMovement
      if (farBelow == MarioGame.EMPTY) {
        dir = doABaseMovement();
        return dir;
      }

      // Ha a rightBelow vagy a kettovelLejjebbEsEggyelJobbrabb nem lyuk → jobbra
      if (rightBelow != MarioGame.EMPTY || kettovelLejjebbEsEggyelJobbrabb != MarioGame.EMPTY) {
        return goRight;
      }

      // Minden egyéb helyzetben felváltva UP és RIGHT (jobbrafelé ugrás)
      // Ezt úgy oldjuk meg, hogy minden második hívásnál mást csinál:
      if (random.nextBoolean()) {
        return jump;
      } else {
        return goRight;
      }
    }


    // =======================
    // Fal / Pipe
    // =======================
    if (right == MarioGame.WALL || right == MarioGame.PIPE) {
      return jump;
    }

    // =======================
    // Alap mozgás véletlen alapján: általában fel, néha jobbra és csak ritkán balra
    // =======================

    return doABaseMovement();
  }

  private Direction doABaseMovement() {
    // --- nyomi változóim ---
    int leftetEldontoBound = 5;
    int konkretLeftHatar = 1;

    int threshold = random.nextInt(leftetEldontoBound);
    if (threshold < konkretLeftHatar) {
      return goLeft;
    } else {
      int felVagyJobbra = random.nextInt(3);
      if (felVagyJobbra < 2) {
        return jump;
      } else {
        return goRight;
      }
    }
  }

  boolean isLowest(int i) {
    return i == MarioGame.H - 1;
  }

  ;


  private int getSafeBlock(int row, int col) {
    if (row < 0 || col < 0) return MarioGame.EMPTY;
    if (row >= state.map.length) return MarioGame.EMPTY;
    if (col >= state.map[row].length) return MarioGame.EMPTY;
    return state.map[row][col];
  }
}