import game.mario.Direction;
import game.mario.MarioGame;
import game.mario.MarioPlayer;
import game.mario.utils.MarioState;

import java.util.*;

/** Agent osztaly, amely a jatekban Mario donteseit hozza meg.
 * A jatek celja eljutni a palya jobb oldalara, kerulve a lyukakat,
 * felveve az utba eso ermeket es meglepetes blokkokat.
 *
 * Az A* keresest alkalmazom egy lecsokkentett allapotteren belul annak erdekeben,
 * hogy Mariot biztonsagos es pontszerzo uton vezessem elore.
 *
 * A donteseket a getDirection() metodusban hozzom meg, amely a palya
 * aktualis allapotarol eltero heuriztikus ertekeles alapjan
 * kivalaszt egy akciot.
 *
 * A megvalositott algoritmus legfontosabb elemei:
 * - egy allapotot reprezentalo Csomo osztaly,
 * - heuriztikus tavolsagbecsles (heurisztika),
 * - segedfuggvenyek kozelben levo blokkok felderitesere,
 * - akciok string-alapu leirasa es iranyokkra bontasa.
 *
 * A kod nem vegez fajlmuveleteket, nem indit uj szalat, es nem ir
 * a kepernyore, megfelelve a feladat kiiras kovetelmenyeinek.
 */
public class Agent extends MarioPlayer {


  /**
   * Letrehozok egy Agent peldanyt, amely a Mario jatekban
   * az adott jatekoshez tartozo szinnel, a keretrendszer altal
   * szolgaltatott Random objektummal es a jatek egy adott
   * allapotaval.
   *
   * @param color  A jatekos szine (GUI-ban megjeleno identifier)
   * @param random A keretrendszer altal orokolt veletlenszam generator
   * @param state  A jatek aktualis allapota, amelybol az Agent kiindul
   */
  public Agent(int color, Random random, MarioState state) {
    super(color, random, state);

  }

  /**
   * A lehetseges akciokat leiro string-elemek.
   * Ezek kombinaciokat reprezentalnak (pl. "SR" = 2 lepest jobb fele).
   *
   * A karakterek jelentese:
   * V: nagyon kis lepes (1 lepest jelent)
   * S: kis lepes (2 lepes)
   * R: jobbra mozgasa
   * L: balra mozgasa
   * U: ugras
   * "": nincs akcio
   */
  String[] akciok = {
          "VR", "VL",
          "SR", "SL",
          "R", "L",
          "U", ""
  };


  /** Csomo osztaly, amelyet az A* kereso algoritmus hasznal a
   * palya allapotteren valo tervezeshez. Egy graf csucsat reprezentalja,
   * hivatkozassal a szulo csucsra, a mozgas iranyara es a koltseg ertekekre.
   */
  static class Csomo implements Comparable<Csomo> {
    public Csomo parent;
    public MarioState state;
    public String direction;
    public double g;
    public double h;
    public double f;


    /**
     *A graf egy adott csucspontja
     * @param parent Kitol jutottunk ide
     * @param state Milyen allapotban vagyunk
     * @param direction Milyen iranybol jottunk
     * @param g,h,f g: eddigi utkoltseg, h: hatralevo tavolsag, f: osszkoltseg
     *
     */
    public Csomo(double g, double h, Csomo parent, MarioState state, String direction) {
      this.parent = parent;
      this.state = state;
      this.direction = direction;
      this.g = g;
      this.h = h;
      this.f = h+g;

    }
    /**
     *2 Csomo osszehasonlitasa f szerint
     */
    @Override
    public int compareTo(Csomo n) {
      return Double.compare(this.f, n.f);
    }
  }

  float mainGoal = 2000;
  float minDistToReach = 40;
  /**
   *Tavolsag szamitashoz helper osztaly
   */
  static class Pont {
    double x, y;
    public Pont(double x, double y) {
      this.x = x;
      this.y = y;
    }
  }

