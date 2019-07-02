package ai.common.simulator.data;

import java.awt.Point;
import java.util.LinkedList;
import java.util.List;

import emulator.machine.Snapshot;
import ai.common.MOVE;
import ai.common.simulator.SimGame;

public class SimData {

	public final GhostData blinky, pinky, inky, sue;
	public final int pacmanOrientation;
	public final int pacX, pacY;
	public final int pacmanNormalPattern;
	public final int pacmanEnergisedPattern;
	public final int energisedFramesRemaining;
	public final int level;
	public List<Point> pillData;
	public List<Point> powerPillData;
	public final int framesSincePillEaten;
	public final int globalPillCount;
	public final boolean globalMode, pacmanEnenergised;
	public final boolean pacmanIsAlive;
	public final int mazeId;
	public final int score;
	public boolean random;
	
	public SimData(char[] RAM) {
		random = RAM[0x0dc1] == 0;
		blinky = new GhostData(RAM, 0);
		pinky  = new GhostData(RAM, 1);
		inky   = new GhostData(RAM, 2);
		sue    = new GhostData(RAM, 3);
		pacmanOrientation = RAM[0x0d30];
		pacmanIsAlive = RAM[0x0da5] == 0;
		pacX = 255-RAM[0x0d09];
		pacY =     RAM[0x0d08];
		energisedFramesRemaining = ((RAM[0x0dcc] * 256) + RAM[0x0dcb])/2;
		level = RAM[0x0e13] + 1;
//		System.out.println("SimData from RAM level = " + level);
		mazeId = getMazeNumber(level-1);
		pillData = new LinkedList<Point>();
		score = getLastScore(RAM);
//		for(int i = 0 ; i < 4 ; i++) {
//			System.out.println(pillPositions[i].length);
//		}
		int start = 0x4000;
		for(Point p : pillPositions[mazeId]) {
			int address = start + (31-p.x) * 32 + p.y;
//			System.out.println(p + "  RAM[0x" + Integer.toHexString(address) + "] = " + (int)(RAM[address]));
			if(RAM[address] == 16) {
				pillData.add(p);
			}
		}
		powerPillData = new LinkedList<Point>();
		for(Point p : powerPillPositions[mazeId]) {
			int address = start + (31-p.x) * 32 + p.y;
			if(RAM[address] == 20) {
				powerPillData.add(p);
			}
		}
//		System.out.println("SimData from RAM " + pillData.size() + " ... " + powerPillData.size());
		framesSincePillEaten = ((RAM[0x0d98] * 256) + RAM[0x0d97]);
		globalPillCount = RAM[0x0d9f];
		globalMode = RAM[0x0e12] == 1;
		pacmanEnenergised = RAM[0x0da6]==1;
		int base = 0x4D46;
		pacmanNormalPattern = (RAM[base+1] << 24) | (RAM[base] << 16) | (RAM[base+3] << 8) | (RAM[base+2]);
		base = 0x4D4A;
		pacmanEnergisedPattern = (RAM[base+1] << 24) | (RAM[base] << 16) | (RAM[base+3] << 8) | (RAM[base+2]);
	}
	
	public SimData(Snapshot snap) {
		this(snap.RAM);
	}
	
