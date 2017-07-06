package ai.mcts;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import ai.common.MOVE;
import ai.common.Maze;
import ai.common.simulator.SimGame;
import ai.common.simulator.data.SimData;

public class TreeNodeSim {

	private final int depth;
	public int maxDepth = 0;
	private final SimGame sim;
	public TreeNodeSim[] children;
	public int nVisits;
	public final MOVE move;
	private Random r = new Random();
	private double epsilon = 1e-6;
	private boolean pacmanAlive;
	public double reward;
	public static int score;
	private final SimData simData;

	public TreeNodeSim(MOVE move, int depth, SimGame sim, SimData simData) {
		this.sim = sim;
		this.depth = depth;
		this.move = move;
		reward = 0;
		this.simData = simData;
	}

	public void selectAction(Maze maze, SimData simData) {

		reward = 100;
		List<TreeNodeSim> visited = new LinkedList<TreeNodeSim>();
		TreeNodeSim current = this;
		sim.syncToDataPoint(simData);

		current.pacmanAlive = true;

		while (!current.isLeaf() && current.pacmanAlive && sim.getPillCount() > 0) {
			current = current.select();
			current.pacmanAlive = sim.advanceToNextTile(current.move, maze);
			visited.add(current);
		}

		if (sim.getPillCount() ==  0) {
			reward += 1000;
		} else
		if (!current.pacmanAlive) {
			reward /= 10;
			if (Double.isNaN(reward)) {
				reward = 0.00001;
			}
		} else {
			current.expand();
			if(current.children == null) {
				return;
			}
			reward += Math.max(1,rollOut() / current.depth);
		}
		for (TreeNodeSim node : visited) {
			node.updateStats(reward);
			maxDepth = Math.max(maxDepth, node.depth);
		}
		this.updateStats(reward);
	}

	private void expand() {
		List<MOVE> moves = pacmanAvailableMoves(sim);
		moves.remove(sim.simPacman.getCurrentMove().opposite());
		if (moves == null) return;
		children = new TreeNodeSim[moves.size()];
		if (moves.size() == 0) {
			System.out.println("Error expanding " + moves.size() + " children.");
			return;
		}
		int i = 0;
		for (MOVE move : moves) {
			TreeNodeSim node = new TreeNodeSim(move, (int)depth+1, sim, simData);
			children[i++] = node;
		}
	}

	public void expand(List<MOVE> moves) {
		children = new TreeNodeSim[moves.size()];
		if (moves.size() == 0) {
			System.out.println("Error expanding " + moves.size() + " children.");
			return;
		}
		int i = 0;
		maxDepth = (int) Math.max(maxDepth, depth);
		for (MOVE move : moves) {
			TreeNodeSim node = new TreeNodeSim(move, (int)depth+1, sim, simData);
			children[i++] = node;
		}
	}

	private TreeNodeSim select() {
		TreeNodeSim selected = children[0];
		double bestValue = Double.NEGATIVE_INFINITY;
		if (children.length == 0) {
			System.out.println("NO children to select.");
		}
		for (TreeNodeSim c : children) {
			double uctValue = (c.reward + epsilon) / ((c.nVisits * c.nVisits) + epsilon)
					+ Math.sqrt(Math.log((nVisits * nVisits) + 1) / ((c.nVisits * c.nVisits) + epsilon))
					+ r.nextDouble() * epsilon;
			if (uctValue > bestValue) {
				selected = c;
				bestValue = uctValue;
			}
		}

		return selected;
	}

	private boolean isLeaf() {
		return children == null;
	}

	private double rollOut() {
		int result = sim.getScore() - score;
		return result * result / 2;
	}

	private void updateStats(double reward) {
		nVisits++;
		this.reward += reward;
	}

	private List<MOVE> pacmanAvailableMoves(SimGame s) {
		return s.maze.getAvailableMoves(s.simPacman.tile);
	}

	public int getMaxDepth() {
		if(isLeaf()) {
			return depth;
		} else {
			int max = -1;
			for(TreeNodeSim child : children) {
				max = Math.max(max, child.getMaxDepth());
			}
			return max;
		}
	}

}
