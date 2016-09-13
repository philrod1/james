package ai.common.simulator;

import java.awt.Point;
import java.util.List;
import java.util.Random;

import emulator.machine.Snapshot;
import ai.common.Game;
import ai.common.Maze;
import ai.common.MOVE;
import ai.common.simulator.data.SimData;
import ai.common.simulator.ghosts.Blinky;
import ai.common.simulator.ghosts.Ghost;
import ai.common.simulator.ghosts.GhostManager;
import ai.common.simulator.ghosts.Inky;
import ai.common.simulator.ghosts.Pinky;
import ai.common.simulator.ghosts.Sue;
import ai.common.simulator.maze.SimMaze;

public class SimGame {
	
	private static final int[] framesEnergised = new int[]{
			0,360,300,240,180,120,300,120,120,60,300,120,60,60,180,60,60,0,60};
	
	private final boolean collisionsEnabled = true;
	
	private int energisedFramesRemaining = 0;
	private int energiserPauseFramesRemaining = 0;
	private int ghostEatenPauseFramesRemaining = 0;
	public final Ghost[] ghosts = new Ghost[]{new Blinky(), new Pinky(), new Inky(), new Sue()};
	private final GhostManager ghostManager = new GhostManager(ghosts);
	private int level = 1;
	public final SimPacman simPacman;
	public final SimMaze maze = new SimMaze();
	private final MOVE[] pacmanOrientations = new MOVE[] {MOVE.RIGHT,MOVE.DOWN,MOVE.LEFT,MOVE.UP};
	private int ghostsEaten = 0;
	private int score;

	private int pause = 1;

	private final Random rng = new Random();
	
	public SimGame(Game game) {
		this.simPacman = new SimPacman(game);
		level = game.getLevel();
		maze.setMaze(game.getLevel());
		score = game.getScore();
	}
	
	public boolean step() {
		if (ghostEatenPauseFramesRemaining > 0) {
                for (int i = 0; i < 4; i++) {
                	if(ghosts[i].getState() == 0) {
                		ghosts[i].update(this);
                	}
                }
			ghostEatenPauseFramesRemaining--;
			return true;
		}
		
        if (energiserPauseFramesRemaining > 0) {
                for (int i = 0; i < 4; i++) {
                    ghosts[i].update(this);
                }
            energiserPauseFramesRemaining--;
            return true;
        }

        ghostManager.update(level);
        
        if(energisedFramesRemaining > 1) {
        	energisedFramesRemaining--;
        	if(energisedFramesRemaining <= 1) {
        		simPacman.setEnergised(false);
        		for(Ghost ghost : ghosts) {
                	ghost.setFrightened(false);
                }
        	}
        }

		simPacman.update(this);

        //TODO - test level complete?
            
        if (checkGhostCollision()) {
        	return false;
        }

        for(Ghost ghost : ghosts) {
    		if(maze.isSlow(ghost.getTile())) {
    			ghost.setSlow(true);
    		} else {
    			ghost.setSlow(false);
    		}
        	ghost.update(this);
        }
            
        // Checking before and after eliminates walk-through.
        // Safest option for sims, as we can't be absolutely
        // sure we're pixel perfect.
        if (checkGhostCollision()) {
        	return false;
        }

        if(maze.pillEaten(simPacman.tile)) {
			// Really, Pacman pauses for one frame after eating a pill, but two
			// frames gave much better results.  Better safe than sorry, i guess.
        	simPacman.pause(pause + 1);
			pause = (pause + 1) % 2;
        	ghostManager.pillEaten();
        	score += 10;
        }
        
        if(maze.powerPillEaten(simPacman.tile)) {
        	ghostsEaten = 0;
        	simPacman.setEnergised(true);
        	energisedFramesRemaining = getEnergisedFramesRemaining();
        	energiserPauseFramesRemaining = 3;
        	for(Ghost ghost : ghosts) {
            	ghost.reverse();
            	ghost.setFrightened(true);
            }
        	ghostManager.pillEaten();
        	score += 50;
        }

        simPacman.incFrame();
        for(Ghost ghost : ghosts) {
        	ghost.incFrame();
        }

        return true;  
	}
	
	public int getEnergisedFramesRemaining() {
		if(level < 19) {
			return framesEnergised[level];
		}
		return 0;
	}

	private boolean checkGhostCollision() {
		for(Ghost ghost : ghosts) {
			if(ghost.getTile().equals(simPacman.tile)) {
				if(ghost.getState() == 4) {
					if(ghost.isFrightened()) {
						ghost.chomp(60);
						ghostEatenPauseFramesRemaining = 60;
						ghostsEaten++;
						score += 100 * (1 << ghostsEaten);
						return false;
					}
					simPacman.setAlive(false);
					return collisionsEnabled ;
				} else {
					return false;
				}
			}
		}
        return false;
	}
	
	private int getGhostState(char[] RAM, int ghost) {
		int state = RAM[0x4dac + ghost];
		if(state == 0) {
			return 3 + RAM[0x4da0 + ghost];
		}
		return state - 1;
	}
	