	public SimData(SimGame game) {
		random = game.areGhostsRandom();
		blinky = new GhostData(game.ghosts[0].getData(game.ghosts[0].getCruiseLevel()), 0);
		pinky  = new GhostData(game.ghosts[1].getData(game.getGhostPillCount(1)), 1);
		inky   = new GhostData(game.ghosts[2].getData(game.getGhostPillCount(2)), 2);
		sue    = new GhostData(game.ghosts[3].getData(game.getGhostPillCount(3)), 3);
		MOVE pmm = game.simPacman.getCurrentMove();
		if(pmm != null) {
			pacmanOrientation = 3 - pmm.ordinal();
		} else {
			System.err.println("SimData pacman move is null");
			pacmanOrientation = 0;
		}
		pacX = game.simPacman.pixel.x;
		pacY = game.simPacman.pixel.y;
		pacmanNormalPattern = game.getPacmanNormalPattern();
		pacmanEnergisedPattern = game.getPacmanEnergisedPattern();
		energisedFramesRemaining = game.getEnergisedFramesRemaining();
		pacmanIsAlive = game.simPacman.isAlive();
		level = game.getLevel();
		mazeId = getMazeNumber(level-1);
		score = game.getScore();
		framesSincePillEaten = game.getFramesSincePillEaten();
		globalPillCount = game.getGlobalPillCount();
		globalMode = game.isGlobalMode();
		pacmanEnenergised = game.simPacman.isEnergised();
		pillData = game.maze.getPills();
		powerPillData = game.maze.getPowerPills();
		pillData = new LinkedList<Point>();
		for(Point p : pillPositions[mazeId]) {
			if(game.maze.getTile(p).getValue() == 1) {
				pillData.add(p);
			}
		}
		powerPillData = new LinkedList<Point>();
		for(Point p : powerPillPositions[mazeId]) {
			if(game.maze.getTile(p).getValue() == 2) {
				powerPillData.add(p);
			}
		}
	}
	
	private int getMazeNumber(int level) {
		if(level < 2) return 0;
		if(level < 5) return 1;
		return (((level - 5) / 4) % 2) + 2;
	}

	public int[] getPacmanPatterns() {
		return new int[] {
			pacmanNormalPattern,
			pacmanEnergisedPattern
		};
	}

	public List<Point> getPillData() {
		return new LinkedList<Point>(pillData);
	}
	
	public List<Point> getPowerPillData() {
		return new LinkedList<Point>(powerPillData);
	}
	
