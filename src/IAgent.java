import game.mario.Direction;

public interface IAgent {
  Direction getDirection(long remainingTime);
}
