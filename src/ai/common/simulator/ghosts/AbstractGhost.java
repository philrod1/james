package ai.common.simulator.ghosts;

import java.awt.Point;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import ai.common.MOVE;
import ai.common.simulator.SimGame;
import ai.common.simulator.maze.SimMaze;
import ai.common.simulator.maze.Tile;


public abstract class AbstractGhost implements Ghost {
	
	protected MOVE currentOrientation = MOVE.LEFT;
	protected MOVE previousOrientation = MOVE.LEFT;
	protected Point target = null;
	protected Point pixel = null;
	protected Point tile = null;
	protected int cruiseLevel = 0;
	protected boolean isFrightened = false;
	protected int frame = 0;
	protected int state = 3;
	protected boolean slow = false;
	protected boolean tileChanged = false;
	protected final Random rng = new Random();
	protected final Point home = new Point (127,127);
	protected final Point door = new Point(127,100);
	protected final int homeBottom = 127, homeTop = 120, homeLeft = 111, homeRight = 143;
	protected boolean reverse = false;
	protected Point startPosition;
	protected int homeNextState;
	protected MOVE homeNextMove;
	protected MOVE leaveHomeMove;
	private int chompPause;
	protected int gid;
	
	
	private static HashSet<Point> ghostHome = new HashSet<>();
	
	static {
		ghostHome.add(new Point(15,13));
		ghostHome.add(new Point(16,13));
		ghostHome.add(new Point(13,14));
		ghostHome.add(new Point(14,14));
		ghostHome.add(new Point(15,14));
		ghostHome.add(new Point(16,14));
		ghostHome.add(new Point(17,14));
		ghostHome.add(new Point(18,14));
		ghostHome.add(new Point(13,15));
		ghostHome.add(new Point(14,15));
		ghostHome.add(new Point(15,15));
		ghostHome.add(new Point(16,15));
		ghostHome.add(new Point(17,15));
		ghostHome.add(new Point(18,15));
		ghostHome.add(new Point(13,16));
		ghostHome.add(new Point(14,16));
		ghostHome.add(new Point(15,16));
		ghostHome.add(new Point(16,16));
		ghostHome.add(new Point(17,16));
		ghostHome.add(new Point(18,16));
	}
	
	protected int[] currentPatterns = new int[] {
			0b10101010101010010101010101010100, // Normal
			0b00100100100100100010010010010010, // Scared
			0b00100010001000100010001000100010,	// Slow
			0, // Cruise 1 (Blinky only)
			0, // Cruise 2 (Blinky only)
		};

	
	public int getState() {
		return state;
	}
	
	public void incFrame() {
		frame = ++frame % 16;
	}

	public void setState(int state) {
		this.state = state;
	}

	public static enum GHOST_STATE {
		DEAD, 
		ENTERING_HOME, 
		MOVE_AWAY_FROM_EXIT, 
		AT_HOME, 
		OUTSIDE, 
		LEAVING_HOME, 
		MOVE_TOWARDS_EXIT
	}
	
	public MOVE getOrientation() {
		return currentOrientation;
	}
	
	@Override
	public void update(SimGame game) {

		if(chompPause > 0) {
			chompPause--;
			return;
		}
		
		int steps = getSteps();
		
		for(int step = 0 ; step < steps ; step++) {
			
			previousOrientation = calculateNextMove(game);
			Point delta = previousOrientation.delta();

			pixel.translate(delta.x, delta.y);
			if(pixel.x < 0) {
				pixel.x = 255;
			} else if (pixel.x > 255) {
				pixel.x = 0;
			}
			Point newTile = new Point(pixel.x/8, pixel.y/8);
			if(newTile.x != tile.x || newTile.y != tile.y) {
				tileChanged = true;
			}

			tile = newTile;
		}
	}
	
