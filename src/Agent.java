import game.mario.Direction;
import game.mario.MarioGame;
import game.mario.MarioPlayer;
import game.mario.utils.MarioState;
import java.util.*;

public class Agent extends MarioPlayer {
  /**
   *
   * Egy Agenst keszít.
   * @param color szine a jatekosnak
   * @param random random szam
   * @param state a jatek egy adott allasa
   */

  public Agent(int color, Random random, MarioState state) {
    super(color, random, state);

  }

  /**
   *
   * A lehetseges akciok
   * lepes merteke:
   * V: very small 1 lepes
   * S: Small 2 lepes
   * R: Jobbra
   * L: Balra
   * UP: Ugras
   * "": allas
   */
  String[] Actions = new String[]{
          "VR",
          "VL",
          "SR",
          "SL",
          "R",
          "L",
          "U",
          ""


  };


  class Node implements Comparable<Node> {
    public Node parent;
    public MarioState state;
    public String direction;
    public double g;
    public double h;
    public double f;


    /**
     *A graf egy adodd csucspontja
     * @param parent Kitol jutottunk ide
     * @param state Milyen allapotban vagyunk
     * @param direction Milyen iranybol jottunk
     * @param g,h,f g: eddigi utkoltseg, h: hatralevo tavolsag, f: osszkoltseg
     *
     */
    public Node(double g, double h,Node parent, MarioState state, String direction) {
      this.parent = parent;
      this.state = state;
      this.direction = direction;
      this.g = g;
      this.h = h;
      this.f = h+g;

    }
    /**
     *2 Node osszehasonlitasa f szerint
     */
    @Override
    public int compareTo(Node n) {
      return Double.compare(this.f, n.f);
    }
  }

  float mainGoal = 2000;
  float minDistToReach = 40;
  /**
   *Tavolsag szamitashoz helper osztaly
   */
  class Point {
    double x, y;
    public Point(double x, double y) {
      this.x = x;
      this.y = y;
    }
  }

  double heur(MarioState s) {
    /**
     * @param dist a vegtol valo tavolsagunk
     * @param scroteDist a cel scroetol valo tavolsag
     * @param supriseDist a legkozelebbi suprise tavolsaga
     */
    double dist = Math.max(0, 50 - s.mario.j); // nem negatív
    double scoreDist = Math.max(0, mainGoal - s.score); // nem negatív
    double surpriseDist = closestBlock(s, MarioGame.SURPRISE);

    if (surpriseDist > 100) surpriseDist = 100; // maximalizáljuk
    return surpriseDist*0.6 + dist*0.5 + scoreDist*0.5;
  }


