package ai.common;

import java.awt.Point;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import ai.common.MOVE;
import ai.common.Game.GHOST;
import emulator.games.Pacman;
import emulator.machine.PartMachine;
import emulator.machine.Snapshot;

public class Emulator {

	private final PartMachine emulator;
	private final Random rng = new Random();
	
	public Emulator (Pacman pacman) {
		emulator = new PartMachine(pacman);
	}

	public void step() {
		emulator.step();
	}
	
	public int advanceToTarget(Point target, Maze maze) {
		try {
			Point pacman = getPacmanTile();
			while(!pacman.equals(target)) {
				if(!validPoint(pacman)) {
					return -1;
				}
				MOVE move = maze.getMoveTowards(pacman, target);
				if(move == null) {
					return -1;
				}
				setMove(move);
				emulator.step();
				if(emulator.memoryRead(0x4e04) == 0xd) {  // Maze complete
					return 1;
				}
				if(emulator.memoryRead(0x4da5) != 0) {  // Dead
					return -1;
				}
				pacman = getPacmanTile();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		return 0;
	}
	
	public void setMove(MOVE move) {
		if(move == null) {
			return;
		}
		emulator.ioWrite(0, (char) move.portValue());
	}
	
	private Point getPacmanTile() {
		return new Point(31-(emulator.memoryRead(0x4d3a)-30), emulator.memoryRead(0x4d39)-32);
	}
	
	private boolean validPoint(Point tile) {
		return tile != null && tile.x < 32 && tile.y < 32 && tile.x >= 0 && tile.y >=0;
	}

	public boolean advanceToTargetSimple(Point target, MOVE move) {
		if(move == null || target == null) {
			return false;
		}
		setMove(move);
		Point pacman = getPacmanTile();
		while(!(pacman.equals(target))) {
			emulator.step();
			if(!isPacmanAlive()) {
				return false;
			}
			if(isWin()) {
				return true;
			}
			pacman = getPacmanTile();
		}
		return true;
	}
	
	public int safeToTarget(Point target, Maze maze, Snapshot snap) {
		syncToSnapshot(snap);
		return advanceToTarget(target, maze);
	}

	public boolean advanceToTargetSimple(MOVE move, Point target, Snapshot snap) {
		syncToSnapshot(snap);
		return advanceToTargetSimple(target, move);
	}

	public boolean isTargetSafe(Point t, Maze maze, Snapshot snap, int depth, MOVE currentMove) {
		if(depth == 0) {
			return true;
		} else {
			if(safeToTarget(t, maze, snap) >= 0) {
				List<MOVE> moves = maze.getAvailableMoves(t);
				moves.remove(currentMove.opposite());
				Snapshot snap2 = getSnapshot();
				for(MOVE move : moves) {
					if(isTargetSafe(maze.getNextDecisionPoint(t, move), maze, snap2, depth - 1, move)) {
						return true;
					}
					syncToSnapshot(snap2);
				}
			}
		}
		return false;
	}

	public void syncToSnapshot(Snapshot snapshot) {
		emulator.syncToSnapshot(snapshot);
	}

	public Point getPacmanTilePosition() {
		return getPacmanTile();
	}
	
	public Point getTilePosition(GHOST ghost) {
		int offset = ghost.ordinal() * 2;
		return new Point (
				31 - (emulator.memoryRead(0x4d32 + offset) - 30),
				emulator.memoryRead(0x4d31 + offset) - 32);
	}
	
	public boolean isEdible(GHOST ghost) {
		return emulator.memoryRead(0x4da7 + ghost.ordinal()) != 0;
	}

	public Point[] getAllGhosts() {
			List<GHOST> egs = new LinkedList<GHOST>();
			for(GHOST g : GHOST.values()) {
				if(getState(g) == 4) {
					egs.add(g);
				}
			}
			Point[] points = new Point[egs.size()];
			for(int i = 0 ; i < points.length ;i++) {
				points[i] = getTilePosition(egs.get(i));
			}
			return points;
	}
	
	public boolean isPacmanEnergised() {
		return emulator.memoryRead(0x4da6) == 1;
	}
	
	public int getState(GHOST ghost) {
		int offset = ghost.ordinal();
		int state = emulator.memoryRead(0x4dac + offset);
		if(state == 0) {
			return 3 + emulator.memoryRead(0x4da0 + offset);
		}
		return state - 1;
	}

	public Snapshot getSnapshot() {
		return emulator.getSnapshot();
	}

	public void disableCollisions() {
		emulator.memoryWrite(0x1764, 0xc3);
		emulator.memoryWrite(0x1765, 0x88);
		emulator.memoryWrite(0x1766, 0x17);
	}
	
	public void enableCollisions() {
		emulator.memoryWrite(0x1764, 0x32);
		emulator.memoryWrite(0x1765, 0xa4);
		emulator.memoryWrite(0x1766, 0x4d);
	}

	public Point getBonusTilePosition() {
			// I think bonus collisions may be pixel, not tile, based.
			int bonus = emulator.memoryRead(0x4c0c);
			if(bonus >= 0 && bonus < 7) {
				return new Point(31-(emulator.memoryRead(0x4dd3)/8), emulator.memoryRead(0x4dd2)/8);
			}
			return null;
	}

	public Point getBonusPixelPosition() {
		int bonus = emulator.memoryRead(0x4c0c);
		if(bonus >= 0 && bonus < 7) {
			return new Point(254- emulator.memoryRead(0x4dd3), emulator.memoryRead(0x4dd2));
		}
		return null;
	}

	public MOVE getPacmanCurrentMove() {
			return MOVE.decodeLastMove(
					emulator.memoryRead(0x4d1d),
					emulator.memoryRead(0x4d1c));
	}
	
	public int getScore() {
		try {
			return Integer.parseInt(
					hexString(emulator.memoryRead(0x4e82)) +
					hexString(emulator.memoryRead(0x4e81)) +
					hexString(emulator.memoryRead(0x4e80)) );
		} catch (NumberFormatException nfe) {
			return 0;
		}
	}

	private String hexString(int value) {
		String hex = Integer.toHexString(value);
		StringBuilder sb = new StringBuilder();
		if(hex.length() < 2) {
			sb.append('0');
		}
		sb.append(hex);
		return sb.toString();
	}

	public boolean isPacmanAlive() {
		return emulator.memoryRead(0x4da5) == 0;
	}

	public Point getPacmanPixelPosition() {
		return new Point(255- emulator.memoryRead(0x4d09), emulator.memoryRead(0x4d08));
	}

	public List<MOVE> getPacmanAvailableMoves(Maze maze) {
		return maze.getAvailableMoves(getPacmanTile());
	}

	public boolean isWin() {
		return emulator.memoryRead(0x4e04) == 0xd;
	}

}
