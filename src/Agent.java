import game.mario.Direction;
import game.mario.MarioGame;
import game.mario.MarioPlayer;
import game.mario.utils.MarioState;

import java.util.*;

import static game.mario.MarioGame.H;

/**
 * Feljavított Mario Agent.
 * <p>
 * Prioritások:
 * 1. Fal, cső vagy lyuk → ugrás
 * 2. Különleges "ajándék fázis" → állapotgép alapú mozgás
 * 3. Ajándék doboz vizsgálata (magasan vagy egyedül) → ugrás
 * 4. Alapértelmezett mozgás: jobbra
 */
public class Agent extends MarioPlayer {
  int INF = Integer.MAX_VALUE / 2;
  int NINF = Integer.MIN_VALUE / 2;


  Direction jump = new Direction(MarioGame.UP);
  Direction goLeft = new Direction(MarioGame.LEFT);
  Direction goRight = new Direction(MarioGame.RIGHT);
  Direction dir;


  double dx; // oldalra speed
  double dy; // felfele speed

  ArrayList<Integer> lyukak = new ArrayList<>();

  enum Status {
    JUMPING(0),
    STATIC(1),
    FALLING(2),
    LEFT_UP(3),
    LEFT(4),
    LEFT_DOWN(5),
    RIGHT_UP(6),
    RIGHT(7),
    RIGHT_DOWN(8);

    public final int code;

    Status(int code) {
      this.code = code;
    }
  }

  Status status;
  private boolean valtottUgrasJobbra = false;
  private boolean inHoleJump = false;
  private int safeStepsRight = 0;


  /**
   * Ajándék fázis állapotgép:
   * 0 = nincs folyamatban
   * 1 = első ugrás után jobbra lépés
   * 2–7 = visszalépések balra
   * 8–14 = ugrások
   * 15–16 = jobbra mozgás
   * 17 = végső ugrás → reset
   */
  private int ajandek = 0;

  public Agent(int color, Random random, MarioState marioState) {
    super(color, random, marioState);
  }

  @Override
  public Direction getDirection(long remainingTime) {

    int[][] map = state.map;
    int height = map.length;
    int width = map[0].length;


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

    // 2-re levok
    int farLeft = getSafeBlock(row, col - 2);
    int farRight = getSafeBlock(row, col + 2);
    int farBelow = getSafeBlock(row + 2, col);
    int farAbove = getSafeBlock(row - 2, col);

    // 3-ra levok
    int furthestLeft = getSafeBlock(row, col - 3);
    int furthestRight = getSafeBlock(row, col + 3);
    int furthestBelow = getSafeBlock(row + 3, col);
    int furthestAbove = getSafeBlock(row - 3, col);

    // 2-1 és 2-2 delták
    int eggyelLejjebbEsKettovelJobbrabb = getSafeBlock(row + 1, col + 2);
    int kettovelLejjebbEsKettovelJobbrabb = getSafeBlock(row + 2, col + 2);
    int kettovelLejjebbEsEggyelJobbrabb = getSafeBlock(row + 2, col + 1);

    int kettovelFeljebbEsEggyelJobbrabb = getSafeBlock(row - 2, col + 1);

    int[] veszelyesPoziciok = new int[]{
            rightBelow,
            below,
            farBelow,
//            eggyelLejjebbEsKettovelJobbrabb,
            kettovelLejjebbEsEggyelJobbrabb,
//            kettovelLejjebbEsKettovelJobbrabb
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

    // és utána mit kezdek a lyukakkal?
    // ha van előttem lyuk, ugrok.
    for (int i = 0; i < lyukak.size(); i++) {
      int elem = lyukak.get(i);

      if (elem == 1 && veszelyesPoziciok[i] == H - 1) {
        status = Status.JUMPING;
        return jump;
      }
    }

    // Ha folyamatban van a lyuk feletti ugrás, akkor tartsuk fent
    if (inHoleJump) {

      // Amíg alatta a legalsó szint EMPTY, addig folytasd az ugrást
      int under = getSafeBlock(row - 1, col);
      if (under == MarioGame.EMPTY && isLowest(under)) {
        return jump;  // továbbra is felfelé
      }

      // Ha már nem a lyuk ovan -> kezdődhet a biztonságos jobbra lépkedés
      if (safeStepsRight == 0) {
        safeStepsRight = 3;  // ennyi lépés jobbra biztonságból
      }
    }

// Ha épp biztonsági jobbra-lépést kell tenni
    if (safeStepsRight > 0) {
      safeStepsRight--;
      if (safeStepsRight == 0) {
        inHoleJump = false; // vége a lyuk-szekvenciának
      }
      return goRight;
    }


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
    // LYUK LOGIKA
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

      // Itt indul az "ugrunk egy lyuk felett" állapot
      inHoleJump = true;

      // Minden egyéb helyzetben felváltva UP és RIGHT (jobbrafelé ugrás)
      if (!valtottUgrasJobbra) {
        valtottUgrasJobbra = true;
        state.apply(jump);
        return jump;
      } else {
        valtottUgrasJobbra = false;
        state.apply(goRight);
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
    int leftetEldontoBound = 11;
    int konkretLeftHatar = 2;

    int threshold = random.nextInt(leftetEldontoBound);
    if (threshold < konkretLeftHatar) {
      return goLeft;
    } else {
      int felVagyJobbra = random.nextInt(3);
      if (felVagyJobbra < 1) {
        return jump;
      } else {
        return goRight;
      }
    }
  }

  boolean isLowest(int i) {
    return i == H - 1;
  }

  ;


  private int getSafeBlock(int row, int col) {
    if (row < 0 || col < 0) return MarioGame.EMPTY;
    if (row >= state.map.length) return MarioGame.EMPTY;
    if (col >= state.map[row].length) return MarioGame.EMPTY;
    return state.map[row][col];
  }
}