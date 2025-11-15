import java.util.Random;
import game.mario.Direction;
import game.mario.MarioGame;
import game.mario.MarioPlayer;
import game.mario.utils.MarioState;

public class Agent extends MarioPlayer {

  private boolean akadalyMiattUgras = false;
  private int lepesekJobbraAkadalyMiatt = 0;
  private boolean falMiattUgras = false;
  private boolean lepkedes = false;  //ha jobbra ment true, ha allt false
  private int ajandek = 0; //ha ajándékot talál akkor elkezd gondókodni hogy van-e felette is, meg megáll miután ugrott, az ajandek novelese a fazisoktol fugg

  public Agent(int color, Random random, MarioState state){
    super(color, random, state);
    System.out.println("Agent constructor called");
  }



  @Override
  public Direction getDirection(long remainingTime) {
    int[][] map = state.map;
    int height = map.length;
    int width = map[0].length;


    //Mario kibaszott pozíciójának változói
    int marioX = -1, marioY = -1;

    //Megkeressük a báttyát
    for(int y = 0; y < 13; y++){
      for(int x = 0; x < 100; x++){
        if(map[y][x] == MarioGame.MARIO){
          marioX = x;
          marioY = y;
          break;
        }
      }
      if(marioX != -1) break;
    }
    //---------------------------------------------


    //----------------------------------------------------------------------
    //Ha fal miatt ugrottunk akkor rajta maradunk a fal tetején
    if(falMiattUgras){
      falMiattUgras = false;
      lepkedes = true;
      Direction action = new Direction(MarioGame.RIGHT);
      state.apply(action);
      return action;
    }
    //Akadály (Pipe)
    if(marioX + 1 < width && map[marioY][marioX + 1] == MarioGame.PIPE){
      //akadalyMiattUgras = true;
      Direction action = new Direction(MarioGame.UP);
      state.apply(action);
      return action;
    }
    //----------------------------------------------------------------------
    //Akadály (Lyuk)
    if(marioY > 10){
      if (marioY + 1 < height && marioX + 1 < width &&
              map[marioY + 1][marioX + 1] == MarioGame.EMPTY &&
              map[marioY][marioX + 1] == MarioGame.EMPTY) {
        //akadalyMiattUgras = true;
        Direction action = new Direction(MarioGame.UP);
        state.apply(action);
        return action;
      }
    }
    //----------------------------------------------------------------------
    //Fal előtte
    if(marioX + 1 < width && map[marioY][marioX + 1] == MarioGame.WALL){
      falMiattUgras = true;
      Direction action = new Direction(MarioGame.UP);
      state.apply(action);
      return action;
    }
    //-----------------------------------------------------------------------
    //Ha van ajándék, vagy fal 4 blokkal felette akkor felugrik rá és kiüti a másikat
    if(ajandek == 1){
      ajandek = 2;
      Direction action = new Direction(MarioGame.RIGHT);
      state.apply(action);
      return action;
    }
    if(ajandek >= 8 && ajandek < 15){
      ajandek += 1;
      Direction action = new Direction(MarioGame.UP);
      state.apply(action);
      return action;
    }
    if(ajandek >= 15 && ajandek < 17){
      ajandek += 1;
      Direction action = new Direction(MarioGame.RIGHT);
      state.apply(action);
      return action;
    }
    if(ajandek == 17){
      ajandek = 0;
      Direction action = new Direction(MarioGame.UP);
      state.apply(action);
      return action;
    }
    if(ajandek < 8 && ajandek > 1){
      ajandek += 1;
      Direction action = new Direction(MarioGame.LEFT);
      state.apply(action);
      return action;
    }
    //-----------------------------------------------------------------------

    //Ajándék egy másikkal felette
    if(map[marioY - 3][marioX + 1] == MarioGame.SURPRISE && map[marioY - 7][marioX + 1] == MarioGame.SURPRISE){
      Direction action = new Direction(MarioGame.UP);
      ajandek = 1;
      state.apply(action);
      return action;
    }
    //-----------------------------------------------------------------------
    //Ajándék magában
    if(map[marioY - 3][marioX + 1] == MarioGame.SURPRISE){
      Direction action = new Direction(MarioGame.UP);
      state.apply(action);
      return action;
    }
    //-----------------------------------------------------------------------

    if(lepkedes){
      lepkedes = false;
      state.apply(null);
      return null;
    }

    Direction action = new Direction(MarioGame.RIGHT);
    lepkedes = true;
    state.apply(action);
    return action;
  }

}



