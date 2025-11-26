import java.util.ArrayList;
import java.util.Random;
import game.mario.Direction;
import game.mario.MarioGame;
import game.mario.MarioPlayer;
import game.mario.utils.MarioState;

import static game.mario.MarioGame.PIPE;
import static game.mario.MarioGame.WALL;

/**
 * Feljavított Mario Agent.
 *
 * Prioritások:
 * 1. Fal, cső vagy lyuk → ugrás
 * 2. Különleges "ajándék fázis" → állapotgép alapú mozgás
 * 3. Ajándék doboz vizsgálata (magasan vagy egyedül) → ugrás
 * 4. Alapértelmezett mozgás: jobbra
 */
public class Agent extends MarioPlayer {
  int INF = Integer.MAX_VALUE;
  int NINF = Integer.MIN_VALUE;

  Direction jump = new Direction(MarioGame.UP);
  Direction goLeft = new Direction(MarioGame.LEFT);
  Direction goRight = new Direction(MarioGame.RIGHT);
  Direction dir;
  boolean menj5xBalra;
  int balraMenetelek = INF;

  ArrayList<Integer> lyukak = new ArrayList<>();


  private boolean falMiattUgras = false;
  private boolean lepkedes = false;

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

  public Agent(int color, Random random, MarioState state){
    super(color, random, state);
  }


  @Override
  public Direction getDirection(long remainingTime) {
    if (balraMenetelek <= 5){
      ++balraMenetelek;
      if (balraMenetelek == 6){
        balraMenetelek = INF;
      }
      return goLeft;
    }

    int[][] map = state.map;
    int height = map.length;
    int width = map[0].length;

    int marioX = -1, marioY = -1;

    // Mario pozíció megkeresése
    for(int y = 0; y < MarioGame.H; y++){
      for(int x = 0; x < MarioGame.W; x++){
        if(map[y][x] == MarioGame.MARIO){
          marioX = x;
          marioY = y;
          break;
        }
      }
      if(marioX != -1) break;
    }

    // Biztonsági ellenőrzés
    if (marioX == -1) {
      return null;
    }


    double rowD = state.mario.i;
    double colD = state.mario.j;

    // -- sor és oszlop változókba elmentve
    int row = (int) Math.round(rowD);
    int col = (int) Math.round(colD);

    // --- Környezeti blokkok változókba elmentve---
    //leftek
    int left = getSafeBlock(row, col - 1);
    int leftBelow = getSafeBlock(row + 1, col - 1);
    int leftAbove = getSafeBlock(row - 1, col - 1);

    //rightok
    int right = getSafeBlock(row, col + 1);
    int rightBelow = getSafeBlock(row + 1, col + 1);
    int rightAbove = getSafeBlock(row - 1, col + 1);

    //le
    int below = getSafeBlock(row + 1, col);

    //fel
    int above = getSafeBlock(row - 1, col);

    // 2-re levok
    int farLeft = getSafeBlock(row, col - 2);
    int farRight = getSafeBlock(row, col + 2);
    int farBelow = getSafeBlock(row + 2, col);
    int farAbove = getSafeBlock(row - 2, col);

    // 3-ra levok
    int furthestLeft = getSafeBlock(row, col - 3);
    int furthestRight = getSafeBlock(row,  col + 3);
    int furthestBelow = getSafeBlock(row + 3, col);
    int furthestAbove = getSafeBlock(row - 3, col);

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



    //-----------------------------------------------------------//
    // 1. Fal miatt ugrás – ha ugrással értünk a tetejére
    //-----------------------------------------------------------//
    if(falMiattUgras){
      falMiattUgras = false;
      lepkedes = true;
      balraMenetelek = 1;

      if (above == PIPE || above == WALL || farAbove == PIPE || farAbove == WALL || furthestAbove == PIPE || furthestAbove == WALL) {
        return goLeft;
      } else return jump;
    }

    //-----------------------------------------------------------//
    // 2. Cső akadály
    //-----------------------------------------------------------//
    if(marioX + 1 < width && map[marioY][marioX + 1] == PIPE){
      Direction a = jump;
      state.apply(a);
      return a;
    }

    //-----------------------------------------------------------//
    // 3. Lyuk detektálása (ha előtte nincs talaj)
    //-----------------------------------------------------------//
    //----------------------------------------------------------------------
    //Akadály (Lyuk)
    boolean holeAhead = rightBelow == MarioGame.EMPTY && below == MarioGame.EMPTY;

    if (holeAhead) return jump;

    //-----------------------------------------------------------//
    // 4. Fal előtte
    //-----------------------------------------------------------//

    boolean onGround = below != MarioGame.EMPTY;

    if (onGround && map[marioY][marioX + 1] == WALL) {
      falMiattUgras = true;
      return jump;
    }

    //-----------------------------------------------------------//
    // 5. Ajándék fázis állapotgép – finomított logika
    //-----------------------------------------------------------//
    if(ajandek == 1){
      ajandek = 2;
      Direction a = new Direction(MarioGame.RIGHT);
      state.apply(a);
      return a;
    }
    if(ajandek >= 8 && ajandek < 15){
      ajandek++;
      Direction a = new Direction(MarioGame.UP);
      state.apply(a);
      return a;
    }
    if(ajandek >= 15 && ajandek < 17){
      ajandek++;
      Direction a = new Direction(MarioGame.RIGHT);
      state.apply(a);
      return a;
    }
    if(ajandek == 17){
      ajandek = 0;
      Direction a = new Direction(MarioGame.UP);
      state.apply(a);
      return a;
    }
    if(ajandek > 1 && ajandek < 8){
      ajandek++;
      Direction a = new Direction(MarioGame.LEFT);
      state.apply(a);
      return a;
    }

    //-----------------------------------------------------------//
    // 6. Ajándék vizsgálata a Mario felett
    //-----------------------------------------------------------//

    // dupla ajándék egymás felett
    if (marioY - 7 >= 0 && marioY - 3 >= 0 && marioX + 1 < width &&
            map[marioY - 3][marioX + 1] == MarioGame.SURPRISE &&
            map[marioY - 7][marioX + 1] == MarioGame.SURPRISE)
    {
      ajandek = 1;
      Direction a = new Direction(MarioGame.UP);
      state.apply(a);
      return a;
    }

    // egyszerű, egy darab ajándék
    if (marioY - 3 >= 0 && marioX + 1 < width &&
            map[marioY - 3][marioX + 1] == MarioGame.SURPRISE)
    {
      Direction a = new Direction(MarioGame.UP);
      state.apply(a);
      return a;
    }

    //-----------------------------------------------------------//
    // 7. Lepkedés kezelés (megállás jobbra lépés után)
    //-----------------------------------------------------------//
    if(lepkedes){
      lepkedes = false;
      return new Direction(MarioGame.RIGHT); // vagy semmi extra
    }

    //-----------------------------------------------------------//
    // 8. Alapértelmezett viselkedés – jobbra haladás
    //-----------------------------------------------------------//
    Direction a = new Direction(MarioGame.RIGHT);
    lepkedes = true;
    state.apply(a);
    return a;

  }

