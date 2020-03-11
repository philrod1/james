package ai.ensemble;

import java.awt.Point;
import java.util.List;

import ai.common.simulator.SimGame;
import emulator.machine.Snapshot;
import ai.common.Game;
import ai.common.Maze;
import ai.common.MOVE;

public class GhostMuncher implements Voice {

	private final int maxPaths = 20;
	private final int maxDepth = 6;
	private final SimGame sim;

	public GhostMuncher(Game game) {
		sim = new SimGame(game);
	}

	@Override
	public double[] getPreferences(Game game, List<MOVE> moves) {
		long stop = System.currentTimeMillis() + 60;
		double[] prefs = new double[4];
		if(game.pacman.isEnergised()) {
			Snapshot snap = game.getSnapshot();
			Maze maze = game.getMaze();
			Point p = game.pacman.getTilePosition();
			while(System.currentTimeMillis() < stop) {
				for (MOVE move : moves) {
					if(prefs[move.ordinal()] > maxPaths-1) {
						for (MOVE m : moves) {
							prefs[m.ordinal()] /= maxPaths;
						}
						return prefs;
					}
					sim.sync(snap.RAM);
					Point target = game.getMaze().getNextCornerOrJunction(p, move);
					prefs[move.ordinal()] += sim.rolloutGhostMunch(target, maxDepth, maze);
				}
			}
			for (MOVE move : moves) {
				prefs[move.ordinal()] /= maxPaths;
			}
		}
		return prefs;
	}

}
