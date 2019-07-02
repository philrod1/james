package ai.ensemble;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

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
	
	public final static Emulator emu = new Emulator(new Pacman());
	private final ExecutorService exec = Executors.newCachedThreadPool();
//	private final SimGame sim;
	
	private final Voice[] voices;
	
	private MOVE lastMove = MOVE.LEFT;
	private Point target = null;
	
	private double[] weights = new double[]{
			1.0,    // Ghost Dodger
			1.0,    // Ghost Dodger
            0.1,   // Pill Muncher
            0.5,    // Fruit Muncher
            1.0,     // Ghost Muncher
			1.0		// Ghost chaser
	};

	private final Random rng = new Random();

	private char ghostCounter;

	public EnsembleAI (Game game) {
//		sim = new SimGame(game);
		this.game = game;
		voices = new Voice[]{
				new GhostDodger(game, 1),
				new GhostDodger(game, 2),
				new PillMuncher(),
				new FruitMuncher(),
				new GhostMuncher(),
				new GhostChaser()
		};
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
			char ghostCounter2 = snap.RAM[0x0dd0];
			if(ghostCounter2 != ghostCounter) {
				ghostCounter = ghostCounter2;
				chomp = true;
			}
			Maze maze = game.getMaze();
			List<MOVE> safe = new LinkedList<>();
			for(MOVE move : game.pacman.getAvailableMoves()) {
				Point t = maze.getNextCornerOrJunction(p, move);
				if(emu.advanceToTargetSimple(move, t, snap)) {
					safe.add(move);
				}
			}
			if((p.equals(target) || chomp) && safe.size() > 0) {

				double[][] results = new double[voices.length][];
				List<Future<double[]>> futures = new ArrayList<>(voices.length);
				try {
					for (int i = 0 ; i < results.length ; i++) {
						Voice v = voices[i];
						futures.add(i, exec.submit(() -> v.getPreferences(game, safe)));
					}
					for (int i = 0 ; i < results.length ; i++) {
						results[i] = futures.get(i).get();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}


				double[] combinedPreferences = combine(results);
				lastMove = bestMove(combinedPreferences, game.pacman.getAvailableMoves());
				if(game.pacman.isEnergised()) {
					target = game.getMaze().getNextCornerOrJunction(p, lastMove);
				} else {
					target = game.pacman.getNextTilePosition(lastMove);
				}
			}
			MOVE move = game.getMaze().getMoveTowards(p, target);
			emu.syncToSnapshot(game.getSnapshot());
//			sim.syncToSnapshot(game.getSnapshot());
			MOVE move2 = emu.oneStepCheck(move, game);
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
			for(int j = 2 ; j < prefs.length ; j++) {
				combined[i] += weights[j] * prefs[j][i];
			}
			combined[i] *= (weights[0] * prefs[0][i]);
			combined[i] *= (weights[1] * prefs[1][i]);
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