  /**
   * 12-es melysegben nezzuk hol vannak empty-k,
   */
  double closestGodor(MarioState s) {
    double dist = 10000;
    Point best = null;
    try {
      Point m =  new  Point(s.mario.j,s.mario.i);
      for(int i = 12; i <= 12; i++) {
        for(int j =  (int)s.mario.j-2; j <(int)s.mario.j+6; j++) {
          if(s.map[i][j] == MarioGame.EMPTY) {
            Point b = new  Point(j,i);
            double x2 = (b.x-m.x)*(b.x-m.x);
            double y2 = (b.y-m.y)*(b.y-m.y);
            double newDist = Math.abs(x2+y2);
            if(newDist<dist) {
              dist = newDist;
              best = b;
            }
          }
        }
      }}
    catch (Exception e) {}

    return dist;
  }
  /**
   * Mariotol jobbra 4-el megkeressuk a legkozelebbi keresendo blokk helyet
   */
  Double closestBlock(MarioState s, int type) {
    double dist = 1000;
    Point best = null;
    try {
      Point m =  new  Point(s.mario.j,s.mario.i);
      for(int i = (int)s.mario.i; i >(int)s.mario.i-4; i--) {
        for(int j =  (int)s.mario.j; j< (int)s.mario.j+4; j++) {
          if(s.map[i][j] == type) {
            Point b = new  Point(j,i);

            double x2 = (b.x-m.x)*(b.x-m.x);
            double y2 = (b.y-m.y)*(b.y-m.y);
            double newDist = Math.abs(x2+y2);
            if(newDist<dist) {
              dist = newDist;
              best = b;
            }
          }
        }
      }}
    catch (Exception e) {}

    return dist;
  }
  /**
   * Atalakitjuk a megkapott irany Strignet Directionne es egy listaban visszaadjuk.
   * @return List<Direction>
   */
  List<Direction> converter(String dir) {
    List<Direction> dirs = new ArrayList<>();
    int db = 4;
    if(dir.contains("S")) {db = 2;}
    if(dir.contains("V")) {db = 1;}
    if(dir.contains("R")) {
      for(int i = 0; i < db; i++) {
        dirs.add(new Direction(MarioGame.RIGHT));
      }
    }
    if(dir.contains("L")) {
      for(int i = 0; i <db; i++) {
        dirs.add(new Direction(MarioGame.LEFT));
      }
    }
    if(dir.contains("U") || dir.equals("UP")) {
      for(int i = 0; i <db; i++) {
        dirs.add(new Direction(MarioGame.UP));
      }


    }
    if(dir.contains("N") || dir.isEmpty()) {
      dirs.add(null);

    }

    return  dirs;
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
     * @param open Az elerheto utak, mindig a legjobb f erteku van elol
     * @param bestF elso erteke a kulcs ami alapjan tudjuk hogy erre jartunk-e mar, erteke a node f  erteke, mindig a jelenlegi legjobbakat tesszuk bele.
     * @param first a kezdeti elso csomopontunk
     */
    PriorityQueue<Node> open = new PriorityQueue<>();
    Map<String, Double> bestF = new HashMap<>();
    Node first = new Node(start.distance,MarioGame.W,null,start,null);
    open.add(first);
    /**
     * Az elerheto utakhoz hozzáadjuk az első kiindulo csucsot
     * Kivesszuk az openbol
     * Megnezzuk hogy elertuk e a celunkat, vagy a melyseget, ha igen visszaadjuk az ide vezeto utat
     * Kiterjesztjuk a jelenlegi csucspontunkat, az Action osszes utjara leuttatjuk az adott allapotot
     */

    while (!open.isEmpty()) {
      depth++;
      Node current = open.poll();
      if(isGoal(current) || depth >maxDepth) {
        depth = 0;
        return path(current);
      }
      double prevDist = MarioGame.W+1-current.state.mario.j;
      for(String dir : Actions) {
        MarioState s = new MarioState(current.state);
        double tileCost = 1;

        /**
         * @param tileCose extra utikoltseg
         **/


        tileCost += Math.min(1, 1/closestGodor(s)); // max 1 extra


        try {
          int kernel[][] = new int[3][3];
          /**
           * @param kernel, mario a kozepe
           **/
          for(int i = (int)s.mario.i-1; i > (int)s.mario.i+1; i++) {
            for(int j = (int)s.mario.j-1; j > (int)s.mario.j+1; j++) {
              kernel[i][j] = s.map[i][j];
            }
          }

/**
 * Ha egy fal vagy cso van felettunk
 **/
          if(kernel[0][1] == MarioGame.WALL  &&kernel[0][2] == MarioGame.WALL || kernel[0][1] == MarioGame.PIPE  &&kernel[0][2] == MarioGame.PIPE) {
            tileCost+=1;
          }
/**
 * ha 11-es y szinten vagyunk es alattunk Empty van;
 **/

          if(s.mario.i >= 12) {
            if(kernel[2][1] == MarioGame.EMPTY  || kernel[2][2]== MarioGame.EMPTY  || kernel[2][0]== MarioGame.EMPTY) {
              tileCost+=1;
            }


            if(kernel[2][1] != MarioGame.EMPTY  && kernel[2][2]!= MarioGame.EMPTY) {
              tileCost-=2;
            }

          }

        }catch (Exception e) {}

        try {
          for(Direction d : converter(dir)) {
            /**
             * iranyok hozzaadasa, ha false azt azt jelenti hogy ott meghaltunk
             **/
            if(s.apply(d) == false) {tileCost +=100;} ;
          }

        } catch (Exception e) {};

        try {
          int kernel[][] = new int[3][3];
          for(int i = (int)s.mario.i-1; i > (int)s.mario.i+1; i++) {
            for(int j = (int)s.mario.j-1; j > (int)s.mario.j+1; j++) {
              kernel[i][j] = s.map[i][j];
            }
          }

          /**
           * Ha mozgasok utan ismet empty van alattunk
           **/

          if(s.mario.i >= 11) {
            if(kernel[2][1] == MarioGame.EMPTY  || kernel[2][2]== MarioGame.EMPTY) {
              tileCost+=10;
            }
            if(kernel[2][1] != MarioGame.EMPTY  || kernel[2][2]!= MarioGame.EMPTY  || kernel[2][0]!= MarioGame.EMPTY) {
              tileCost-=10;
            }

          }

          //    if(s.isInAir){tileCost+=0.8;}
        }catch (Exception e) {}

        /***
         * Ha elötte egy 5 magas fal van akkor ha nincs elég nagy sebessége ne közelítse meg
         *
         * **/


        if(prevDist >= s.distance) {tileCost+=3;}
        /**
         * Ha nem erte el a kello pontszámot, +koltseg
         **/

        if(s.score <1000) {tileCost +=3;}
        else if(s.score <2000) {tileCost += 1;}
/**
 * Eltaroljuk a csucspontot, letrehozzuk egy kulcsot, fontos hogy ne legyen tul egyedi. ez alapjan hivatkozunk ra
 **/
        Node n = new Node(current.g+tileCost,heur(s),current,s,dir);

        String key = s.mario.i + ":" + s.mario.j + ":" + s.isInAir;
        if (!bestF.containsKey(key) || n.f < bestF.get(key)) {
          bestF.put(key, n.f);
          open.add(n);
        }

      }

    }
    return null;
  }
  /**
   * A megadott csomoponttol visszafele a parenteken keresztul eljut a root csomopontig, majd az egeszet megfordítja
   **/
  List<Direction> path(Node node) {
    List<Direction> path = new ArrayList<>();
    Node current = node;
    while (current != null) {
      if (current.direction != null) {
        for(Direction d : converter(current.direction)) {
          path.add(d);
        }
      }
      current = current.parent;
    }
    Collections.reverse(path);
    return path;
  }
  /**
   * Ha elertük a kívant celt igazzal terunk vissza, a celt mindig kicsivel megemeljük.
   **/
  boolean isGoal(Node node) {
    if(node.state.score >= mainGoal) { mainGoal+=500; return true;}
    if(node.state.distance >= minDistToReach) {minDistToReach+=20;return true;}
    return false;
  }
  /**
   * @param lepesek az aStar altal visszaadott lepeseket ebben taroljuk és mindig csak az elso lepest vesszük ki minden getDirection hivaskor. Ha ures ne csinaljuk semmit.
   *  a state apply-t try-ba kell beletenni mert kepes index out of bounds-ot dobni.
   **/
  List<Direction> lepesek = new ArrayList<>();

  @Override
  public Direction getDirection(long remainingTime) {

    if(lepesek == null || lepesek.isEmpty()) {

      lepesek = aStar(state);
    }
    if(lepesek == null || lepesek.isEmpty()) {
      return null;
    }
    Direction d = lepesek.remove(0);
    try {    state.apply(d);} catch (Exception e) {

    }

    return d;


  }
}