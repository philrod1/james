package ai.hello;

import java.awt.Point;
import java.util.List;

import ai.AbstractAI;
import ai.common.Game;
import ai.common.Node;
import ai.common.MOVE;
import ai.common.Game.BONUS;
import ai.common.Game.GHOST;
import ai.common.Game.PacMan;

public class HelloWorldAI extends AbstractAI {
	
	private PacMan pacman;

	public HelloWorldAI (Game game) {
		this.game = game;
		pacman = game.pacman;
	}

	protected MOVE play() {
		
		try {
			List<MOVE> moves = game.pacman.getAvailableMoves();
			
			for (GHOST ghost : GHOST.values()) {
				if (!game.isEdible(ghost) && game.getState(ghost) == 4) {
					int distance = 
							game.getMaze().distancePath(
									pacman.getTilePosition(),
									game.getTilePosition(ghost));
					if (distance > -1 && distance < 4) {
						moves.remove(
								game.getMaze().getMoveTowards(
										pacman.getTilePosition(),
										game.getTilePosition(ghost)));
					}
				}
			}
			
			if (!moves.isEmpty()) {
				if (moves.size() == 1) {
					return moves.get(0);
				} else {
					// Go after nearest edible ghost
					Node[] ghostNodes = new Node[GHOST.values().length];
					for(GHOST ghost : GHOST.values()) {
						if(!game.isEdible(ghost)) {
							Node ghostNode = game.getMaze().getNode(game.getTilePosition(ghost));
							ghostNodes[ghost.ordinal()] = ghostNode;
							if(ghostNode != null) {
								ghostNode.blockAllMoves();
							}
						}
					}
					int minDistance = Integer.MAX_VALUE;
					MOVE dir = null;
					for (GHOST ghost : GHOST.values()) {
						if (game.isEdible(ghost) && game.getState(ghost) == 4) {
							int distance = game.getMaze().distancePath(
									pacman.getTilePosition(),
									game.getTilePosition(ghost));
							if (distance > -1 && distance < minDistance) {
								minDistance = distance;
								dir = game.getMaze().getMoveTowards(
										pacman.getTilePosition(),
										game.getTilePosition(ghost));
							}
						}
					}
					for(Node ghostNode : ghostNodes) {
						if(ghostNode != null) {
							ghostNode.unblockAllMoves();
						}
					}
					if (dir != null) {
						return dir;
					} else {
						Point target = null;
						
						//Block ghosty paths
						ghostNodes = new Node[GHOST.values().length];
						for(GHOST ghost : GHOST.values()) {
							Node ghostNode = game.getMaze().getNode(game.getTilePosition(ghost));
							ghostNodes[ghost.ordinal()] = ghostNode;
							if(!game.isEdible(ghost) && ghostNode != null) {
								ghostNode.blockAllMoves();
							}
						}
						
						//Go after bonus
						BONUS bonus = game.getCurrentBonus();
						if(bonus != null) {
							target = game.getBonusTilePosition();
						} else {
						
							// Go after nearest pill
							double minD = Double.POSITIVE_INFINITY;
							List<Point> pills = game.getMaze().getPillPositions(game);
							
							for(Point pill : pills) {
								if(!isTooCloseToGhost(pill)) {
									double d = game.getMaze().aStarDistance(pacman.getTilePosition(), pill);
									if(d > -1 && d < minD) {
										minD = d;
										target = pill;
									}
								}
							}
						}
						if(target != null) {
							dir = game.getMaze().getMoveTowardsAStar(pacman.getTilePosition(), target);
						}
						for(Node ghostNode : ghostNodes) {
							if(ghostNode != null) {
								ghostNode.unblockAllMoves();
							}
						}
						if (dir != null) {
							return dir;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private boolean isTooCloseToGhost(Point pill) {
		for(GHOST ghost : GHOST.values()) {
			Point gp = game.getTilePosition(ghost);
			if(Math.abs(gp.x - pill.x) < 3 
					&& Math.abs(gp.y - pill.y) < 3)
				return true;
		}
		return false;
	}

	@Override
	protected void reset() {
		
	}

}