	private Point getGhostPixelPosition(char[] RAM, int ghost) {
		int offset = ghost * 2;
		return new Point (
				256-RAM[0x4d01 + offset], 
				RAM[0x4d00 + offset]
			);
	}

	public int isTargetSafe(Point target, SimData snap, int depth, MOVE move, Maze maze) {
		try {
			if (depth == 0) {
				return 0;
			} else {
				syncToDataPoint(snap);
				int result = safeToTarget(target);
				if (result == 0) {
					List<MOVE> moves = maze.getAvailableMoves(simPacman.tile);
					moves.remove(simPacman.getCurrentMove().opposite());
					SimData dataPoint2 = new SimData(this);
					for (MOVE m : moves) {
						int result2 = isTargetSafe(
								maze.getNextDecisionPoint(simPacman.tile, m),
								dataPoint2, depth - 1, m, maze);
						if(result2 != 0) {
							return result2;
						}
						syncToDataPoint(dataPoint2);
					}
				} else if (result == 1) {
					return 1;
				}
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
			return -1;
		}
		return 0;
	}

	private int safeToTarget(Point target) {
		while(!simPacman.tile.equals(target)) {
			simPacman.setTarget(target);
			if(maze.getPillCount() == 0) {
				return 1;
			}
			if(!step()) {
				return -1;
			}
		}
		return 0;
	}

	public void syncToSnapshot(Snapshot snapshot) {
		sync(snapshot.RAM);
	}

	public void sync(char[] RAM) {
		for(int i = 0 ; i < 4 ; i++) {
			ghosts[i].setPixelPosition(getGhostPixelPosition(RAM, i));
			ghosts[i].setPreviousOrientation(RAM[0x4d28+i]);
			ghosts[i].setCurrentOrientation(RAM[0x4d2c+i]);
			ghosts[i].setState(getGhostState(RAM, i));
			ghosts[i].setFrightened(RAM[0x4da7+i] != 0);
			ghosts[i].updatePatterns(RAM);
		}
		ghosts[0].setCruiseLevel(RAM[0x4db6] + RAM[0x4db7]);
		simPacman.setCurrentMove(pacmanOrientations[RAM[0x4d30]]);
		simPacman.setPixelPosition(new Point(255-RAM[0x4d09], RAM[0x4d08]));
		simPacman.setEnergised(RAM[0x4da6]==1);
		simPacman.updatePatterns(RAM);
		simPacman.setAlive(RAM[0x4da5] == 0);
		energisedFramesRemaining = ((RAM[0x4dcc] * 256) + RAM[0x4dcb])/2;
		level = RAM[0x4e13] + 1;
		maze.sync(level, RAM);
		ghostManager.sync(level, RAM);
		score = getLastScore(RAM);
		// Need a reliable way of getting this info ...
		ghostsEaten = 0;
	}

	public void syncToDataPoint(SimData data) {
		ghosts[0].setPixelPosition(new Point(data.blinky.px, data.blinky.py));
		ghosts[0].setPreviousOrientation(data.blinky.previousOrientation);
		ghosts[0].setCurrentOrientation(data.blinky.currentOrientation);
		ghosts[0].setState(data.blinky.state);
		ghosts[0].setFrightened(data.blinky.frightened);
		ghosts[0].updatePatterns(data.blinky.getPatterns());
		ghosts[0].setCruiseLevel(data.blinky.cruiseLevel);
		
		ghosts[1].setPixelPosition(new Point(data.pinky.px, data.pinky.py));
		ghosts[1].setPreviousOrientation(data.pinky.previousOrientation);
		ghosts[1].setCurrentOrientation(data.pinky.currentOrientation);
		ghosts[1].setState(data.pinky.state);
		ghosts[1].setFrightened(data.pinky.frightened);
		ghosts[1].updatePatterns(data.pinky.getPatterns());
		
		ghosts[2].setPixelPosition(new Point(data.inky.px, data.inky.py));
		ghosts[2].setPreviousOrientation(data.inky.previousOrientation);
		ghosts[2].setCurrentOrientation(data.inky.currentOrientation);
		ghosts[2].setState(data.inky.state);
		ghosts[2].setFrightened(data.inky.frightened);
		ghosts[2].updatePatterns(data.inky.getPatterns());
		
		ghosts[3].setPixelPosition(new Point(data.sue.px, data.sue.py));
		ghosts[3].setPreviousOrientation(data.sue.previousOrientation);
		ghosts[3].setCurrentOrientation(data.sue.currentOrientation);
		ghosts[3].setState(data.sue.state);
		ghosts[3].setFrightened(data.sue.frightened);
		ghosts[3].updatePatterns(data.sue.getPatterns());
		
		simPacman.setCurrentMove(data.pacmanOrientation);
		simPacman.setPixelPosition(new Point(data.pacX, data.pacY));
		simPacman.setEnergised(data.pacmanEnenergised);
		simPacman.updatePatterns(data.getPacmanPatterns());
		simPacman.setAlive(data.pacmanIsAlive);
		energisedFramesRemaining = data.energisedFramesRemaining;
		level = data.level;
		maze.sync(data);
		ghostManager.sync(data);
		score = data.score;
		ghostsEaten = 0;
	}

	public int getGhostPillCount(int gid) {
		return ghostManager.getPillCount(gid);
	}

	public int getLevel() {
		return level;
	}

	public int getFramesSincePillEaten() {
		return ghostManager.getFramesSincePillEaten();
	}

	public int getGlobalPillCount() {
		return ghostManager.getGlobalPillCount();
	}

	public boolean isGlobalMode() {
		return ghostManager.isGlobalMode();
	}

	public int getPacmanNormalPattern() {
		return simPacman.getNormalStepPattern();
	}
	
	public int getPacmanEnergisedPattern() {
		return simPacman.getEnergisedStepPattern();
	}
	
	public boolean advanceToNextDecisionPoint(Point target, Maze maze) {
		simPacman.setTarget(target);
		while(!simPacman.tile.equals(target) && this.maze.pillCount > 0) {
			if(!step()) {
				return false;
			}
		}
		return true;
	}

	public boolean isTargetSafe(Point t, Maze maze, SimData snap, int depth, MOVE currentMove) {
		if(depth == 0) {
			return true;
		} else {
			syncToDataPoint(snap);
			if(safeToTarget(t) >= 0) {
				List<MOVE> moves = maze.getAvailableMoves(t);
				moves.remove(currentMove.opposite());
				SimData snap2 = new SimData(this);
				for(MOVE move : moves) {
					if(isTargetSafe(maze.getNextDecisionPoint(t, move), maze, snap2, depth - 1, move)) {
						return true;
					}
					syncToDataPoint(snap2);
				}
			}
		}
		return false;
	}

	public int getScore() {
		return score;
	}
	
	private int getLastScore(char[] RAM) {
		try {
			return Integer.parseInt(
					hexString(RAM[0x4e82]) + 
					hexString(RAM[0x4e81]) + 
					hexString(RAM[0x4e80]) );
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

	public boolean advanceToNextDecisionPoint(MOVE move, Maze maze) {
		Point target = maze.getNextCornerOrJunction(simPacman.tile, move);
		return advanceToNextDecisionPoint(target, maze);
	}

	public int getPillCount() {
		return maze.pillCount;
	}

	public boolean areGhostsRandom() {
		return ghostManager.areGhostsRandom();
	}

	public boolean advanceToTargetSimple(MOVE move, Point target, Snapshot snap) {
		syncToSnapshot(snap);
		simPacman.setTarget(target);
		while(!simPacman.tile.equals(target)) {
			if(!step()) {
				return false;
			}
			if(this.maze.pillCount == 0) {
				return true;
			}
		}
		if(!step()) {
			return false;
		}
		if(!step()) {
			return false;
		}
		return true;
	}

	public double rolloutSafe(Point target, int depth, Maze maze) {
		if (depth==0) return 1;
		simPacman.setTarget(target);
		while(!simPacman.tile.equals(target)) {
			if(!step()) {
				return 0;
			}
			if(this.maze.pillCount == 0) {
				return 1;
			}
		}
		List<MOVE> moves = maze.getAvailableMoves(target);
		moves.remove(simPacman.getCurrentMove().opposite());
		Point target2 = maze.getNextCornerOrJunction(target, moves.get(rng.nextInt(moves.size())));
		return rolloutSafe(target2, depth-1, maze);
	}

	public double rolloutGhostMunch(Point target, int depth, Maze maze) {
		ghostsEaten = 0;
		return rolloutGhostMunchPrivate(target, depth, maze);
	}

	private double rolloutGhostMunchPrivate(Point target, int depth, Maze maze) {
		if (depth==0) {
			return ghostsEaten;
		}
		simPacman.setTarget(target);
		while(!simPacman.tile.equals(target)) {
			if(!step()) {
				return 0;
			}
			if(this.maze.pillCount == 0) {
				return 0;
			}
		}
		List<MOVE> moves = maze.getAvailableMoves(target);
		moves.remove(simPacman.getCurrentMove().opposite());
		Point target2 = maze.getNextCornerOrJunction(target, moves.get(rng.nextInt(moves.size())));
		return rolloutGhostMunchPrivate(target2, depth-1, maze);
	}

	public MOVE oneStepCheck(MOVE move, Game game) {
		Snapshot snap = game.getSnapshot();
		syncToSnapshot(snap);
		Point target = game.getMaze().getNextCornerOrJunction(game.pacman.getTilePosition(), move);
		simPacman.setTarget(target);
		for(int i = 0 ; i < 8 ; i ++) {
			if(!step()) {
				syncToSnapshot(snap);
				List<MOVE> moves = game.pacman.getAvailableMoves();
//				moves.remove(move);
				while(!moves.isEmpty()) {
					MOVE m2 = moves.remove(rng.nextInt(moves.size()));
					if(advanceToNextDecisionPoint(m2, game.getMaze())) {
						return m2;
					}
				}
				return move.opposite();
			}
		}
		return move;
	}

}