	protected MOVE calculateNextMove(SimGame game) {
		List<MOVE> moves;
		
		// Make the appropriate move for the current state
		try {
			switch (state) {
			case 0: //DEAD
				if(reverse) {
					reverse = false;
				}
				if(pixel.equals(door)) {
					state = 1;
					target = home;
					return MOVE.DOWN;
				} else {
					target = door;
				}
				
				if(tileChanged) {
						moves = game.maze.getAvailableMoves(tile);
						moves.remove(previousOrientation.opposite());
						if (moves.size() == 1) {
							currentOrientation = moves.get(0);
						} else {
							currentOrientation = calculateMove(game, moves, tile, new Point(door.x/8,door.y/8));
						}
					tileChanged = false;
				}
				
				if(isTileCentre(pixel)) {
					previousOrientation = currentOrientation;
				}
				return previousOrientation;
			case 1: //Entering Home
				 if(pixel.y >= home.y) {
					state = homeNextState;
					isFrightened = false;
					target = new Point(startPosition);
					currentOrientation = homeNextMove;
					return homeNextMove;
				} else {
					currentOrientation = MOVE.DOWN;
					return MOVE.DOWN;
				}
			case 2: //Move away from exit (Inky and Sue)
				if(this instanceof Inky) {
					if(pixel.x <= startPosition.x) {
						state = 3;
						currentOrientation = MOVE.UP;
						return MOVE.UP;
					} else {
						currentOrientation = MOVE.LEFT;
						return MOVE.LEFT;
					}
				} else if(pixel.x >= startPosition.x) {
					state = 3;
					currentOrientation = MOVE.UP;
					return MOVE.UP;
				} else {
					currentOrientation = MOVE.RIGHT;
					return MOVE.RIGHT;
				}
			case 3:  //At home
				if(pixel.y >= startPosition.y + 4) {
					if(this instanceof Blinky) {
						state = 5;
						target = game.simPacman.tile;
						currentOrientation = MOVE.LEFT;
						return MOVE.LEFT;
					}
					currentOrientation = MOVE.UP;
					return MOVE.UP;
				} else if (pixel.y <= startPosition.y-2){
					currentOrientation = MOVE.DOWN;
					return MOVE.DOWN;
				}
				return previousOrientation;
			case 4: //Outside
				if (reverse) {
					previousOrientation = previousOrientation.opposite();
					reverse = false;
				} else {
					moves = game.maze.getAvailableMoves(tile);
					if(moves == null) {
						System.err.println("No moves for " + getClass().getSimpleName() + " " + tile);
						return previousOrientation;
					}
					
					if (isFrightened) {
						moves.remove(previousOrientation);
						currentOrientation = moves.get(rng.nextInt(moves.size()));
					} else {
						moves.remove(previousOrientation.opposite());
						Point nextTile = game.maze.getNextTile(tile, previousOrientation);
						if(isDecisionPoint(pixel,nextTile,game.maze)) {
							target = getTarget(game);
							moves = game.maze.getAvailableMoves(nextTile);
							moves.remove(previousOrientation.opposite());
							if(game.areGhostsRandom()) {
								currentOrientation = moves.get(rng.nextInt(moves.size()));
							} else {
								currentOrientation = calculateMove(game, moves, nextTile, target);
							}
						}
					}
						
					if(isTileCentre(pixel) && game.maze.isDecisionTile(tile)) {
						previousOrientation = currentOrientation;
					}
				}
				return previousOrientation;
			case 5: //Leaving home
				if(pixel.y == door.y) {
					state = 4;
					if(this instanceof Sue) {
						reverse = true;
					}
					currentOrientation = MOVE.LEFT;
					previousOrientation = MOVE.LEFT;
				} else {
					currentOrientation = MOVE.UP;
					previousOrientation = MOVE.UP;
				}
				return previousOrientation;
			case 6: //Move towards exit (Inky and Sue)
				if(pixel.x == home.x) {
					state = 5;
					return MOVE.UP;
				} else {
					return homeNextMove.opposite();
				}
			default:
				System.out.println("AbstractGhost says \"WTF!\"");
				return previousOrientation;
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
			return previousOrientation;
		}
	}
	
	private boolean isDecisionPoint(Point pixel, Point tile, SimMaze maze) {
		if(maze.isLegalTilePoint(tile)) {
			if(maze.isDecisionTile(tile)) {
				switch(previousOrientation) {
				case UP:	return pixel.y % 8 == 4;
				case DOWN:	return pixel.y % 8 == 4;
				case LEFT:	return pixel.x % 8 == 4;
				case RIGHT:	return pixel.x % 8 == 4;
				default: 	return false;
				}
			}
		} else {
			System.out.println(tile + " is not legal.");
		}
		return false;
	}

	protected abstract Point getTarget(SimGame game);
	
	public void setFrightened(boolean frightened) {
		this.isFrightened = frightened;
		tileChanged = true;
	}
	
	public Point getTile() {
		return tile;
	}
	
	public void reverse() {
		reverse  = true;
	}
	
	public void setPreviousOrientation(int moveVal) {
		switch(moveVal) {
		case 0:  previousOrientation = MOVE.RIGHT; break;
		case 1:  previousOrientation = MOVE.DOWN; break;
		case 2:  previousOrientation = MOVE.LEFT; break;
		case 3:  previousOrientation = MOVE.UP; break;
		default: previousOrientation = null;
		}
	}
	
	public void setCurrentOrientation(int moveVal) {
		switch(moveVal) {
		case 0:  currentOrientation = MOVE.RIGHT; break;
		case 1:  currentOrientation = MOVE.DOWN; break;
		case 2:  currentOrientation = MOVE.LEFT; break;
		case 3:  currentOrientation = MOVE.UP; break;
		default: currentOrientation = null;
		}
	}

	public void setPixelPosition(Point position) {
		this.pixel = position;
		tile = new Point(pixel.x/8, pixel.y/8);
	}
	
	public void chomp(int pause) {
		chompPause = pause;
		state = 0;
		setFrightened(false);
		tileChanged = true;
		target = door;
	}
	
	public void setSlow(boolean slow) {
		this.slow = slow;
	}
	
	protected MOVE calculateMove(SimGame game, List<MOVE> moves, Point tile, Point target) {
		MOVE m = previousOrientation;
		Tile t = game.maze.getTile(tile);
		double dist = 10000000;
		for (MOVE move : new MOVE[] { MOVE.UP, MOVE.LEFT, MOVE.DOWN, MOVE.RIGHT }) {
			if (moves.contains(move)) {
				Tile next = t.getNeighbour(move);
				if (next != null) {
					double d = target.distance(next.getPosition());
					if (d < dist) {
						dist = d;
						m = move;
					}
				}
			}
		}
		return m;
	}

	protected MOVE calculatePixelMove(SimGame game, List<MOVE> moves, Point pixel, Point target) {
		MOVE m = previousOrientation;
		Tile t = game.maze.getTile(new Point(pixel.x/8, pixel.y/8));
		double dist = 10000000;
		for(MOVE move : new MOVE[]{MOVE.UP,MOVE.RIGHT,MOVE.DOWN,MOVE.LEFT}) {
			if(moves.contains(move)) {
				Tile next = t.getNeighbour(move);
				if(next != null) {
					Point tp = next.getPosition();
					double d = target.distance(new Point(tp.x*8+4, tp.y*8+4));
					if(d < dist) {
						dist = d;
						m = move;
					}
				}
			}
		}
		return m;
	}

	@Override
	public Point getStartPosition() {
		return startPosition;
	}

	protected boolean isTileCentre(Point pixel) {
		switch (previousOrientation) {
		case UP:
			return pixel.y%8 == 4;
		case DOWN:
			return pixel.y%8 == 4;
		case LEFT:
			return pixel.x%8 == 4;
		case RIGHT:
			return pixel.x%8 == 4;
		default:
			System.out.println("AbstractGhost.isTileCentre() WTF!");
			return false;
		}
	}
	
	public Point getPosition() {
		return pixel;
	}

	public boolean isFrightened() {
		return isFrightened;
	}
	
	protected int getSteps() {
		if (state < 2) {
			return 2;
		}
		int index = 0;
		if (isFrightened) {
			index = 1;
		} else if (state != 4 || slow) {
			index = 2;
		}
		int p = currentPatterns[index];
		int val = p & 3;
		val = (val>1) ? val-1 : val;
		currentPatterns[index] = (p << 2) | (p >>> 30);
		return val;
	}
	
	@Override
	public void updatePatterns(char[] RAM) {
		int base = 0x4D56 + gid*12;
		for(int i = 0 ; i < 3 ; i++) {
			currentPatterns[i] = (RAM[base + i*4] << 24) | (RAM[base+1 + i*4] << 16) | (RAM[base+2 + i*4] << 8) | (RAM[base+3 + i*4]);
		}
	}

	@Override
	public void setCruiseLevel(int cruiseLevel) {}
	
	@Override
	public int getID() {
		return gid;
	}
	
	@Override
	public int[] getData(int pillCount) {
		return new int[] {
				toTinyData(pillCount),
				currentPatterns[0],
				currentPatterns[1],
				currentPatterns[2],
				currentPatterns[3],
				currentPatterns[4]
		};
	}
	
	public void updatePatterns(int[] patterns) {
		this.currentPatterns = patterns;
	}
	
	private int toTinyData(int pillCount) {
		int data = pixel.x;
		data <<= 8;
		data |= pixel.y;
		data <<= 3;
		data |= state;
		data <<= 2;
		data |= 3-previousOrientation.ordinal();
		data <<= 2;
		data |= 3-currentOrientation.ordinal();
		data <<= 1;
		data |= isFrightened ? 1 : 0;
		data <<=8;
		data |= pillCount;
		return data;
	}
	
	@Override
	public int getCruiseLevel() {
		return 0;
	}
	
	@Override
	public String getCurrentPattern() {
		int index = 0;
		if (isFrightened) {
			index = 1;
		} else if (state != 4 || slow) {
			index = 2;
		}
		String p = Integer.toBinaryString(currentPatterns[index]);
		if(p.length() == 31) {
			return "0" + p;
		}
		if(p.length() == 30) {
			return "00" + p;
		}
		return p;
	}
}
