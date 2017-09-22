package ai.mcts;

import java.awt.Point;
import java.util.List;
import java.util.Random;

import ai.AbstractAI;
import ai.common.simulator.SimGame;
import ai.common.simulator.data.SimData;
import ai.ensemble.FruitMuncher;
import emulator.games.Pacman;
import emulator.machine.Snapshot;
import ai.common.Game;
import ai.common.MOVE;
import ai.common.Game.PacMan;
import ai.common.Game.STATE;
import ai.common.Emulator;

public class MCTSPlayer extends AbstractAI {

 	private final Emulator emu = new Emulator(new Pacman());
	private PacMan pacman;
	private Point target;
	private final Random rng = new Random();
	private TreeNodeSim root;
	private MOVE move = MOVE.LEFT;
	private SimGame sim;
	
	private final FruitMuncher fm;

	public MCTSPlayer(Game game) {
		this.game = game;
		pacman = game.pacman;
		sim = new SimGame(game);
		fm = new FruitMuncher();
	}

	protected synchronized MOVE play() {
		Point pacmanTile = pacman.getTilePosition();
		try {

			Snapshot snap = game.getSnapshot();
			emu.syncToSnapshot(snap);
			int result = emu.advanceToTarget(target, game.getMaze());
			if (result >= 0) {
				emu.step();
				if (!emu.isPacmanAlive()) {
					result = -1;
				}
			}
			if (result < 0) {
				List<MOVE> moves = pacman.getAvailableMoves();
				for(MOVE m : moves) {
					emu.syncToSnapshot(snap);
					Point next = game.getMaze().getNextTile(pacmanTile, m);
					if(next == null) {
						System.out.println("NULL next :-( " + m);
					} else if(emu.advanceToTarget(next, game.getMaze()) >= 0) {
						move = m;
						target = next;
						return move;
					}
				}
				move = move.opposite();
				return move;
			}

			snap = emu.getSnapshot();
			sim.syncToSnapshot(snap);
			SimData data = new SimData(snap);

			root = new TreeNodeSim(null, 1, sim, data);
			root.maxDepth = 0;
			TreeNodeSim.score = emu.getScore();
			List<MOVE> availableMoves = game.getMaze().getAvailableMoves(target);
			root.expand(availableMoves);

			while (!pacman.getTilePosition().equals(target)) {
				root.selectAction(game.getMaze(), data);
				if(game.getState() != STATE.PLAYING) {
					reset();
					return move;
				}
			}
			double[] fruitPrefs = fm.getPreferences(game, availableMoves);
			double bestValue = Double.NEGATIVE_INFINITY;
			MOVE bestMove = null;
			for (TreeNodeSim child : root.children) {
				double fpm = fruitPrefs[child.move.ordinal()] * 100;
//				fpm *= fpm * 0;
				
				
				double value = (child.reward / child.nVisits) * (1 + fpm);
				System.out.println(child.move + " : " + value + " (" + child.nVisits + ") " + child.getMaxDepth());
				if (value > bestValue) {
					bestValue = value;
					bestMove = child.move;
				}
			}
			root = null;
			if (bestMove == null) {
				List<MOVE> moves = game.pacman.getAvailableMoves();
				move = moves.get(rng.nextInt(moves.size()));
				return move;
			}
			target = game.getMaze().getNextCornerOrJunction(target, bestMove);
			System.out.println("Going " + bestMove + " to " + target);
			System.out.println("----------------------------------------------");
			move = bestMove;
		} catch (NullPointerException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return move;
	}

	protected void reset() {
		target = new Point(14,24);
		move = MOVE.LEFT;
		game.makeMove(move);
	}


}
