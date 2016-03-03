package ai.random;

import java.util.List;
import java.util.Random;

import ai.AbstractAI;
import ai.common.Game;
import ai.common.MOVE;
import ai.common.Game.PacMan;

public class RandomAI extends AbstractAI {

	private final PacMan pacman;
	private final Random rng = new Random();
	private boolean inJunction = false;
	private MOVE move = MOVE.LEFT;

	public RandomAI (Game game) {
		this.game = game;
		pacman = game.pacman;
	}

	@Override
	protected MOVE play() {
		List<MOVE> moves = pacman.getAvailableMoves();
		if(inJunction) {
			inJunction = moves.size() > 2;
		} else {
			inJunction = moves.size() >2;
			moves.remove(move.opposite());
			move = moves.get(rng .nextInt(moves.size()));
		}
		return move;
	}

	@Override
	protected void reset() {
		move = MOVE.LEFT;
	}
}
