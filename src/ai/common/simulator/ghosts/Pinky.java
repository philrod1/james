package ai.common.simulator.ghosts;

import java.awt.Point;

import ai.common.MOVE;
import ai.common.simulator.SimGame;

public class Pinky extends AbstractGhost {
	
	public Pinky() {
		gid = 1;
		homeNextState = 3;
		homeNextMove = MOVE.UP;
		startPosition = new Point(127,124);
		pixel = new Point(startPosition);
		tile = new Point(pixel.x/8, pixel.y/8);
		previousOrientation = MOVE.UP;
		currentOrientation = MOVE.UP;
		state = 3;
	}

	@Override
	public int getPersonalPillReleaseCount(int level) {
		return 0;
	}

	@Override
	public void leaveHome() {
		state = 5;
		target = door;
		previousOrientation = MOVE.UP;
	}

	@Override
	protected Point getTarget(SimGame game) {
		Point t = game.simPacman.tile;
		if(t == null) {
			return new Point(15,24);
		}
		Point p = new Point(t);
		MOVE pmm = game.simPacman.getCurrentMove();
		if(pmm == null) {
			System.err.println("Pinky get target pacman move is null");
			return p;
		}
		switch(pmm) {
		case UP    : target = new Point(p.x-4, p.y-4); break;
		case DOWN  : target = new Point(p.x,   p.y+4); break;
		case LEFT  : target = new Point(p.x-4, p.y  ); break;
		case RIGHT : target = new Point(p.x+4, p.y  ); break;
		}
		return target;
	}

}
