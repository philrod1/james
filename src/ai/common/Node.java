package ai.common;

import java.awt.Point;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;


public class Node implements Serializable {

	private static final long serialVersionUID = -7988485523851984006L;
	public final int x, y, offset, spriteRAM, colourRAM;
	public final char value;
	private final Node[] neighbours;
	private final Node[] blocked;
	private final Node[] nextJunctions;
	private final Node[] nextCorners;
	private final Node[] nextDecisionPoints;
	private Object userData = null;
	private boolean isIntersection = false;
	private boolean isCorner = false;
	private boolean isPacmanDecisionPoint;
	
	public Node (int x, int y, char value) {
		this.x = x; this.y = y;
		this.value = value;
		offset = (31-x) * 32 + y;
		spriteRAM = 0x4000 + offset;
		colourRAM = 0x4400 + offset;
		neighbours = new Node[MOVE.values().length];
		blocked = new Node[MOVE.values().length];
		nextJunctions = new Node[MOVE.values().length];
		nextCorners = new Node[MOVE.values().length];
		nextDecisionPoints = new Node[MOVE.values().length];
	}
	
	public List<MOVE> getAvailableMoves() {
		List<MOVE> moves = new LinkedList<>();
		for(MOVE move : MOVE.values()) {
			Node n = neighbours[move.ordinal()];
			if(n != null) {
				moves.add(move);
			}
		}
		return moves;
	}
	
	public MOVE getMoveToNeighbour(Node node) {
		for(int i = 0 ; i < neighbours.length ; i++) {
			if(neighbours[i] == node) {
				return MOVE.values()[i];
			}
		}
		return null;
	}
	
	public void setNeighbour(MOVE move, Node neighbour) {
		neighbours[move.ordinal()] = neighbour;
	}
	
	public Node getNeighbour(MOVE move) {
		return neighbours[move.ordinal()];
	}
	
	public void setNextJunction(MOVE move, Node junction) {
		nextJunctions[move.ordinal()] = junction;
	}
	
	public Node getNextJunction(MOVE move) {
		return nextJunctions[move.ordinal()];
	}

	public void setNextCorner(MOVE move, Node corner) {
		nextCorners[move.ordinal()] = corner;
	}
	
	public Node getNextCorner(MOVE move) {
		return nextCorners[move.ordinal()];
	}
	
	public Node[] getNeighbours() {
		return neighbours.clone();
	}
	
	public Point getPosition() {
		return new Point(x,y);
	}
	
	public Point getNeighbourPosition(MOVE move) {
		switch (move) {
		case UP:
			return (y==0) ? new Point(x,Game.HEIGHT-1) : new Point(x,y-1);
		case DOWN:
			return (y<Game.HEIGHT-1) ? new Point(x,y+1) : new Point(x,0);
		case RIGHT:
			return (x<Game.WIDTH-1) ? new Point(x+1,y) : new Point(0,y);
		case LEFT:
			return (x==0) ? new Point(Game.WIDTH-1,y) : new Point(x-1,y);
		default:
			return null;
		}
	}
	
	public boolean block(MOVE move) {
		if(neighbours[move.ordinal()] != null) {
			blocked[move.ordinal()] = neighbours[move.ordinal()];
			neighbours[move.ordinal()] = null;
			return true;
		}
		return false;
	}
	
	public void unblock(MOVE move) {
		if(blocked[move.ordinal()] != null) {
			neighbours[move.ordinal()] = blocked[move.ordinal()];
			blocked[move.ordinal()] = null;
		}
	}
	
	public List<MOVE> blockAllExcept(MOVE move) {
		List<MOVE> blockedMoves = new LinkedList<>();
		for(MOVE move2 : MOVE.values()) {
			if(move2 != move) {
				if(block(move2)) {
					blockedMoves.add(move2);
				}
			}
		}
		return blockedMoves;
	}
	
	public void blockAllMoves() {
		for(MOVE move : MOVE.values()) {
			block(move);
		}
	}
	
	public void unblockAllMoves() {
		for(MOVE move : MOVE.values()) {
			unblock(move);
		}
	}

	public void unblockAll(List<MOVE> blockedMoves) {
		if(blockedMoves != null) {
			for(MOVE move : blockedMoves) {
				unblock(move);
			}
		}
	}
	
	@Override
	public String toString() {
		return "Node(" + x + "," + y + ")";
	}
	
	public boolean hasPill(Game game) {
		return game.readMemory(spriteRAM) == 0x10;
	}

	public boolean hasPowerPill(Game game) {
		return game.readMemory(spriteRAM) == 0x14;
	}
	
	public boolean hasPill(char[] RAM) {
		return RAM[spriteRAM] == 0x10;
	}

	public boolean hasPowerPill(char[] RAM) {
		return RAM[spriteRAM] == 0x14;
	}
	
	public boolean isSlowTileForGhosts(Game game) {
		return game.readMemory(colourRAM) == 0x5d;
	}

	@Override
	public int hashCode() {
		return y * Game.WIDTH + x;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node that = (Node) obj;
		return this.x == that.x && this.y == that.y;
	}

	public void setNeighbours(Node[] neighbours) {
		for(int i = 0 ; i < neighbours.length ; i++) {
			this.neighbours[i] = neighbours[i];
		}
	}

	public Object getUserData() {
		return userData;
	}

	public void setUserData(Object userData) {
		this.userData = userData;
	}

	public boolean isIntersection() {
		return isIntersection;
	}
	
	public void setIntersection() {
		isIntersection = getAvailableMoves().size() > 2;
		isPacmanDecisionPoint = isIntersection;
	}
	
	public boolean isCorner() {
		return isCorner;
	}
	
	public boolean isPacmanDecisionPoint() {
		return isPacmanDecisionPoint;
	}
	
	public void setCorner() {
		List<MOVE> moves = getAvailableMoves();
		isCorner = moves.size() == 2 && moves.get(0) != moves.get(1).opposite();
	}

	public void makeIntersection() {
		isIntersection = true;
	}

	public void setPacmanDecisionPoint(boolean b) {
		isPacmanDecisionPoint = b;
	}

	public void setNextPacmanDecisionPoint(MOVE move, Node dp) {
		nextDecisionPoints[move.ordinal()] = dp;
	}
	
	public Node getNextPacmanDecisionPoint(MOVE move) {
		return nextDecisionPoints[move.ordinal()];
	}

	public char toChar() {
		if(value == 1)
			return (char)0x2219;
		if(value == 2)
			return (char)0x2022;
		return ' ';
	}
	
}
