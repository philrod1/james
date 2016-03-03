package ai.common.simulator.ghosts;

import java.awt.Point;

import ai.common.MOVE;
import ai.common.simulator.SimGame;

public class Blinky extends AbstractGhost {
	
	public Blinky() {
		gid = 0;
		homeNextState = 5;
		homeNextMove = MOVE.UP;
		startPosition = new Point(127,100);
		pixel = new Point(startPosition);
		tile = new Point(pixel.x/8, pixel.y/8);
		previousOrientation = MOVE.LEFT;
		currentOrientation = MOVE.LEFT;
		state = 4;
	}

	@Override
	public void setCruiseLevel(int cruiseLevel) {
		this.cruiseLevel = cruiseLevel;
	}

	@Override
	public void leaveHome() {
		state = 3; //TODO - Inky and Sue should be 5
		target = new Point(127,100);
		previousOrientation = MOVE.UP;
		currentOrientation = MOVE.LEFT;
	}

	@Override
	public int getPersonalPillReleaseCount(int level) {
		return 0;
	}

	@Override
	protected Point getTarget(SimGame game) {
		Point t = game.simPacman.tile;
		if(t == null) {
			return new Point(15,24);
		}
		Point p = new Point(t);
		return p;
	}

	@Override
	protected int getSteps() {
		if (state < 2) {
			return 2;
		}
		int index = 0;
		if (isFrightened) {
			index = 1;
		} else if (state != 4 || slow) {
			index = 2;
		} else {
			index = cruiseLevel > 0 ? cruiseLevel+2 : cruiseLevel;
		}
		int p = currentPatterns[index];
		int val = p & 3;
		currentPatterns[index] = (p << 2) | (p >>> 30);
		return (val>1) ? val-1 : val;
	}
	
	@Override
	public void updatePatterns(char[] RAM) {
		currentPatterns[0] =  (RAM[0x4D56+1] << 24) 
							| (RAM[0x4D56+0] << 16) 
							| (RAM[0x4D56+3] << 8) 
							| (RAM[0x4D56+2]);
		currentPatterns[3] =  (RAM[0x4D52+1] << 24) 
							| (RAM[0x4D52+0] << 16) 
							| (RAM[0x4D52+3] << 8) 
							| (RAM[0x4D52+2]);
		currentPatterns[4] =  (RAM[0x4D4E+1] << 24) 
							| (RAM[0x4D4E+0] << 16) 
							| (RAM[0x4D4E+3] << 8) 
							| (RAM[0x4D4E+2]);
		currentPatterns[1] =  (RAM[0x4D5A+1] << 24) 
							| (RAM[0x4D5A+0] << 16) 
							| (RAM[0x4D5A+3] << 8) 
							| (RAM[0x4D5A+2]);
		currentPatterns[2] =  (RAM[0x4D5E+1] << 24) 
							| (RAM[0x4D5E+0] << 16) 
							| (RAM[0x4D5E+3] << 8) 
							| (RAM[0x4D5E+2]);
	}
	
	@Override
	public int getCruiseLevel() {
		return cruiseLevel;
	}

	@Override
	public String getCurrentPattern() {
		System.out.println("Blinky cruise level: " + cruiseLevel);
		int index = 0;
		if (isFrightened) {
			index = 1;
		} else if (state != 4 || slow) {
			index = 2;
		} else {
			index = cruiseLevel > 0 ? cruiseLevel+2 : cruiseLevel;
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
