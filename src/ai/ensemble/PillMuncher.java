package ai.ensemble;

import java.awt.Point;
import java.util.List;
import java.util.Random;

import ai.common.Game;
import ai.common.MOVE;

public class PillMuncher implements Voice {
	
	private final Random rng = new Random();
	private final double epsilon = 1e-3;

	@Override
	public double[] getPreferences(Game game, List<MOVE> moves) {
		double[] results = new double[4];
		for(MOVE move : moves) {
				Point p = game.pacman.getTilePosition();
				Point t = game.getMaze().getNextJunction(p, move);
				if((game.pacman.isEnergised() || !game.allGhostsOut())
						&& game.getMaze().pathContainsPowerPill(p, t, game)) {
					results[move.ordinal()] = 0.00001;
				} else {
					int distance = game.pacman.distanceToNearestPillPath(move) + 1;
					results[move.ordinal()] = 1.0 / distance + rng.nextDouble() * epsilon;
				}
		}
		return results;
	}

}