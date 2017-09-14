package ai.common;

import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import emulator.machine.Machine;
import emulator.machine.Snapshot;



public class Game {
	
	public static final int WIDTH = 32, HEIGHT = 32;
	private MOVE move = null;
	private final Maze[] mazes;
	private Maze maze;
	private int mazeID = 0;
	private STATE lastState = STATE.OFF;
	public final PacMan pacman;
	private int level = 0;
	private int highLevel = 0;
	
	private final Machine machine;
	
	private final boolean invincible = false;
	private final boolean infiniteLives = false;
	private final int skipToLevel = 0;
	
	private boolean logScore = true;
//	private final String logFile = "MCTS_no_emu.txt";
	private final String logFile = "MCTS_fruit.txt";

	public Game (Machine machine) {

		if(invincible) {
			disableCollisions();			
		}
		this.machine = machine;
		pacman = new PacMan();
		mazes = new Maze[]{new Maze(1), new Maze(2), new Maze(3), new Maze(4)};
		maze = mazes[0];
	}

	private void message(String message) {
		String l2 = message;
		String l1 = "LEVEL " + (int) (machine.memoryRead(0x4e13) + 1);
		
		machine.memoryWrite(0x0327, 0xd0);
		machine.memoryWrite(0x0353, 0xcc);
		machine.memoryWrite(0x0354, 0x76);
		machine.memoryWrite(0x0355, 0x03);
		machine.memoryWrite(0x0369, 0xc3);
		machine.memoryWrite(0x036a, 0x5c);
		machine.memoryWrite(0x036b, 0x0f);
		machine.memoryWrite(0x036c, 0xc9);

		for (int i = 0x0f5c; i <= 0x0fa3; i++) {
			machine.memoryWrite(i, 0x0);
		}

		for (char i = 0; i < 12; i++) {
			char c = 0x40;
			try {
				c = l1.charAt(11 - i);
			} catch (Exception e) {
			}
			if (c == 0x20 || c == 0x5F) c = 0x40;
			machine.memoryWrite(0x0f5c + i * 4, 0xdd);
			machine.memoryWrite(0x0f5d + i * 4, 0x36);
			machine.memoryWrite(0x0f5e + i * 4, i);
			machine.memoryWrite(0x0f5f + i * 4, c);
		}

		machine.memoryWrite(0x0fa3, 0xc9);
		machine.memoryWrite(0x032b, 0xc3);
		machine.memoryWrite(0x0376, 0xc3);
		machine.memoryWrite(0x0377, 0xa4);
		machine.memoryWrite(0x0378, 0x0f);
		machine.memoryWrite(0x0390, 0xc9);

		for (int i = 0x0fa4; i <= 0x0feb; i++) {
			machine.memoryWrite(i, 0x0);
		}

		for (char i = 0; i < 12; i++) {
			char c = 0x40;
			try {
				c = l2.charAt(11 - i);
			} catch (Exception e) {
			}
			if (c == 0x20 || c == 0x5F)
				c = 0x40;
			machine.memoryWrite(0x0fa4 + i * 4, 0xfd);
			machine.memoryWrite(0x0fa5 + i * 4, 0x36);
			machine.memoryWrite(0x0fa6 + i * 4, i);
			machine.memoryWrite(0x0fa7 + i * 4, c);
		}
		machine.memoryWrite(0x0feb, 0xc9);
	}

	private void disableCollisions() {
		machine.memoryWrite(0x1764, 0xc3);
		machine.memoryWrite(0x1765, 0xb0);
		machine.memoryWrite(0x1766, 0x1f);
		char[] hex = new char[]{
				/* 1fb0 */	0x21,0xa6,0x4d,
				/* 1fb3 */	0x5f,
				/* 1fb4 */	0x16,0x00,
				/* 1fb6 */	0x19,
				/* 1fb7 */	0x7e,
				/* 1fb8 */	0xa7,
				/* 1fb9 */	0xca,0xc3,0x1f,
				/* 1fbc */	0x78,
				/* 1fbd */	0x32,0xa4,0x4d,
				/* 1fc0 */	0xc3,0x67,0x17,
				/* 1fc3 */	0x3a,0x40,0x50,
				/* 1fc6 */	0xe6,0x20,
				/* 1fc8 */	0xc8,
				/* 1fc9 */	0xc9,
				/* 1fca */	0x32,0xa4,0x4d,
				/* 1fcd */	0xc3,0x67,0x17	
		};
		for(int i = 0 ; i < hex.length ; i++) {
			machine.memoryWrite(i+0x1fb0, hex[i]);
		}
	}
	
