package ai.common;

import java.io.Serializable;

public class MoveDistance implements Serializable {
	private static final long serialVersionUID = 3967182217769962210L;
	public int distance;
	public final MOVE move;
	public MoveDistance (MOVE move, int distance) {
		this.move = move;
		this.distance = distance;
	}
}