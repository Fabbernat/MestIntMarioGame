import game.mario.Direction;
import game.mario.MarioPlayer;
import game.mario.utils.MarioState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import static game.mario.MarioGame.EMPTY;

public class Agent extends MarioPlayer {
  String[] Actions = new String[]{"VR", "VL", "SR", "SL", "R", "L", "U", ""};
  float mainGoal = 2000.0F;
  float minDistToReach = 40.0F;
  double maxDepth = 1000.0F;
  double depth = 0.0F;
  List<Direction> lepesek = new ArrayList<>();

  public Agent(int color, Random random, MarioState state) {
    super(color, random, state);
  }

  public Direction getDirection(long remainingTime) {

    // Ha még nincs adat a lépésekben,
    if (this.lepesek == null || this.lepesek.isEmpty()) {
      // akkor meghívjuk az A* algoritmust, amely kiszámolja az összes lépést.
      this.lepesek = this.aStar(this.state);
    }

    /** Különben töröljük és alkalmazzuk az 1. lépést a "lepesek"-ből.
     * "removeFirst"-tel nem működik.
     */
    if (this.lepesek != null && !this.lepesek.isEmpty()) {
      Direction aktualisLepes = this.lepesek.remove(0);

      // Enelkul rosszul működik
      try {
        this.state.apply(aktualisLepes);
      } catch (Exception ignored) {
      }

      return aktualisLepes;
    } else {
      return null;
    }
  }

  double heurisztika(MarioState state) {
    double aktOszlopIndex = Math.max(0.0F, (double) 50.0F - state.mario.j); // Nem lehet negatív!
    double hajlam = Math.max(0.0F, (double) this.mainGoal - state.score); // Nem lehet negatív!
    double legkozelebbiLyuk = this.closestBlock(state);
    if (legkozelebbiLyuk > (double) 100.0F) {
      legkozelebbiLyuk = 100.0F;
    }

    return legkozelebbiLyuk * 0.6 + aktOszlopIndex * (double) 0.5F + hajlam * (double) 0.5F;
  }

  double closestHole(MarioState state) {
    double hatarertek = 10000.0F;
    Object legjobb = null;

    try {
      Point aktMarioPozicio = new Point(state.mario.j, state.mario.i);

      // Lyuk kereső algoritmus a pálya alján (csak ott lehet)
      for (int i = (int) state.mario.j - 2; i < (int) state.mario.j + 6; ++i) {
        if (state.map[12][i] == EMPTY) {
          Point pont = new Point(i, 12);
          double var9 = (pont.x - aktMarioPozicio.x) * (pont.x - aktMarioPozicio.x);
          double var11 = (pont.y - aktMarioPozicio.y) * (pont.y - aktMarioPozicio.y);
          double var13 = Math.abs(var9 + var11);
          if (var13 < hatarertek) {
            hatarertek = var13;
          }
        }
      }
    } catch (Exception ignored) {
    }

    return hatarertek;
  }

  Double closestBlock(MarioState state) {
    double var2 = 1000.0F;
    Object var4 = null;

    try {
      Point var5 = new Point(state.mario.j, state.mario.i);

      for (int var6 = (int) state.mario.i; var6 > (int) state.mario.i - 4; --var6) {
        for (int var7 = (int) state.mario.j; var7 < (int) state.mario.j + 4; ++var7) {
          if (state.map[var6][var7] == 3) {
            Point var8 = new Point(var7, var6);
            double var9 = (var8.x - var5.x) * (var8.x - var5.x);
            double var11 = (var8.y - var5.y) * (var8.y - var5.y);
            double var13 = Math.abs(var9 + var11);
            if (var13 < var2) {
              var2 = var13;
            }
          }
        }
      }
    } catch (Exception ignored) {
    }

    return var2;
  }

  ArrayList<Direction> converter(String s) {
    ArrayList<Direction> list = new ArrayList<>();
    byte b = 4;
    if (s.contains("S")) {
      b = 2;
    }

    if (s.contains("V")) {
      b = 1;
    }

    if (s.contains("R")) {
      for (int var4 = 0; var4 < b; ++var4) {
        list.add(new Direction(0));
      }
    }

    if (s.contains("L")) {
      for (int var5 = 0; var5 < b; ++var5) {
        list.add(new Direction(2));
      }
    }

    if (s.contains("U") || s.equals("UP")) {
      for (int var6 = 0; var6 < b; ++var6) {
        list.add(new Direction(1));
      }
    }

    if (s.contains("N") || s.isEmpty()) {
      list.add(null);
    }

    return list;
  }