	public int getFrame() {
		return (machine.memoryRead(0x4dc0) * 8 + machine.memoryRead(0x4dc4));
	}
	
//	public int readMemory(int address) {
//		return machine.memoryRead(address);
//	}
	
	public int mazeID() {
		return mazeID;
	}
	
	public synchronized void makeMove(MOVE move) {
		if(move != null)
			this.move = move;
	}
	
	public int getLevel() {
		if(lastState == STATE.PLAYING || lastState == STATE.GET_READY) {
			return level();
		}
		return 0;
	}
	
	private int level() {
		return machine.memoryRead(0x4e13) + 1;
	}

	public int getScore() {
		try {
			return Integer.parseInt(
					hexString(machine.memoryRead(0x4e82), 2) + 
					hexString(machine.memoryRead(0x4e81), 2) + 
					hexString(machine.memoryRead(0x4e80), 2) );
		} catch (NumberFormatException nfe) {
			return 0;
		}
	}

	public int getHighScore() {
		try {
			return Integer.parseInt(
					hexString(machine.memoryRead(0x4e8a), 2) + 
					hexString(machine.memoryRead(0x4e89), 2) + 
					hexString(machine.memoryRead(0x4e88), 2) );
		} catch (NumberFormatException nfe) {
			return 0;
		}
	}
	
	public int getNumberOfPillsEaten() {
		if(lastState == STATE.PLAYING) {
			return machine.memoryRead(0x4e0e);
		}
		return 0;
	}
	
	public int getNumberOfLives() {
		if(lastState == STATE.PLAYING) {
			return machine.memoryRead(0x4e14) + 1;
		}
		return 0;
	}
	
	private int checkMazeID() {
		return getMazeID(level);
	}
	
	private int getMazeID(int level) {
		if(level > 5) {
			return (((level - 6) / 4) % 2) + 2;
		} else if (level > 2) {
			return 1;
		} else {
			return 0;
		}
	}
	
	private void updateState() {
		int e00 = machine.memoryRead(0x4e00);
		int e01 = machine.memoryRead(0x4e01);
		int e02 = machine.memoryRead(0x4e02);
		int e04 = machine.memoryRead(0x4e04);
		int ee0 = machine.memoryRead(0x4ee0);
		if (e00 == 3) {
				if(e04 == 3) {
					lastState = STATE.PLAYING;
					if(getLevel() > level) {
						level = getLevel();
						highLevel = Math.max(highLevel, level);
					}
					if(infiniteLives) {
						if(getNumberOfLives()<3) {
							machine.memoryWrite(0x4e14, 4);
						}
					}
				} else 	if (ee0 == 3 || ee0 == 2) {
					if (e04 == 2) {
						lastState = STATE.PAUSED;
					} else if (e04 == 0xd) {
						lastState = STATE.LEVEL_COMPLETE;
						move = MOVE.LEFT;
					} else if (e04 == 0xa) {
						lastState = STATE.GET_READY;
					} else if (e04 == 7) {
						lastState = STATE.GAME_OVER;
						if(logScore) {
							log(level + " " + getScore() + " " + new Date());
							level = 0;
							logScore = false;
						}
					} else if (e04 == 0xc) {
						lastState = STATE.LEVEL_SKIP;
					}
				} else if (e04 == 0x20) {
					lastState = STATE.CUT_SCENE;
				} else if (ee0 == 2 && e04 == 2) {
					lastState = STATE.GET_READY;
				}
			} else if (ee0 == 0 && e00 == 0 && e01 == 0) {
				lastState = STATE.BOOTING;
			} else if (ee0 == 0x10 && e00 == 0x10) {
				lastState = STATE.TESTING;
			} else if (e00 == 1 && e01 == 0 && e02 == 3) {
				lastState = STATE.DEMO;
			} else if (e00 == 2) {
				if (ee0 == 0 || ee0 == 3) {
					lastState = STATE.COIN_IN;
				} else if (ee0 == 2) {
					lastState = STATE.START_PRESSED;
					logScore = true;
				}
			} else {
				lastState = STATE.UNKNOWN;
			}
			if(level < skipToLevel) {
				skipLevel();
			}
			message("LEVEL " + highLevel);
	}