  /**
   * Heurisztikus fuggveny, amely becslest ad arra,
   * milyen messze vagyok a celtol.
   * A heurisztika figyelembe veszi
   * - a palya vegetol valo tavolsagot,
   * - a jelenlegi pontszamot a kivant maximalis ertekhez kepest,
   * - a legkozelebbi meglepetesblokk (SURPRISE) tavolsagat,
   * - es normalizalast a tul nagy ertekek elkerulese erdekeben.
   * @param state a vizsgalt jatekallapot
   * @return egy nem negativ lebegopontos ertek, amely a becsult hatralevo koltseg
   */
  double heurisztika(MarioState state) {
    /**
     * @param tavolsag a palya vegetol valo tavolsagunk
     * @param tavolsagPontszam a cel pontszamtol valo tavolsag
     * @param ajandekTavolsag a legkozelebbi suprise tavolsaga
     */
    double tavolsag = Math.max(0, 50 - state.mario.j); // nem negativ
    double tavolsagPontszam = Math.max(0, mainGoal - state.score); // nem negativ
    double ajandekTavolsag = closestBlock(state);

    if (ajandekTavolsag > 100) ajandekTavolsag = 100; // maximalizaljuk
    return ajandekTavolsag *0.6 + tavolsag*0.5 + tavolsagPontszam*0.5;
  }


  /**
   * A legkozelebbi lyukhoz (EMPTY mezo az also sorokban) mert tavolsagot
   * becsli. Az agens elsorban azert hasznalja, hogy elkerulje az olyan
   * akciokat, amelyek kozvetlenul veszelyes teruletre vezetnenek.
   *
   * <p>A fuggveny egy szukitett vizsgalati savban keres a jatekos
   * elotti es koruli teruleten.</p>
   *
   * @param state A jatekallapot
   * @return A lyukhoz becsult tavolsag negyzete, vagy nagy ertek ha nincs lyuk
   */
  double legkozelebbiGodor(MarioState state) {
    double tavolsag = 10000;
    Pont legjobb = null;
    try {
      Pont mario = new Pont(state.mario.j, state.mario.i);
      for (int j = (int) state.mario.j - 2; j < (int) state.mario.j + 6; j++) {
        if (state.map[12][j] == MarioGame.EMPTY) {
          Pont b = new Pont(j, 12);
          double x2 = (b.x - mario.x) * (b.x - mario.x);
          double y2 = (b.y - mario.y) * (b.y - mario.y);
          double ujTavolsag = Math.abs(x2 + y2);
          if (ujTavolsag < tavolsag) {
            tavolsag = ujTavolsag;
            legjobb = b;
          }
        }
      }
    }
    catch (Exception ignored) {}

    return tavolsag;
  }


  /**
   * Megkeresi az adott tipusu elem (pl. SURPRISE, COIN) legkozelebbi Mario-tol jobbra eso elofordulasat.
   *
   * <p>A kereses vizszintesen elore tortenik, fuggoleges szinten
   * pedig a jatekos aktualis pozicioja korul vizsgalunk.</p>
   *
   * @param state A jatekallapot
   * @return A legkisebb talalt tavolsag, vagy nagy ertek ha nincs ilyen elem
   */
  Double closestBlock(MarioState state) {
    double dist = 1000;
    Pont legjobb = null;
    try {
      Pont mario = new Pont(state.mario.j, state.mario.i);
      for(int i = (int)state.mario.i; i >(int)state.mario.i-4; i--) {
        for(int j =  (int)state.mario.j; j< (int)state.mario.j+4; j++) {
          if(state.map[i][j] == MarioGame.SURPRISE) {
            Pont b = new Pont(j, i);

            double x2 = (b.x-mario.x)*(b.x-mario.x);
            double y2 = (b.y-mario.y)*(b.y-mario.y);
            double ujTavolsag = Math.abs(x2+y2);
            if(ujTavolsag<dist) {
              dist = ujTavolsag;
              legjobb = b;
            }
          }
        }
      }}
    catch (Exception ignored) {}

    return dist;
  }

