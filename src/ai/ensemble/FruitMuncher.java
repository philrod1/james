package ai.ensemble;

import java.awt.Point;
import java.util.List;
import java.util.Random;

import ai.common.Game;
import ai.common.MOVE;
import ai.common.Game.BONUS;

public class FruitMuncher implements Voice {
	
	private final Random rng = new Random();
	private final double epsilon = 1e-6;

	@Override
	public double[] getPreferences(Game game, List<MOVE> moves) {
		double[] prefs = new double[4];
		BONUS bonus = game.getCurrentBonus();
		if(bonus != null) {
			for(MOVE move : moves) {
				prefs[move.ordinal()] = 10.0 / (game.pacman.distanceToNearestFruit(move) + rng.nextDouble() * epsilon);
			}
		}
		return prefs;
	}

}
