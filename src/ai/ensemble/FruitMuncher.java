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
		
//		double minDist = 10000000;
		BONUS bonus = game.getCurrentBonus();
		if(bonus != null) {
//			for(MOVE move : moves) {
//				minDist = Math.min(minDist, game.pacman.distanceToNearestFruit(move));
//			}
//			if(minDist < 3) {
				for(MOVE move : moves) {
					prefs[move.ordinal()] = 10.0 / (game.pacman.distanceToNearestFruit(move) + rng.nextDouble() * epsilon);
				}
//			} else {
//				EnsembleAI.emulator.syncToSnapshot(game.getSnapshot());
//				for(int i = 0 ; i < minDist * 3 ; i++) {
//					EnsembleAI.emulator.setMove(moves.get(rng.nextInt(moves.size())));
//					EnsembleAI.emulator.step();
//				}
//				Point b = EnsembleAI.emulator.getBonusTilePosition();
//				for(MOVE move : moves) {
//					try {
//						prefs[move.ordinal()] = 10.0 /
//								(game.pacman.distancesToPoint(b)[move.ordinal()]
//										+ rng.nextDouble() * epsilon);
//					} catch (NullPointerException e) {
//					}
//				}
//			}
		}
		return prefs;
	}

}