	private int getLastScore(char[] RAM) {
		try {
			return Integer.parseInt(
					hexString(RAM[0x0e82]) +
					hexString(RAM[0x0e81]) +
					hexString(RAM[0x0e80]) );
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
	
	private static final Point[][] powerPillPositions = new Point[][] {
		new Point[]{new Point(3,3),		new Point(3,28),	new Point(28,3),	new Point(28,28)},
		new Point[]{new Point(3,5),		new Point(3,27),	new Point(28,5),	new Point(28,27)},
		new Point[]{new Point(3,4),		new Point(3,24),	new Point(28,4),	new Point(28,24)},
		new Point[]{new Point(3,4),		new Point(3,28),	new Point(28,4),	new Point(28,28)}
	};
	
	private static final Point[][] pillPositions = new Point[][] {
		new Point[]{new Point(3,2),   	new Point(3,4),   	new Point(3,5),   	new Point(3,24),
				    new Point(3,25),  	new Point(3,26),  	new Point(3,27),  	new Point(3,29),
				    new Point(3,30),  	new Point(4,2),   	new Point(4,5),   	new Point(4,24),
				    new Point(4,30),  	new Point(5,2),   	new Point(5,5),   	new Point(5,6),
				    new Point(5,7),   	new Point(5,8),   	new Point(5,9),   	new Point(5,10),
				    new Point(5,11),  	new Point(5,12),  	new Point(5,13),  	new Point(5,14),
				    new Point(5,15),  	new Point(5,16),  	new Point(5,17),  	new Point(5,18),
				    new Point(5,19),  	new Point(5,20),  	new Point(5,21),  	new Point(5,22),
				    new Point(5,23),  	new Point(5,24),  	new Point(5,30),  	new Point(6,2),
				    new Point(6,5),   	new Point(6,21),  	new Point(6,24),  	new Point(6,30),
				    new Point(7,2),   	new Point(7,5),   	new Point(7,21),  	new Point(7,24),
				    new Point(7,30),  	new Point(8,2),   	new Point(8,3),   	new Point(8,4),
				    new Point(8,5),   	new Point(8,6),   	new Point(8,7),   	new Point(8,8),
				    new Point(8,9),   	new Point(8,21),  	new Point(8,24),  	new Point(8,25),
				    new Point(8,26), 	new Point(8,27),  	new Point(8,28),  	new Point(8,29),
				    new Point(8,30),  	new Point(9,5),   	new Point(9,9),   	new Point(9,21),
				    new Point(9,24),  	new Point(9,30),  	new Point(10,5),  	new Point(10,9),
				    new Point(10,21), 	new Point(10,24), 	new Point(10,30), 	new Point(11,2),
				    new Point(11,3),  	new Point(11,4),  	new Point(11,5),  	new Point(11,9),
				    new Point(11,21), 	new Point(11,22), 	new Point(11,23), 	new Point(11,24),
				    new Point(11,27), 	new Point(11,28), 	new Point(11,29), 	new Point(11,30),
				    new Point(12,2),  	new Point(12,5),  	new Point(12,9),  	new Point(12,24),
				    new Point(12,27), 	new Point(12,30), 	new Point(13,2),  	new Point(13,5),
				    new Point(13,9),  	new Point(13,24), 	new Point(13,27), 	new Point(13,30),
				    new Point(14,2),  	new Point(14,5),  	new Point(14,6),  	new Point(14,7),
				    new Point(14,8),  	new Point(14,9),  	new Point(14,24), 	new Point(14,25),
				    new Point(14,26), 	new Point(14,27), 	new Point(14,30), 	new Point(15,2),
				    new Point(15,5),  	new Point(15,30), 	new Point(16,2),  	new Point(16,5),
				    new Point(16,30), 	new Point(17,2),  	new Point(17,5),  	new Point(17,6),
				    new Point(17,7),  	new Point(17,8),  	new Point(17,9),  	new Point(17,24),
				    new Point(17,25), 	new Point(17,26), 	new Point(17,27), 	new Point(17,30),
				    new Point(18,2),  	new Point(18,5),  	new Point(18,9),  	new Point(18,24),
				    new Point(18,27), 	new Point(18,30), 	new Point(19,2),  	new Point(19,5),
				    new Point(19,9),  	new Point(19,24), 	new Point(19,27), 	new Point(19,30),
				    new Point(20,2),  	new Point(20,3),  	new Point(20,4),  	new Point(20,5),
				    new Point(20,9),  	new Point(20,21), 	new Point(20,22), 	new Point(20,23),
				    new Point(20,24), 	new Point(20,27), 	new Point(20,28), 	new Point(20,29),
				    new Point(20,30), 	new Point(21,5),  	new Point(21,9),  	new Point(21,21),
				    new Point(21,24), 	new Point(21,30), 	new Point(22,5),  	new Point(22,9),
				    new Point(22,21), 	new Point(22,24), 	new Point(22,30), 	new Point(23,2),
				    new Point(23,3),  	new Point(23,4),  	new Point(23,5),  	new Point(23,6),
				    new Point(23,7),  	new Point(23,8),  	new Point(23,9),  	new Point(23,21),
				    new Point(23,24), 	new Point(23,25), 	new Point(23,26), 	new Point(23,27),
				    new Point(23,28), 	new Point(23,29), 	new Point(23,30), 	new Point(24,2),
				    new Point(24,5),  	new Point(24,21), 	new Point(24,24), 	new Point(24,30),
				    new Point(25,2),  	new Point(25,5),  	new Point(25,21), 	new Point(25,24),
				    new Point(25,30), 	new Point(26,2),  	new Point(26,5),  	new Point(26,6),
				    new Point(26,7),  	new Point(26,8),  	new Point(26,9),  	new Point(26,10),
				    new Point(26,11), 	new Point(26,12), 	new Point(26,13), 	new Point(26,14),
				    new Point(26,15), 	new Point(26,16), 	new Point(26,17), 	new Point(26,18),
				    new Point(26,19), 	new Point(26,20), 	new Point(26,21), 	new Point(26,22),
				    new Point(26,23), 	new Point(26,24), 	new Point(26,30), 	new Point(27,2),
				    new Point(27,5),  	new Point(27,24), 	new Point(27,30), 	new Point(28,2),
				    new Point(28,4),  	new Point(28,5),  	new Point(28,24), 	new Point(28,25),
				    new Point(28,26), 	new Point(28,27), 	new Point(28,29), 	new Point(28,30)},
		new Point[]{new Point(3,6),   	new Point(3,7),		new Point(3,8),		new Point(3,9),
				    new Point(3,10),  	new Point(3,11),	new Point(3,14),	new Point(3,15),
				    new Point(3,16),  	new Point(3,17),	new Point(3,28),	new Point(3,29),
				    new Point(3,30),  	new Point(4,5),		new Point(4,11),	new Point(4,14),
				    new Point(4,17),	new Point(4,27),	new Point(4,30),	new Point(5,5),
				    new Point(5,11),	new Point(5,14),	new Point(5,17),	new Point(5,18),
				    new Point(5,19),	new Point(5,20),	new Point(5,21),	new Point(5,22),
				    new Point(5,23),	new Point(5,24),	new Point(5,25),	new Point(5,26),
				    new Point(5,27),	new Point(5,30),	new Point(6,5),		new Point(6,8),
				    new Point(6,9),		new Point(6,10),	new Point(6,11),	new Point(6,14),
				    new Point(6,21),	new Point(6,24),	new Point(6,30),	new Point(7,5),
				    new Point(7,8),		new Point(7,11),	new Point(7,14),	new Point(7,21),
				    new Point(7,24),	new Point(7,30),	new Point(8,5),		new Point(8,8),
				    new Point(8,11),	new Point(8,12),	new Point(8,13),	new Point(8,14),
				    new Point(8,15),	new Point(8,16),	new Point(8,17),	new Point(8,18),
				    new Point(8,19),	new Point(8,20),	new Point(8,21),	new Point(8,24),
				    new Point(8,25),	new Point(8,26),	new Point(8,27),	new Point(8,28),
				    new Point(8,29),	new Point(8,30),	new Point(9,5),		new Point(9,8),
				    new Point(9,21),	new Point(9,27),	new Point(9,30),	new Point(10,5),
				    new Point(10,8),	new Point(10,21),	new Point(10,27),	new Point(10,30),
				    new Point(11,2),	new Point(11,3),	new Point(11,4),	new Point(11,5),
				    new Point(11,6),	new Point(11,7),	new Point(11,8),	new Point(11,21),
				    new Point(11,24),	new Point(11,25),	new Point(11,26),	new Point(11,27),
				    new Point(11,30),	new Point(12,2),	new Point(12,5),	new Point(12,21),
				    new Point(12,24),	new Point(12,27),	new Point(12,30),	new Point(13,2),
				    new Point(13,5),	new Point(13,21),	new Point(13,22),	new Point(13,23),
				    new Point(13,24),	new Point(13,27),	new Point(13,30),	new Point(14,2),
				    new Point(14,5),	new Point(14,6),	new Point(14,7),	new Point(14,8),
				    new Point(14,9),	new Point(14,27),	new Point(14,28),	new Point(14,29),
				    new Point(14,30),	new Point(15,2),	new Point(15,9),	new Point(15,30),
				    new Point(16,2),	new Point(16,9),	new Point(16,30),	new Point(17,2),
				    new Point(17,5),	new Point(17,6),	new Point(17,7),	new Point(17,8),
				    new Point(17,9),	new Point(17,27),	new Point(17,28),	new Point(17,29),
				    new Point(17,30),	new Point(18,2),	new Point(18,5),	new Point(18,21),
				    new Point(18,22),	new Point(18,23),	new Point(18,24),	new Point(18,27),
				    new Point(18,30),	new Point(19,2),	new Point(19,5),	new Point(19,21),
				    new Point(19,24),	new Point(19,27),	new Point(19,30),	new Point(20,2),
				    new Point(20,3),	new Point(20,4),	new Point(20,5),	new Point(20,6),
				    new Point(20,7),	new Point(20,8),	new Point(20,21),	new Point(20,24),
				    new Point(20,25),	new Point(20,26),	new Point(20,27),	new Point(20,30),
				    new Point(21,5),	new Point(21,8),	new Point(21,21),	new Point(21,27),
				    new Point(21,30),	new Point(22,5),	new Point(22,8),	new Point(22,21),
				    new Point(22,27),	new Point(22,30),	new Point(23,5),	new Point(23,8),
				    new Point(23,11),	new Point(23,12),	new Point(23,13),	new Point(23,14),
				    new Point(23,15),	new Point(23,16),	new Point(23,17),	new Point(23,18),
				    new Point(23,19),	new Point(23,20),	new Point(23,21),	new Point(23,24),
				    new Point(23,25),	new Point(23,26),	new Point(23,27),	new Point(23,28),
				    new Point(23,29),	new Point(23,30),	new Point(24,5),	new Point(24,8),
				    new Point(24,11),	new Point(24,14),	new Point(24,21),	new Point(24,24),
				    new Point(24,30),	new Point(25,5),	new Point(25,8),	new Point(25,9),
				    new Point(25,10),	new Point(25,11),	new Point(25,14),	new Point(25,21),
				    new Point(25,24),	new Point(25,30),	new Point(26,5),	new Point(26,11),
				    new Point(26,14),	new Point(26,17),	new Point(26,18),	new Point(26,19),
				    new Point(26,20),	new Point(26,21),	new Point(26,22),	new Point(26,23),
				    new Point(26,24),	new Point(26,25),	new Point(26,26),	new Point(26,27),
				    new Point(26,30),	new Point(27,5),	new Point(27,11),	new Point(27,14),
				    new Point(27,17),	new Point(27,27),	new Point(27,30),	new Point(28,6),
				    new Point(28,7),	new Point(28,8),	new Point(28,9),	new Point(28,10),
				    new Point(28,11),	new Point(28,14),	new Point(28,15),	new Point(28,16),
				    new Point(28,17),	new Point(28,28),	new Point(28,29),	new Point(28,30)},
		new Point[]{new Point(3,2),		new Point(3,3),		new Point(3,5),		new Point(3,6),
					new Point(3,7),		new Point(3,10),	new Point(3,11),	new Point(3,12),
					new Point(3,13),	new Point(3,14),	new Point(3,15),	new Point(3,16),
					new Point(3,17),	new Point(3,18),	new Point(3,19),	new Point(3,20),
					new Point(3,21),	new Point(3,25),	new Point(3,26),	new Point(3,27),
					new Point(3,28),	new Point(3,29),	new Point(3,30),	new Point(4,2),
					new Point(4,7),		new Point(4,10),	new Point(4,21),	new Point(4,24),
					new Point(4,27),	new Point(4,30),	new Point(5,2),		new Point(5,7),
					new Point(5,10),	new Point(5,21),	new Point(5,22),	new Point(5,23),
					new Point(5,24),	new Point(5,27),	new Point(5,30),	new Point(6,2),
					new Point(6,5),		new Point(6,6),		new Point(6,7),		new Point(6,8),
					new Point(6,9),		new Point(6,10),	new Point(6,21),	new Point(6,27),
					new Point(6,30),	new Point(7,2),		new Point(7,5),		new Point(7,21),
					new Point(7,27),	new Point(7,30),	new Point(8,2),		new Point(8,5),
					new Point(8,21),	new Point(8,22),	new Point(8,23),	new Point(8,24),
					new Point(8,25),	new Point(8,26),	new Point(8,27),	new Point(8,28),
					new Point(8,29),	new Point(8,30),	new Point(9,2),		new Point(9,5),
					new Point(9,6),		new Point(9,7),		new Point(9,8),		new Point(9,9),
					new Point(9,24),	new Point(10,2),	new Point(10,5),	new Point(10,9),
					new Point(10,24),	new Point(11,2),	new Point(11,3),	new Point(11,4),
					new Point(11,5),	new Point(11,9),	new Point(11,21),	new Point(11,22),
					new Point(11,23),	new Point(11,24),	new Point(11,27),	new Point(11,28),
					new Point(11,29),	new Point(11,30),	new Point(12,5),	new Point(12,9),
					new Point(12,21),	new Point(12,24),	new Point(12,27),	new Point(12,30),
					new Point(13,5),	new Point(13,9),	new Point(13,21),	new Point(13,24),
					new Point(13,27),	new Point(13,30),	new Point(14,2),	new Point(14,3),
					new Point(14,4),	new Point(14,5),	new Point(14,6),	new Point(14,7),
					new Point(14,8),	new Point(14,9),	new Point(14,21),	new Point(14,24),
					new Point(14,25),	new Point(14,26),	new Point(14,27),	new Point(14,30),
					new Point(15,2),	new Point(15,9),	new Point(15,30),	new Point(16,2),
					new Point(16,9),	new Point(16,30),	new Point(17,2),	new Point(17,3),
					new Point(17,4),	new Point(17,5),	new Point(17,6),	new Point(17,7),
					new Point(17,8),	new Point(17,9),	new Point(17,21),	new Point(17,24),
					new Point(17,25),	new Point(17,26),	new Point(17,27),	new Point(17,30),
					new Point(18,5),	new Point(18,9),	new Point(18,21),	new Point(18,24),
					new Point(18,27),	new Point(18,30),	new Point(19,5),	new Point(19,9),
					new Point(19,21),	new Point(19,24),	new Point(19,27),	new Point(19,30),
					new Point(20,2),	new Point(20,3),	new Point(20,4),	new Point(20,5),
					new Point(20,9),	new Point(20,21),	new Point(20,22),	new Point(20,23),
					new Point(20,24),	new Point(20,27),	new Point(20,28),	new Point(20,29),
					new Point(20,30),	new Point(21,2),	new Point(21,5),	new Point(21,9),
					new Point(21,24),	new Point(22,2),	new Point(22,5),	new Point(22,6),
					new Point(22,7),	new Point(22,8),	new Point(22,9),	new Point(22,24),
					new Point(23,2),	new Point(23,5),	new Point(23,21),	new Point(23,22),
					new Point(23,23),	new Point(23,24),	new Point(23,25),	new Point(23,26),
					new Point(23,27),	new Point(23,28),	new Point(23,29),	new Point(23,30),
					new Point(24,2),	new Point(24,5),	new Point(24,21),	new Point(24,27),
					new Point(24,30),	new Point(25,2),	new Point(25,5),	new Point(25,6),
					new Point(25,7),	new Point(25,8),	new Point(25,9),	new Point(25,10),
					new Point(25,21),	new Point(25,27),	new Point(25,30),	new Point(26,2),
					new Point(26,7),	new Point(26,10),	new Point(26,21),	new Point(26,22),
					new Point(26,23),	new Point(26,24),	new Point(26,27),	new Point(26,30),
					new Point(27,2),	new Point(27,7),	new Point(27,10),	new Point(27,21),
					new Point(27,24),	new Point(27,27),	new Point(27,30),	new Point(28,2),
					new Point(28,3),	new Point(28,5),	new Point(28,6),	new Point(28,7),
					new Point(28,10),	new Point(28,11),	new Point(28,12),	new Point(28,13),
					new Point(28,14),	new Point(28,15),	new Point(28,16),	new Point(28,17),
					new Point(28,18),	new Point(28,19),	new Point(28,20),	new Point(28,21),
					new Point(28,25),	new Point(28,26),	new Point(28,27),	new Point(28,28),
					new Point(28,29),	new Point(28,30)},
		new Point[]{new Point(3,2),		new Point(3,3),		new Point(3,5),		new Point(3,6),
					new Point(3,7),		new Point(3,8),		new Point(3,9),		new Point(3,24),
					new Point(3,25),	new Point(3,26),	new Point(3,27),	new Point(3,29),
					new Point(3,30),	new Point(4,2),		new Point(4,9),		new Point(4,24),
					new Point(4,30),	new Point(5,2),		new Point(5,9),		new Point(5,10),
					new Point(5,11),	new Point(5,12),	new Point(5,19),	new Point(5,20),
					new Point(5,21),	new Point(5,22),	new Point(5,23),	new Point(5,24),
					new Point(5,30),	new Point(6,2),		new Point(6,3),		new Point(6,4),
					new Point(6,5),		new Point(6,6),		new Point(6,9),		new Point(6,12),
					new Point(6,19),	new Point(6,24),	new Point(6,27),	new Point(6,28),
					new Point(6,29),	new Point(6,30),	new Point(7,2),		new Point(7,6),
					new Point(7,9),		new Point(7,12),	new Point(7,19),	new Point(7,24),
					new Point(7,27),	new Point(7,30),	new Point(8,2),		new Point(8,6),
					new Point(8,7),		new Point(8,8),		new Point(8,9),		new Point(8,12),
					new Point(8,13),	new Point(8,14),	new Point(8,15),	new Point(8,16),
					new Point(8,17),	new Point(8,18),	new Point(8,19),	new Point(8,20),
					new Point(8,21),	new Point(8,24),	new Point(8,25),	new Point(8,26),
					new Point(8,27),	new Point(8,30),	new Point(9,2),		new Point(9,6),
					new Point(9,21),	new Point(9,24),	new Point(9,30),	new Point(10,2),
					new Point(10,6),	new Point(10,21),	new Point(10,24),	new Point(10,30),
					new Point(11,2),	new Point(11,3),	new Point(11,4),	new Point(11,5),
					new Point(11,6),	new Point(11,7),	new Point(11,8),	new Point(11,9),
					new Point(11,21),	new Point(11,22),	new Point(11,23),	new Point(11,24),
					new Point(11,25),	new Point(11,26),	new Point(11,27),	new Point(11,30),
					new Point(12,2),	new Point(12,9),	new Point(12,27),	new Point(12,30),
					new Point(13,2),	new Point(13,9),	new Point(13,27),	new Point(13,30),
					new Point(14,2),	new Point(14,5),	new Point(14,6),	new Point(14,7),
					new Point(14,8),	new Point(14,9),	new Point(14,27),	new Point(14,28),
					new Point(14,29),	new Point(14,30),	new Point(15,2),	new Point(15,5),
					new Point(15,27),	new Point(16,2),	new Point(16,5),	new Point(16,27),
					new Point(17,2),	new Point(17,5),	new Point(17,6),	new Point(17,7),
					new Point(17,8),	new Point(17,9),	new Point(17,27),	new Point(17,28),
					new Point(17,29),	new Point(17,30),	new Point(18,2),	new Point(18,9),
					new Point(18,27),	new Point(18,30),	new Point(19,2),	new Point(19,9),
					new Point(19,27),	new Point(19,30),	new Point(20,2),	new Point(20,3),
					new Point(20,4),	new Point(20,5),	new Point(20,6),	new Point(20,7),
					new Point(20,8),	new Point(20,9),	new Point(20,21),	new Point(20,22),
					new Point(20,23),	new Point(20,24),	new Point(20,25),	new Point(20,26),
					new Point(20,27),	new Point(20,30),	new Point(21,2),	new Point(21,6),
					new Point(21,21),	new Point(21,24),	new Point(21,30),	new Point(22,2),
					new Point(22,6),	new Point(22,21),	new Point(22,24),	new Point(22,30),
					new Point(23,2),	new Point(23,6),	new Point(23,7),	new Point(23,8),
					new Point(23,9),	new Point(23,12),	new Point(23,13),	new Point(23,14),
					new Point(23,15),	new Point(23,16),	new Point(23,17),	new Point(23,18),
					new Point(23,19),	new Point(23,20),	new Point(23,21),	new Point(23,24),
					new Point(23,25),	new Point(23,26),	new Point(23,27),	new Point(23,30),
					new Point(24,2),	new Point(24,6),	new Point(24,9),	new Point(24,12),
					new Point(24,19),	new Point(24,24),	new Point(24,27),	new Point(24,30),
					new Point(25,2),	new Point(25,3),	new Point(25,4),	new Point(25,5),
					new Point(25,6),	new Point(25,9),	new Point(25,12),	new Point(25,19),
					new Point(25,24),	new Point(25,27),	new Point(25,28),	new Point(25,29),
					new Point(25,30),	new Point(26,2),	new Point(26,9),	new Point(26,10),
					new Point(26,11),	new Point(26,12),	new Point(26,19),	new Point(26,20),
					new Point(26,21),	new Point(26,22),	new Point(26,23),	new Point(26,24),
					new Point(26,30),	new Point(27,2),	new Point(27,9),	new Point(27,24),
					new Point(27,30),	new Point(28,2),	new Point(28,3),	new Point(28,5),
					new Point(28,6),	new Point(28,7),	new Point(28,8),	new Point(28,9),
					new Point(28,24),	new Point(28,25),	new Point(28,26),	new Point(28,27),
					new Point(28,29),	new Point(28,30)}
	};
}