  List<Direction> aStar(MarioState state) {
    PriorityQueue queue = new PriorityQueue();
    HashMap map = new HashMap();
    Node addedNode = new Node(state.distance, 100.0F, null, state, null);
    queue.add(addedNode);

    while (!queue.isEmpty()) {
      ++this.depth;
      Node polledNode = (Node) queue.poll();
      if (this.isGoal(polledNode) || this.depth > this.maxDepth) {
        this.depth = 0.0F;
        return this.path(polledNode);
      }

      double szam = (double) 101.0F - polledNode.state.mario.j;

      for (String akcio : this.Actions) {
        MarioState aktState = new MarioState(polledNode.state);
        double f = 1.0F;
        f += Math.min(1.0F, (double) 1.0F / this.closestHole(aktState));

        try {
          int[][] gameMap = new int[3][3];

          for (int var16 = (int) aktState.mario.i - 1; var16 > (int) aktState.mario.i + 1; ++var16) {
            for (int var17 = (int) aktState.mario.j - 1; var17 > (int) aktState.mario.j + 1; ++var17) {
              gameMap[var16][var17] = aktState.map[var16][var17];
            }
          }

          if (gameMap[0][1] == 1 && gameMap[0][2] == 1 || gameMap[0][1] == 2 && gameMap[0][2] == 2) {
            ++f;
          }

          if (aktState.mario.i >= (double) 12.0F) {
            if (gameMap[2][1] == 0 || gameMap[2][2] == 0 || gameMap[2][0] == 0) {
              ++f;
            }

            if (gameMap[2][1] != 0 && gameMap[2][2] != 0) {
              f -= 2.0F;
            }
          }
        } catch (Exception var18) {
        }

        try {
          for (Direction aktDirection : this.converter(akcio)) {
            if (!aktState.apply(aktDirection)) {
              f += 100.0F;
            }
          }
        } catch (Exception ignored) {
        }

        try {
          int[][] minimap = new int[3][3];

          for (int i = (int) aktState.mario.i - 1; i > (int) aktState.mario.i + 1; ++i) {
            for (int j = (int) aktState.mario.j - 1; j > (int) aktState.mario.j + 1; ++j) {
              minimap[i][j] = aktState.map[i][j];
            }
          }

          if (aktState.mario.i >= (double) 11.0F) {
            if (minimap[2][1] == 0 || minimap[2][2] == 0) {
              f += 10.0F;
            }

            if (minimap[2][1] != 0 || minimap[2][2] != 0 || minimap[2][0] != 0) {
              f -= 10.0F;
            }
          }
        } catch (Exception ignored) {
        }

        if (szam >= aktState.distance) {
          f += 3.0F;
        }

        if (aktState.score < (double) 1000.0F) {
          f += 3.0F;
        } else if (aktState.score < (double) 2000.0F) {
          ++f;
        }

        Node var24 = new Node(polledNode.g + f, this.heurisztika(aktState), polledNode, aktState, akcio);
        String var27 = aktState.mario.i + ":" + aktState.mario.j + ":" + aktState.isInAir;
        if (!map.containsKey(var27) || var24.f < (Double) map.get(var27)) {
          map.put(var27, var24.f);
          queue.add(var24);
        }
      }
    }

    return null;
  }

  ArrayList<Direction> path(Node var1) {
    ArrayList var2 = new ArrayList();

    for (Node var3 = var1; var3 != null; var3 = var3.parent) {
      if (var3.direction != null) {
        var2.addAll(this.converter(var3.direction));
      }
    }

    Collections.reverse(var2);
    return var2;
  }

  boolean isGoal(Node var1) {
    if (var1.state.score >= (double) this.mainGoal) {
      this.mainGoal += 500.0F;
      return true;
    } else if (var1.state.distance >= (double) this.minDistToReach) {
      this.minDistToReach += 20.0F;
      return true;
    } else {
      return false;
    }
  }

  static class Node implements Comparable<Node> {
    public Node parent;
    public MarioState state;
    public String direction;
    public double g;
    public double h;
    public double f;

    public Node(double var1, double var3, Node var5, MarioState var6, String var7) {
      this.parent = var5;
      this.state = var6;
      this.direction = var7;
      this.g = var1;
      this.h = var3;
      this.f = var3 + var1;
    }

    public int compareTo(Node var1) {
      return Double.compare(this.f, var1.f);
    }
  }

  static class Point {
    double x;
    double y;

    public Point(double var1, double var3) {
      this.x = var1;
      this.y = var3;
    }
  }
}
