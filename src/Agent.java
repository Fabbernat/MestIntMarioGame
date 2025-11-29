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

  Double closestBlock(MarioState var1) {
    double var2 = 1000.0F;
    Object var4 = null;

    try {
      Point var5 = new Point(var1.mario.j, var1.mario.i);

      for (int var6 = (int) var1.mario.i; var6 > (int) var1.mario.i - 4; --var6) {
        for (int var7 = (int) var1.mario.j; var7 < (int) var1.mario.j + 4; ++var7) {
          if (var1.map[var6][var7] == 3) {
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

  ArrayList<Direction> converter(String var1) {
    ArrayList var2 = new ArrayList();
    byte var3 = 4;
    if (var1.contains("S")) {
      var3 = 2;
    }

    if (var1.contains("V")) {
      var3 = 1;
    }

    if (var1.contains("R")) {
      for (int var4 = 0; var4 < var3; ++var4) {
        var2.add(new Direction(0));
      }
    }

    if (var1.contains("L")) {
      for (int var5 = 0; var5 < var3; ++var5) {
        var2.add(new Direction(2));
      }
    }

    if (var1.contains("U") || var1.equals("UP")) {
      for (int var6 = 0; var6 < var3; ++var6) {
        var2.add(new Direction(1));
      }
    }

    if (var1.contains("N") || var1.isEmpty()) {
      var2.add(null);
    }

    return var2;
  }

  List<Direction> aStar(MarioState var1) {
    PriorityQueue var2 = new PriorityQueue();
    HashMap var3 = new HashMap();
    Node var4 = new Node(var1.distance, 100.0F, (Node) null, var1, (String) null);
    var2.add(var4);

    while (!var2.isEmpty()) {
      ++this.depth;
      Node var5 = (Node) var2.poll();
      if (this.isGoal(var5) || this.depth > this.maxDepth) {
        this.depth = 0.0F;
        return this.path(var5);
      }

      double var6 = (double) 101.0F - var5.state.mario.j;

      for (String var11 : this.Actions) {
        MarioState var12 = new MarioState(var5.state);
        double var13 = 1.0F;
        var13 += Math.min(1.0F, (double) 1.0F / this.closestHole(var12));

        try {
          int[][] var15 = new int[3][3];

          for (int var16 = (int) var12.mario.i - 1; var16 > (int) var12.mario.i + 1; ++var16) {
            for (int var17 = (int) var12.mario.j - 1; var17 > (int) var12.mario.j + 1; ++var17) {
              var15[var16][var17] = var12.map[var16][var17];
            }
          }

          if (var15[0][1] == 1 && var15[0][2] == 1 || var15[0][1] == 2 && var15[0][2] == 2) {
            ++var13;
          }

          if (var12.mario.i >= (double) 12.0F) {
            if (var15[2][1] == 0 || var15[2][2] == 0 || var15[2][0] == 0) {
              ++var13;
            }

            if (var15[2][1] != 0 && var15[2][2] != 0) {
              var13 -= 2.0F;
            }
          }
        } catch (Exception var18) {
        }

        try {
          for (Direction var25 : this.converter(var11)) {
            if (!var12.apply(var25)) {
              var13 += 100.0F;
            }
          }
        } catch (Exception var19) {
        }

        try {
          int[][] var23 = new int[3][3];

          for (int var26 = (int) var12.mario.i - 1; var26 > (int) var12.mario.i + 1; ++var26) {
            for (int var28 = (int) var12.mario.j - 1; var28 > (int) var12.mario.j + 1; ++var28) {
              var23[var26][var28] = var12.map[var26][var28];
            }
          }

          if (var12.mario.i >= (double) 11.0F) {
            if (var23[2][1] == 0 || var23[2][2] == 0) {
              var13 += 10.0F;
            }

            if (var23[2][1] != 0 || var23[2][2] != 0 || var23[2][0] != 0) {
              var13 -= 10.0F;
            }
          }
        } catch (Exception ignored) {
        }

        if (var6 >= var12.distance) {
          var13 += 3.0F;
        }

        if (var12.score < (double) 1000.0F) {
          var13 += 3.0F;
        } else if (var12.score < (double) 2000.0F) {
          ++var13;
        }

        Node var24 = new Node(var5.g + var13, this.heurisztika(var12), var5, var12, var11);
        String var27 = var12.mario.i + ":" + var12.mario.j + ":" + var12.isInAir;
        if (!var3.containsKey(var27) || var24.f < (Double) var3.get(var27)) {
          var3.put(var27, var24.f);
          var2.add(var24);
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
