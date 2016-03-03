package ai.common.simulator.ghosts;

import java.awt.Point;

import ai.common.MOVE;
import ai.common.simulator.SimGame;

public class Inky extends AbstractGhost {
	
	public Inky() {
		gid = 2;
		homeNextState = 2;
		homeNextMove = MOVE.LEFT;
		startPosition = new Point(111,124);
		pixel = new Point(startPosition);
		tile = new Point(pixel.x/8, pixel.y/8);
		previousOrientation = MOVE.UP;
		currentOrientation = MOVE.UP;
		state = 3;
	}
	
	@Override
	public void leaveHome() {
//		System.out.println("Inky leave home");
		state = 6;
		target = new Point(home);
		previousOrientation = MOVE.RIGHT;
		currentOrientation = MOVE.UP;
	}
	
	@Override
	protected Point getTarget(SimGame game) {
		Point t = game.simPacman.tile;
		if(t == null) {
			return new Point(15,24);
		}
		Point p = new Point(t);
		
		switch(game.simPacman.getCurrentMove()) {
		case UP    : target = new Point(p.x-2, p.y-2); break;
		case DOWN  : target = new Point(p.x,   p.y+2); break;
		case LEFT  : target = new Point(p.x-2, p.y  ); break;
		case RIGHT : target = new Point(p.x+2, p.y  ); break;
		}
		Point b = game.ghosts[0].getTile();
		int dx = target.x - b.x;
		int dy = target.y - b.y;
		target = new Point(target.x + dx, target.y + dy);
		return target;
	}

	@Override
	public int getPersonalPillReleaseCount(int level) {
		return level == 1 ? 30 : 0;
	}
	
	
}