  private Direction doABaseMovement() {
    // --- nyomi változóim ---
    int leftetEldontoBound = 5;
    int konkretLeftHatar = 1;

    int threshold = random.nextInt(leftetEldontoBound);
    if (threshold < konkretLeftHatar) {
      return goLeft;
    } else {
      int felVagyJobbra = random.nextInt(2);
      if (felVagyJobbra < 1) {
        return jump;
      } else {
        return goRight;
      }
    }
  }



  private Direction doALeftOrRightMovement(){
    int threshold = random.nextInt(2);
    if (threshold < 1)
      return goLeft;
    else return goRight;
  }



  boolean isLowest(int i) {
    return i == MarioGame.H - 1;
  }



  private int getSafeBlock(int row, int col) {
    if (row < 0 || col < 0) return MarioGame.EMPTY;
    if (row >= state.map.length) return MarioGame.EMPTY;
    if (col >= state.map[row].length) return MarioGame.EMPTY;
    return state.map[row][col];
  }
}





class Agent2 extends MarioPlayer {
  Direction jump = new Direction(MarioGame.UP);
  Direction goLeft = new Direction(MarioGame.LEFT);
  Direction goRight = new Direction(MarioGame.RIGHT);
  Direction dir;

  ArrayList<Integer> lyukak = new ArrayList<>();
  private boolean valtottUgrasJobbra = false;

  public Agent2(int color, Random random, MarioState marioState) {
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
    if (right == MarioGame.WALL || right == PIPE) {
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




  private int getSafeBlock(int row, int col) {
    if (row < 0 || col < 0) return MarioGame.EMPTY;
    if (row >= state.map.length) return MarioGame.EMPTY;
    if (col >= state.map[row].length) return MarioGame.EMPTY;
    return state.map[row][col];
  }
}