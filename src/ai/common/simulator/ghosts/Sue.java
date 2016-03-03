package ai.common.simulator.ghosts;

import java.awt.Point;

import ai.common.MOVE;
import ai.common.simulator.SimGame;

public class Sue extends AbstractGhost {
	
	public Sue() {
		gid = 3;
		homeNextState = 2;
		homeNextMove = MOVE.RIGHT;
		startPosition = new Point(143,124);
		pixel = new Point(startPosition);
		tile = new Point(pixel.x/8, pixel.y/8);
		previousOrientation = MOVE.UP;
		currentOrientation = MOVE.UP;
		state = 3;
	}
	
	@Override
	public void leaveHome() {
		state = 6;
		target = new Point(home);
		previousOrientation = MOVE.LEFT;
		currentOrientation = MOVE.UP;
	}

	@Override
	public int getPersonalPillReleaseCount(int level) {
		switch (level) {
		case 1:  return 60;
		case 2:  return 50;
		default: return 0;
		}
	}

	@Override
	protected Point getTarget(SimGame game) {
		Point t = game.simPacman.tile;
		if(t == null) {
			return new Point(15,24);
		}
		Point p = new Point(t);		
		int distance = (int) (tile.distance(p));
		distance *= distance;
//		System.out.println();
//		System.out.println("Sim: " + distance);
		target = new Point(2,33);
		if(distance > 64) {
			target = new Point(p);
		}
		return target;
	}
	
}