  /**
   * Atalakitom a megkapott irany Strignet Directionne es egy listaban visszaadjuk.
   * @return List<Direction>
   */
  List<Direction> atalakito(String dir) {
    List<Direction> directions = new ArrayList<>();
    int db = 4;
    if(dir.contains("S")) {db = 2;}
    if(dir.contains("V")) {db = 1;}
    if(dir.contains("R")) {
      for(int i = 0; i < db; i++) {
        directions.add(new Direction(MarioGame.RIGHT));
      }
    }
    if(dir.contains("L")) {
      for(int i = 0; i <db; i++) {
        directions.add(new Direction(MarioGame.LEFT));
      }
    }
    if(dir.contains("U") || dir.equals("UP")) {
      for(int i = 0; i <db; i++) {
        directions.add(new Direction(MarioGame.UP));
      }


    }
    if(dir.contains("N") || dir.isEmpty()) {
      directions.add(null);

    }

    return  directions;
  }
  /**
   * @param maxDepth milyen sokaig menjen le az kereses
   * @param depth jelenlegi melyseg
   *
   */
  double maxDepth = 1000;
  double depth = 0;



  List<Direction> aStar(MarioState start) {
    /**
     * @param elerhetoUtak Az elerheto utak, mindig a legjobb f erteku van elol
     * @param legjobbak elso erteke a kulcs ami alapjan tudjuk hogy erre jartunk-e mar, erteke a csomo f  erteke, mindig a jelenlegi legjobbakat tesszuk bele.
     * @param elsoCsomopont a kezdeti elso csomopontunk
     */
    PriorityQueue<Csomo> elerhetoUtak = new PriorityQueue<>();
    Map<String, Double> legjobbak = new HashMap<>();
    Csomo elsoCsomopont = new Csomo(start.distance, MarioGame.W, null, start, null);
    elerhetoUtak.add(elsoCsomopont);
    /**
     * Az elerheto utakhoz hozzaadjuk az elso kiindulo csucsot
     * Kivesszuk az openbol
     * Megnezzuk hogy elertuk e a celunkat, vagy a melyseget, ha igen visszaadjuk az ide vezeto utat
     * Kiterjesztjuk a jelenlegi csucspontunkat, az `akciok` osszes utjara leuttatjuk az adott allapotot
     */

    while (!elerhetoUtak.isEmpty()) {
      depth++;
      Csomo jelenlegiCsomo = elerhetoUtak.poll();
      if(celE(jelenlegiCsomo) || depth >maxDepth) {
        depth = 0;
        return ut(jelenlegiCsomo);
      }
      double elozoTavolsag = MarioGame.W+1-jelenlegiCsomo.state.mario.j;
      for(String akcio : akciok) {
        MarioState state = new MarioState(jelenlegiCsomo.state);
        double tileKoltseg = 1;

        /**
         * @param tileKoltseg extra utikoltseg
         **/


        tileKoltseg += Math.min(1, 1/ legkozelebbiGodor(state)); // max 1 extra


        try {
          int[][] kornyezet = new int[3][3];
          /**
           * @param kornyezet, mario a kozepe
           **/
          for(int i = (int)state.mario.i-1; i > (int)state.mario.i+1; i++) {
            for(int j = (int)state.mario.j-1; j > (int)state.mario.j+1; j++) {
              kornyezet[i][j] = state.map[i][j];
            }
          }

/**
 * Ha egy fal vagy cso van felettunk
 **/
          if(kornyezet[0][1] == MarioGame.WALL  &&kornyezet[0][2] == MarioGame.WALL || kornyezet[0][1] == MarioGame.PIPE  &&kornyezet[0][2] == MarioGame.PIPE) {
            tileKoltseg+=1;
          }
/**
 * ha 11-es y szinten vagyunk es alattunk Empty van;
 **/

          if(state.mario.i >= 12) {
            if(kornyezet[2][1] == MarioGame.EMPTY  || kornyezet[2][2]== MarioGame.EMPTY  || kornyezet[2][0]== MarioGame.EMPTY) {
              tileKoltseg+=1;
            }


            if(kornyezet[2][1] != MarioGame.EMPTY  && kornyezet[2][2]!= MarioGame.EMPTY) {
              tileKoltseg-=2;
            }

          }

        }catch (Exception ignored) {}

        try {
          for(Direction direction : atalakito(akcio)) {
            /**
             * iranyok hozzaadasa, ha false azt azt jelenti hogy ott meghaltunk
             **/
            if(state.apply(direction) == false) {tileKoltseg +=100;} ;
          }

        } catch (Exception ignored) {};

        try {
          int[][] kornyezet = new int[3][3];
          for(int i = (int)state.mario.i-1; i > (int)state.mario.i+1; i++) {
            for(int j = (int)state.mario.j-1; j > (int)state.mario.j+1; j++) {
              kornyezet[i][j] = state.map[i][j];
            }
          }

          /**
           * Ha mozgasok utan ismet empty van alattunk
           **/

          if(state.mario.i >= 11) {
            if(kornyezet[2][1] == MarioGame.EMPTY  || kornyezet[2][2]== MarioGame.EMPTY) {
              tileKoltseg+=10;
            }
            if(kornyezet[2][1] != MarioGame.EMPTY  || kornyezet[2][2]!= MarioGame.EMPTY  || kornyezet[2][0]!= MarioGame.EMPTY) {
              tileKoltseg-=10;
            }

          }

          //    if(state.isInAir){tileKoltseg+=0.8;}
        }catch (Exception ignored) {}

        /***
         * Ha elotte egy 5 magas fal van akkor ha nincs eleg nagy sebessege ne kozelitse meg
         *
         * **/


        if(elozoTavolsag >= state.distance) {tileKoltseg+=3;}
        /**
         * Ha nem erte el a kello pontszamot, +koltseg
         **/

        if(state.score <1000) {tileKoltseg +=3;}
        else if(state.score <2000) {tileKoltseg += 1;}
/**
 * Eltaroljuk a csucspontot, letrehozzuk egy kulcsot, fontos hogy ne legyen tul egyedi. ez alapjan hivatkozunk ra
 **/
        Csomo csomo = new Csomo(jelenlegiCsomo.g + tileKoltseg, heurisztika(state), jelenlegiCsomo, state, akcio);

        String kulcs = state.mario.i + ":" + state.mario.j + ":" + state.isInAir;
        if (!legjobbak.containsKey(kulcs) || csomo.f < legjobbak.get(kulcs)) {
          legjobbak.put(kulcs, csomo.f);
          elerhetoUtak.add(csomo);
        }

      }

    }
    return null;
  }
  /**
   * A megadott csomoponttol visszafele a parenteken keresztul eljut a root csomopontig, majd az egeszet megforditja
   **/
  List<Direction> ut(Csomo csomo) {
    List<Direction> ut = new ArrayList<>();
    Csomo jelenlegiCsomo = csomo;
    while (jelenlegiCsomo != null) {
      if (jelenlegiCsomo.direction != null) {
        for(Direction direction : atalakito(jelenlegiCsomo.direction)) {
          ut.add(direction);
        }
      }
      jelenlegiCsomo = jelenlegiCsomo.parent;
    }
    Collections.reverse(ut);
    return ut;
  }
  /**
   * Ha elertuk a kivant celt igazzal terunk vissza, a celt mindig kicsivel megemeljük.
   **/
  boolean celE(Csomo csomo) {
    if(csomo.state.score >= mainGoal) { mainGoal+=500; return true;}
    if(csomo.state.distance >= minDistToReach) {minDistToReach+=20;return true;}
    return false;
  }

  List<Direction> lepesek = new ArrayList<>();

  /**
   * A jatek minden lepeseben meghivott fuggveny, amely kivalasztja
   * Mario kovetkezo iranyat. A dontes alapja:
   * - heurisztikus becsles a palya vegeig,
   * - potencialis veszelyek (lyukak, falak) felmerese,
   * - pontszerzesi lehetosegek felterkepezese,
   * - A* kereses egy rovid melysegu tervezesi terben.
   *
   * <p>A modszer vegul egy Direction objektumot ad vissza,
   * amelyet a keretrendszer kozvetlenul vegrehajt.</p>
   *
   * @param remainingTime A lepes meghozatalara rendelkezesre allo ido (ms)
   * @return A valasztott irany a Direction enum segitsegevel
   */
  @Override
  public Direction getDirection(long remainingTime) {

    if(lepesek == null || lepesek.isEmpty()) {

      lepesek = aStar(state);
    }
    if(lepesek == null || lepesek.isEmpty()) {
      return null;
    }
    Direction direction = lepesek.remove(0);
    try {    state.apply(direction);} catch (Exception ignored) {

    }

    return direction;


  }
}