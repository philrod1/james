package ai.ensemble;

import java.awt.Point;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import ai.AbstractAI;
import ai.common.Emulator;
import ai.common.Game;
import ai.common.Maze;
import ai.common.MOVE;
import ai.common.Game.STATE;
import ai.common.simulator.SimGame;
import emulator.games.Pacman;
import emulator.machine.Snapshot;

public class EnsembleAI extends AbstractAI {
	
	public final static Emulator emulator = new Emulator(new Pacman());
//	private final SimGame sim;
	
	private final Voice pillMuncher;
	private final Voice ghostDodger;
	private final Voice fruitMuncher;
	private final Voice ghostMuncher;
	
	private MOVE lastMove = MOVE.LEFT;
	private Point target = null;
	
	private double[] weights = new double[]{
			1.0,    // Ghost Dodger
            0.01,   // Pill Muncher
            0.1,    // Fruit Muncher
            1.0     // Ghost Muncher
	};

	private final Random rng = new Random();

	private char ghostCounter;

	public EnsembleAI (Game game) {
//		sim = new SimGame(game);
		this.game = game;
		pillMuncher = new PillMuncher();
		ghostDodger = new GhostDodger(game);
		fruitMuncher = new FruitMuncher();
		ghostMuncher = new GhostMuncher(game);
	}

	protected MOVE play() {
		if(game.getState() == STATE.GET_READY) {
			target = new Point(14,24);
		}
		if(game.pacman.isAlive()) {
			boolean chomp = false;
			Point p = game.pacman.getTilePosition();
			while (target == null) {
				target = game.pacman.getTilePosition();
				p = target;
			}
			Snapshot snap = game.getSnapshot();
			char ghostCounter2 = snap.RAM[0x4dd0];
			if(ghostCounter2 != ghostCounter) {
				ghostCounter = ghostCounter2;
				chomp = true;
			}
			Maze maze = game.getMaze();
			List<MOVE> safe = new LinkedList<>();
			for(MOVE move : game.pacman.getAvailableMoves()) {
				Point t = maze.getNextCornerOrJunction(p, move);
				if(emulator.advanceToTargetSimple(move, t, snap)) {
					safe.add(move);
				}
			}
			if((p.equals(target) || chomp) && safe.size() > 0) {
				double[] combinedPreferences = combine( 
						ghostDodger.getPreferences(game, safe),
						pillMuncher.getPreferences(game, safe), 
						fruitMuncher.getPreferences(game, safe),
						ghostMuncher.getPreferences(game, safe));
				lastMove = bestMove(combinedPreferences, game.pacman.getAvailableMoves());
				if(game.pacman.isEnergised()) {
					target = game.getMaze().getNextCornerOrJunction(p, lastMove);
				} else {
					target = game.pacman.getNextTilePosition(lastMove);
				}
			}
			MOVE move = game.getMaze().getMoveTowards(p, target);
			emulator.syncToSnapshot(game.getSnapshot());
//			sim.syncToSnapshot(game.getSnapshot());
			MOVE move2 = emulator.oneStepCheck(move, game);
			if(move2 != move) {
				target = null;
				lastMove = move2;
				return move2;
			}
			lastMove = move;
			return move;
		} else {
			target = game.pacman.getTilePosition();
		}
		return null;
	}

	private double[] combine(double[]... prefs) {
		double[] combined = new double[4];
		for(int i = 0 ; i < 4 ; i++) {
			combined[i] = 0;
			for(int j = 1 ; j < prefs.length ; j++) {
				combined[i] += weights[j] * prefs[j][i];
			}
			combined[i] *= (weights[0] * prefs[0][i]);
		}
		return combined;
	}

	private MOVE bestMove(double[] values, List<MOVE> availableMoves) {
		boolean[] available = new boolean[4];
		for(MOVE move : availableMoves) {
			available[move.ordinal()] = true;
		}
		List<MOVE> bestMoves = new LinkedList<>();
		double bestValue = -1;
		for(int i = 0 ; i < 4 ; i++) {
			if(available[i]) {
				if(values[i] > bestValue) {
					bestValue = values[i];
					bestMoves.clear();
					bestMoves.add(MOVE.values()[i]);
				} else if (values[i] == bestValue) {
					bestMoves.add(MOVE.values()[i]);
				}
			}
		}
		if(bestMoves.size()==0) {
			System.out.println("No moves");
			return lastMove.opposite();
		}
		return bestMoves.get(rng .nextInt(bestMoves.size()));
	}
	
	protected void reset() {
		target = new Point(14,24);
		game.makeMove(MOVE.LEFT);
	}

}