	private int count = 0;
	private void log(String string) {
		System.out.println("Logging " + string);
		FileWriter log = null;
		try {
			log = new FileWriter(new File("res/" + logFile ), true);
			log.write(string + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				log.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(++count == 100) {
			System.exit(0);
		}
	}



	public STATE getState() {
		return lastState;
	}

	public void update() {
		updateState();
		int mid = checkMazeID();
		if (mid >= 0 && mid != mazeID) {
			mazeID = mid;
			maze = mazes[mid];
			move = MOVE.LEFT;
		}
		if(move != null) {
			machine.portWrite(0, (char) move.portValue());
		}
	}
	
	public Maze getMaze() {
		return maze;
	}
	
	public void insertCoin() {
		action(0, 223);
	}
	
	public void player1Start() {
		action(1 ,223);
	}
	
	private synchronized void skipLevel() {
		if(lastState == STATE.PLAYING) {
			machine.memoryWrite(0x4e04, 0xc);
			getState();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {}
		}
	}
	
	private synchronized void action(int port, int value) {
		machine.portWrite(port, (char) value);
	}
	
	private String hexString(int value, int width) {
		String hex = Integer.toHexString(value);
		StringBuilder sb = new StringBuilder();
		while(hex.length() < width) {
			sb.append('0');
			width--;
		}
		sb.append(hex);
		return sb.toString();
	}
	
	/* * * * * * * * * * * * * * * * * * * * * * *
	 * Ghost operations
	 * * * * * * * * * * * * * * * * * * * * * * */
	/**
	 * 0 = dead, 1 = entering home, 2 = leaving home, 3 = at home,
	 * 4 = going for pac-man, 5 = crossing the door, 6 = going to the door
	 */
	public int getState(GHOST ghost) {
		int offset = ghost.ordinal();
		int state = machine.memoryRead(0x4dac + offset);
		if(state == 0) {
			return 3 + machine.memoryRead(0x4da0 + offset);
		}
		return state - 1;
	}
	
	public Point getTilePosition(GHOST ghost) {
		int offset = ghost.ordinal() * 2;
		return new Point (
				31 - (machine.memoryRead(0x4d32 + offset) - 30), 
					  machine.memoryRead(0x4d31 + offset) - 32);
	}
	
	public Point getPixelPosition(GHOST ghost) {
		int offset = ghost.ordinal() * 2;
		return new Point (
				255 - machine.memoryRead(0x4d01 + offset), 
					  machine.memoryRead(0x4d00 + offset));
	}
	
	public MOVE getLastMoveMade(GHOST ghost) {
		int offset = ghost.ordinal() * 2;
		return MOVE.decodeLastMove(
				machine.memoryRead(0x4d15 + offset), 
				machine.memoryRead(0x4d14 + offset));
	}
	
	public MOVE getGhostPreviousOrientation(GHOST ghost) {
		int offset = ghost.ordinal();
		switch(machine.memoryRead(0x4d28 + offset)) {
		case 0: return MOVE.RIGHT;
		case 1: return MOVE.DOWN;
		case 2: return MOVE.LEFT;
		case 3: return MOVE.UP;
		default:
			System.out.println("WTF!");
			return null;
		}
	}

	public boolean isEdible(GHOST ghost) {
		return (machine.memoryRead(0x4da7 + ghost.ordinal()) != 0) && getState(ghost) == 4;
	}
	
	/* * * * * * * * * * * * * * * * * * * * * * * * *
	 * BONUS operations
	 * * * * * * * * * * * * * * * * * * * * * * * * */
	public Point getBonusPixelPosition() {
		int bonus = machine.memoryRead(0x4c0c);
		if(bonus >= 0 && bonus < 7) {
			return new Point(254-machine.memoryRead(0x4dd3), machine.memoryRead(0x4dd2));
		}
		return null;
	}
	
	/*
	 * The the bonus doesn't have a variable for tile position like the others,
	 * but it does suffer from the walk-through bug.  i'm not sure exactly how
	 * bonus collisions are detected, but this doesn't always work.
	 */
	public Point getBonusTilePosition() {
		int bonus = machine.memoryRead(0x4c0c);
		if(bonus >= 0 && bonus < 7) {
			return new Point(31-(machine.memoryRead(0x4dd3)/8), machine.memoryRead(0x4dd2)/8);
		}
		return null;
	}
	
	public BONUS getCurrentBonus() {
		int bonus = machine.memoryRead(0x4c0c);
		if(bonus >= 0 && bonus < 7 && 
				(31-(machine.memoryRead(0x4dd3)/8)) > 1 && 
				(31-(machine.memoryRead(0x4dd3)/8)) < 30) {
			return BONUS.values()[bonus];
		}
		return null;
	}

	public boolean isPill(int address) {
		return machine.memoryRead(address) == 0x10;
	}

	public boolean isPowerPill(int address) {
		return machine.memoryRead(address) == 0x14;
	}

	public final class PacMan {
		
		public Point getTilePosition() {
			Point p = new Point(31-(machine.memoryRead(0x4d3a)-30), machine.memoryRead(0x4d39)-32);
			if(maze.isValidTile(p)) {
				return p;
			}
			return null;
		}
		
		public Point getPixelPosition() {
			return new Point(255-machine.memoryRead(0x4d09), machine.memoryRead(0x4d08));
		}
		
		public MOVE getLastMoveMade() {
			return MOVE.decodeLastMove(
					machine.memoryRead(0x4d1d), 
					machine.memoryRead(0x4d1c));
		}
		
		public Point getNearestPillPosition() {
			return maze.getNearestPill(getTilePosition(), Game.this);
		}
		
		public MOVE getMoveTowardsNearestPill() {
			Point p = getNearestPillPosition();
			return maze.getMoveTowards(getTilePosition(), p);
		}
		
		public int distanceToGhost(GHOST ghost) {
			return maze.distancePath(getTilePosition(), Game.this.getTilePosition(ghost));
		}
		
		public MOVE moveTowardsGhost(GHOST ghost) {
			System.out.println(ghost + " " + Game.this.getTilePosition(ghost));
			return maze.getMoveTowards(getTilePosition(), Game.this.getTilePosition(ghost));
		}
		
		public MOVE moveTowardsTile(Point tile) {
			return maze.getMoveTowards(getTilePosition(), tile);
		}
		
		public List<MOVE> getAvailableMoves() {
			return maze.getAvailableMoves(getTilePosition());
		}
		
		public Point getNextTilePosition(MOVE move) {
			return maze.getTileNeighbour(getTilePosition(), move).getPosition();
		}
		
		public boolean isAlive() {
			return machine.memoryRead(0x4da5) == 0;
		}
		
		public boolean isEnergised() {
			return machine.memoryRead(0x4da6) == 1;
		}
		
		public int livesRemaining() {
			return machine.memoryRead(0x4e14);
		}
		
		public int distanceToNearestGhost(){
			Game game = Game.this;
			int minDistance = Integer.MAX_VALUE;
			for (GHOST ghost : GHOST.values()) {
				if (!game.isEdible(ghost) && game.getState(ghost) == 4) {
					int distance = game.getMaze().distancePath(
							game.pacman.getTilePosition(),
							game.getTilePosition(ghost));
					if (distance < minDistance) {
						minDistance = distance;
					}
				}
			}
			return minDistance;
		}
		
		public int distanceToNearestPillPath(MOVE move) {
			Point pac = pacman.getTilePosition();
			int best = 1000000;
			List<Point> pills = getPillPositions();
			for(Point pill : pills) { 
				best = Math.min(best,maze.getAllDistances(pac, pill)[move.ordinal()]);
			}
			return best;
		}
		
		public int[] distancesToPoint(Point p) {
			return maze.getAllDistances(pacman.getTilePosition(), p);
		}
		
		public double distanceToNearestFruit(MOVE move) {
			try {
				if(getCurrentBonus() != null) {
					Point pac = pacman.getTilePosition();
					return maze.getAllDistances(pac, getBonusTilePosition())[move.ordinal()];
				}
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
			return 1000000;
		}
		
	}
	
	public List<Point> getPillPositions() {
		return maze.getPillPositions(this);
	}

	public List<Point> getNormalPillPositions() {
		return maze.getNormalPillPositions(this);
	}

	public List<Point> getPowerPillPositions() {
		return maze.getPowerPillPositions(this);
	}
	
	public enum BONUS {
		CHERRY 		{ public int points() { return 100;  }},
		STRAWBERRY 	{ public int points() { return 200;  }},
		ORANGE 		{ public int points() { return 500;  }},
		PRETZEL 	{ public int points() { return 700;  }},
		APPLE	 	{ public int points() { return 1000; }},
		PEAR	 	{ public int points() { return 2000; }},
		BANANA	 	{ public int points() { return 5000; }};
		public abstract int points();
	}
	
	public enum GHOST {
		BLINKY, PINKY, INKY, SUE;
	}
	public enum STATE {
		BOOTING, TESTING, DEMO, COIN_IN, START_PRESSED, GET_READY, KILLED,
		PLAYING, PAUSED, LEVEL_COMPLETE, CUT_SCENE, GAME_OVER, OFF, LEVEL_SKIP, UNKNOWN;
	}
	public Snapshot getSnapshot() {
		return machine.getSnapshot();
	}

	public boolean allGhostsOut() {
		for(GHOST g : GHOST.values()) {
			if(getState(g) != 4) {
				return false;
			}
		}
		return true;
	}
	
	public int getLastLevel() {
		return machine.memoryRead(0x4e13) + 1;
	}
}
