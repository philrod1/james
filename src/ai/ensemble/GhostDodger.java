package ai.ensemble;

import java.awt.Point;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ai.common.simulator.SimGame;
import emulator.machine.Snapshot;
import ai.common.Game;
import ai.common.Maze;
import ai.common.MOVE;

public class GhostDodger implements Voice {
	
	private final SimGame sim;
	private final int maxPaths = 8;
	private final int maxDepth = 8;
	
	public GhostDodger(Game game, int pacPause) {
		sim = new SimGame(game);
		sim.setPacPause(pacPause);
	}

	@Override
	public double[] getPreferences(Game game, List<MOVE> moves) {
		
		if(game.pacman.isEnergised()) {
			return new double[]{1,1,1,1};
		}
		
		long stop = System.currentTimeMillis() + 10;
		double[] prefs = new double[4];
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
				prefs[move.ordinal()] += sim.rolloutSafe(target, maxDepth, maze);
			}
		}
		for (MOVE move : moves) {
			prefs[move.ordinal()] /= maxPaths;
		}
		return prefs;
	}
}