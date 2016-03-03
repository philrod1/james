package ai.common.simulator.maze;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.LinkedList;
import java.util.List;

import ai.common.MOVE;

public class Tile {

	public static final char[] CHARS = new char[] {
		(char)0x2591,	//	░
		'u',			//	?
		'l',			//	?
		(char)0x255D,	//	╝
		'd',			//	?
		(char)0x2551,	//	║
		(char)0x2557,	//	╗
		(char)0x2563,	//	╣
		'r',			//	?
		(char)0x255A,	//	╚
		(char)0x2550,	//	═
		(char)0x2569,	//	╩
		(char)0x2554,	//	╔
		(char)0x2560,	//	╠
		(char)0x2566,	//	╦
		(char)0x256C,	//	╬	
	};

	private final List<MOVE> moves = new LinkedList<>();
	public final int x, y;
	private final Tile[] neighbours;
	private int value;
	private boolean isGhostHome, isJunction, isCorner, isDecisionPoint;
	public final int spriteRAM;
	
	public Tile(int x, int y) {
		this.x = x;
		this.y = y;
		neighbours = new Tile[4];
		int offset = (31-x) * 32 + y;
		spriteRAM = 0x4000 + offset;
	}

	public boolean hasPill() {
		return value == 1;
	}

	public boolean hasPowerPill() {
		return value == 2;
	}

	public boolean isGhostHome() {
		return isGhostHome;
	}

	public void setHasPill(boolean hasPill) {
		value = hasPill ? 1 : 0;
	}

	public void setHasPowerPill(boolean hasPowerPill) {
		value = hasPowerPill ? 2 : 0;
	}

	public void setGhostHome(boolean isGhostHome) {
		this.isGhostHome = isGhostHome;
	}

	public Tile getNeighbour(MOVE move) {
		return neighbours[move.ordinal()];
	}

	public void setNeighbour(MOVE move, Tile neighbour) {
		neighbours[move.ordinal()] = neighbour;
	}
	
	//For my convenience, this method returns true if the tile has a pill.
	public boolean setValue(char val) {
		value = 0;
		if(val == 0x10) {
			value = 1;
			return true;
		}
		if(val == 0x14) {
			value = 2;
			return true;
		}
		return false;
	}
	
	public void resetValue(int value) {
		this.value = value;
	}
	
	public int getResetValue() {
		return value;
	}

	public void init() {
		for(MOVE move : MOVE.values()) {
			Tile n = neighbours[move.ordinal()];
			if(n != null) {
				moves.add(move);
			}
		}
		if(moves.size() > 2) {
			isJunction = true;
			isDecisionPoint = true;
		} else if (moves.get(0) != moves.get(1).opposite()) {
			isCorner = true;
			isDecisionPoint = true;
		}
	}
	
	public boolean isJunction() {
		return isJunction;
	}
	
	public boolean isCorner() {
		return isCorner;
	}
	
	public byte toByte() {
		return (byte) value;
	}
	
	public char toChar() {
		if(value == 1)
			return (char)0x2219;
		if(value == 2)
			return (char)0x2022;
		return ' ';
	}

	public List<MOVE> getAvailableMoves() {
		return new LinkedList<MOVE>(moves);
	}

	public Point getPosition() {
		return new Point(x,y);
	}

	public Point getCentrePoint() {
		return new Point(x*8+4,y*8+4);
	}

	public void paint(Graphics2D g2) {
		g2.setColor(Color.DARK_GRAY);
		g2.drawRect(x*16, y*16, 16, 16);
		if(value == 1) {
			g2.setColor(Color.WHITE);
			g2.fillOval(getCentrePoint().x*2-2, getCentrePoint().y*2-2, 4, 4);
			return;
		}
		if(value == 2) {
			g2.setColor(Color.WHITE);
			g2.fillOval(getCentrePoint().x*2-4, getCentrePoint().y*2-4, 8, 8);
			return;
		}
	}

	public boolean isDecisionPoint() {
		return isDecisionPoint;
	}
	
	@Override
	public String toString() {
		return "(" + x + "," + y +")";
	}

	public int getValue() {
		return value;
	}
}
