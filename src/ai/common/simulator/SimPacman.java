package ai.common.simulator;

import java.awt.Point;

import ai.common.Game;
import ai.common.MOVE;

public class SimPacman {
	
	private final int[] stepPatterns = new int[] {
			0b10101010101010101010101010101010,
			0b11010101011010101101010101101010
	};
	private final boolean turbo;
	public Point pixel = new Point(127,196), tile = new Point(15,24);
	private boolean isEnergised = false;
	private MOVE move = MOVE.LEFT;
	private MOVE nextMove = MOVE.LEFT;
	private int frame = 0;
	private int pauseFrames = 0;
	private Point target;
	private final Game game;
	private boolean isAlive;
	
	public SimPacman(Game game, boolean turbo) {
		this.turbo = turbo;
		this.game = game;
	}
	
	public void update(SimGame game) {
		if(move == null) return;
		
		if(isLegal(nextMove, game)) {
			setCurrentMove(nextMove);
		} else {
			return;
		}
		
		if(pauseFrames > 0) {
			pauseFrames--;
			return;
		}
		int steps = getSteps();
		for(int step = 0 ; step < steps ; step++) {
			
			if(isLegal(move, game)) {
	
				int tpx = pixel.x % 8;
				int tpy = pixel.y % 8;
	
				Point delta = move.delta();
				
				switch(move) {
				case UP:
				case DOWN:
					if(tpx < 4) {
						delta.translate(1, 0);
					} else if (tpx > 4) {
						delta.translate(-1, 0);
					}
					break;
				default:
					if(tpy < 4) {
						delta.translate(0, 1);
					} else if (tpy > 4) {
						delta.translate(0, -1);
					}
				}
				pixel.translate(delta.x, delta.y);
				if(pixel.x < 0) {
					pixel.x = 255;
				} else if (pixel.x > 255) {
					pixel.x = 0;
				}
				Point newTile = new Point(pixel.x/8, pixel.y/8);
				
				if(!newTile.equals(tile)) {
					tile = newTile;
					if(!tile.equals(target)) {
						setCurrentMove(this.game.getMaze().getMoveTowards(tile, target));
					}
				}
			}
		}
	}
	
	private boolean isLegal(MOVE move, SimGame game) {
		return game.maze.isLegal(move, pixel);
	}

	public void setPixelPosition(Point p) {
		pixel = p;
		tile = new Point(p.x/8, p.y/8);
	}
	
	public void pause(int frames) {
		this.pauseFrames = frames;
	}
	
	private int getSteps() {
		if (turbo) {
			return 2;
		}
		int index = isEnergised ? 1 : 0;
		int p = stepPatterns[index];
		int val = p & 3;
		val = (val>1) ? val-1 : val;
		stepPatterns[index] = (p << 2) | (p >>> 30);
		return val;
	}

	public void setEnergised(boolean isEnergised) {
		this.isEnergised = isEnergised;
	}

	public void incFrame() {
		frame = ++frame % 16;
	}

	public void setCurrentMove(MOVE move) {
		this.move = move;
		setNextMove(move);
	}
	
	public void setCurrentMove(int pacmanOrientation) {
		switch (pacmanOrientation) {
		case 0: setCurrentMove(MOVE.RIGHT); break;
		case 1: setCurrentMove(MOVE.DOWN);  break;
		case 2: setCurrentMove(MOVE.LEFT);  break;
		case 3: setCurrentMove(MOVE.UP);    break;
		default: System.out.println("WTF! Ms. Pac-Man orientaion is " + pacmanOrientation + "!?!");
		}
	}
	
	public void setNextMove(MOVE move) {
		this.nextMove = move;
	}
	
	public MOVE getCurrentMove() {
		if(move == null) {
			move = MOVE.LEFT;
		}
		return move;
	}

	public Point getTileCentre() {
		return new Point((tile.x*8)+3, (tile.y*8)+4);
	}

	public boolean isEnergised() {
		return isEnergised;
	}

	public void updatePatterns(char[] RAM) {
		int base = 0x4D46;
		stepPatterns[0] = (RAM[base+1] << 24) | (RAM[base] << 16) | (RAM[base+3] << 8) | (RAM[base+2]);
		base = 0x4D4A;
		stepPatterns[0] = (RAM[base+1] << 24) | (RAM[base] << 16) | (RAM[base+3] << 8) | (RAM[base+2]);
	}

	public int getNormalStepPattern() {
		return stepPatterns[0];
	}
	
	public int getEnergisedStepPattern() {
		return stepPatterns[1];
	}

	public void updatePatterns(int[] pacmanPatterns) {
		stepPatterns[0] = pacmanPatterns[0];
		stepPatterns[1] = pacmanPatterns[1];
	}

	public void setTarget(Point target) {
		this.target = target;
		MOVE m = game.getMaze().getMoveTowards(tile, target);
		if(m == null) {
			m = MOVE.LEFT;
			this.target = new Point(14,24);
		}
		setCurrentMove(m);
	}

	public Point getTarget() {
		return target;
	}
	
	public void setAlive(boolean alive) {
		isAlive = alive;
	}

	public boolean isAlive() {
		return isAlive;
	}


}
