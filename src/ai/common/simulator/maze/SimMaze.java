package ai.common.simulator.maze;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import ai.common.Maze;
import ai.common.MOVE;
import ai.common.simulator.data.SimData;

public class SimMaze {
	private static final int WIDTH = 32, HEIGHT = 32;
	private final Tile[][][] mazes = new Tile[4][][];
	@SuppressWarnings("unchecked")
	private final static HashSet<Point>[] slowTiles = new HashSet[2];
	static {
		slowTiles[0] = new HashSet<Point>();
		for(int x : new int[]{1,3,4,27,28,29}) {
			slowTiles[0].add(new Point(x, 9));
			slowTiles[0].add(new Point(x, 18));
		}
		slowTiles[1] = new HashSet<Point>();
		for(int x = 2 ; x <= 8 ; x++) {
			slowTiles[1].add(new Point(x, 2));
		}
		for(int x = 23 ; x <= 29 ; x++) {
			slowTiles[1].add(new Point(x, 2));
		}
		for(int x = 2 ; x <= 4 ; x++) {
			slowTiles[1].add(new Point(x, 24));
		}
		for(int x = 27 ; x <= 29 ; x++) {
			slowTiles[1].add(new Point(x, 24));
		}
	}
	@SuppressWarnings("unchecked")
	private final List<Tile>[] mazeLists = new List[4];
	private int mazeID = 0;
	private Tile[][] currentMaze = mazes[0];
	public int pillCount = 0;
	
	public SimMaze() {
		buildGraphs();
		currentMaze = mazes[0];
	}
	
	public Tile[][] getMaze() {
		return currentMaze;
	}
	
	public void sync(int level, char[] RAM) {
		setMaze(level);
		pillCount = 0;
		for(Tile tile : mazeLists[getMazeID(level)]) {
			if(tile != null) {
				if(tile.setValue(RAM[tile.spriteRAM])) {
					pillCount++;
				}
			}
		}
	}
	
	public byte[] toByteArray() {
		List<Tile> tiles = mazeLists[mazeID];
		byte[] bytes = new byte[tiles.size()];
		for(int i = 0 ; i < tiles.size() ; i++) {
			bytes[i] = tiles.get(i).toByte();
		}
		return bytes;
	}
	
	public void fromByteArray(byte[] bytes) {
		List<Tile> tiles = mazeLists[mazeID];
		for(int i = 0 ; i < tiles.size() ; i++) {
			tiles.get(i).setValue((char) bytes[i]);
		}
	}
	
	private void buildGraphs() {
		for(int mazeID = 1 ; mazeID < 5 ; mazeID++) {
			try {
				char[][] ram = new char[WIDTH][HEIGHT];
				String data = readFile("res/maze" + mazeID + "_vram.txt", Charset.defaultCharset());
				String[] lines = data.split("[\\r\\n]+");
				for(int y = 0 ; y < HEIGHT ; y++) {
					String[] values = lines[y].split("\\|");
					for(int x = 0 ; x < WIDTH ; x++) {
						int x2 = (HEIGHT - 1) - y;
						int y2 = x;
						String value = values[x];
						ram[x2][y2] = (char) Integer.parseInt(value,16);
					}
				}
				buildGraph(ram, mazeID);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void buildGraph(char[][] ram, int mazeID) {
		mazeLists[mazeID-1] = new ArrayList<Tile>();
		Tile[][] graph = new Tile[WIDTH][HEIGHT];
		for (int x = 0; x < WIDTH; x++) {
			for (int y = 0; y < HEIGHT; y++) {
				switch (ram[x][y]) {
				case 0x40:
					graph[x][y] = new Tile(x, y);
					mazeLists[mazeID-1].add(graph[x][y]);
					graph[x][y].setValue(ram[x][y]);
					break;
				case 0x10:
					graph[x][y] = new Tile(x, y);
					mazeLists[mazeID-1].add(graph[x][y]);
					graph[x][y].setValue(ram[x][y]);
					pillCount++;
					break;
				case 0x14:
					graph[x][y] = new Tile(x, y);
					mazeLists[mazeID-1].add(graph[x][y]);
					graph[x][y].setValue(ram[x][y]);
					pillCount++;
					break;
				default:
					graph[x][y] = null;
				}
			}
		}

		for (Tile t : mazeLists[mazeID - 1]) {
			for (MOVE move : MOVE.values()) {
				Point d = move.delta();
				int x = t.x + d.x;
				int y = t.y + d.y;
				if(x == -1) x = 31;
				if(x == 32) x = 0;
				Tile neighbour = graph[x][y];
				if (neighbour != null) {
					t.setNeighbour(move, neighbour);
				}
			}
			t.init();
		}
		mazes[mazeID-1] = graph;
	}
	
	public void setMaze(int level) {
		mazeID = getMazeID(level);
		switch (mazeID) {
		case 0:  pillCount = 220; break;
		case 1:  pillCount = 240; break;
		case 2:  pillCount = 234; break;
		case 3:  pillCount = 230; break;
		default: pillCount = 0;   break;
		}
		currentMaze = mazes[mazeID];
	}
	
	private int getMazeID(int level) {
		if(level > 5) {
			return (((level - 6) / 4) % 2) + 2;
		} else if (level > 2) {
			return 1;
		} else {
			return 0;
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				Tile t = currentMaze[x][y];
				if(t==null) {
					sb.append((char)0x2591);
				} else {
					sb.append(t.toChar());
				}
			}
			sb.append('\n');
		}
		return sb.toString();
	}
	
	private String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}
	
	public boolean pillEaten(Point msPacmanTile) {
		Tile t = currentMaze[msPacmanTile.x][msPacmanTile.y];
		if(t!=null && t.hasPill()) {
			t.setHasPill(false);
			pillCount--;
			return true;
		}
		return false;
	}
	
	public boolean powerPillEaten(Point msPacmanTile) {
		Tile t = currentMaze[msPacmanTile.x][msPacmanTile.y];
		if(t!=null && t.hasPowerPill()) {
			t.setHasPowerPill(false);
			pillCount--;
			return true;
		}
		return false;
	}

	public List<MOVE> getAvailableMoves(Point tile) {
		if(tile.x == 32) tile.x = 0;
		try {
			Tile t = currentMaze[tile.x][tile.y];
			return t.getAvailableMoves();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(tile);
			System.err.println(mazeID);
			return null;
		}
	}

	public boolean isJunction(Point tile) {
		Tile t = currentMaze[tile.x][tile.y];
		if (t == null) return false;
		return t.isJunction();
	}

	public Tile getTile(Point tile) {
		if(tile.x == 32) tile.x = 0;
		return currentMaze[tile.x][tile.y];
	}

	public Point getNextTile(Point tile, MOVE move) {
		switch (move) {
		case UP: 	return tile.y == 0  ? new Point(tile.x,31)  : new Point(tile.x, tile.y-1);
		case DOWN:	return tile.y == 31 ? new Point(tile.x,0)   : new Point(tile.x, tile.y+1);
		case LEFT:	return tile.x == 0  ? new Point(31, tile.y) : new Point(tile.x-1, tile.y);
		case RIGHT:	return tile.x == 31 ? new Point(0, tile.y)  : new Point(tile.x+1, tile.y);
		default:	return null;
		}
	}

	public boolean isDecisionTile(Point tile) {
		Tile t = getTile(tile);
		if(t!=null) {
			return t.isCorner() || t.isJunction();
		}
		return false;
	}

	public boolean isLegal(MOVE move, Point pixel) {
		Tile t = currentMaze[pixel.x/8][pixel.y/8];
		if (t == null) return false;
		List<MOVE> moves = t.getAvailableMoves();
		if(moves.contains(move)) {
			return true;
		}
		switch(move) {
		case UP:
			return pixel.y > t.getCentrePoint().y;
		case DOWN:
			return pixel.y < t.getCentrePoint().y;
		case RIGHT:
			return pixel.x < t.getCentrePoint().x;
		case LEFT:
			return pixel.x > t.getCentrePoint().x;
		default:
			return false;
		}
	}
	


	public boolean isSlow(Point tile) {
		return mazeID < 2 ? slowTiles[mazeID].contains(tile) : false;
	}
	
	public List<Point> getPills() {
		List<Point> pills = new LinkedList<Point>();
		for(Point p : pillPositions[mazeID]) {
			if(currentMaze[p.x][p.y].hasPill()) {
				pills.add(new Point(p));
			}
		}
		return pills;
	}
	
	public List<Point> getPowerPills() {
		List<Point> pills = new LinkedList<Point>();
		for(Point p : powerPillPositions[mazeID]) {
			if(currentMaze[p.x][p.y].hasPowerPill()) {
				pills.add(new Point(p));
			}
		}
		return pills;
	}

	public void sync(SimData data) {
		setMaze(data.level);
		pillCount = 0;
		for(Point pill : data.getPillData()) {
			currentMaze[pill.x][pill.y].setValue((char)0x10);
			pillCount++;
		}
		for(Point pill : data.getPowerPillData()) {
			currentMaze[pill.x][pill.y].setValue((char)0x14);
			pillCount++;
		}
	}

	public Point getNextDecisionPoint(Point tile, MOVE currentMove, Maze maze) {
		List<MOVE> moves = maze.getAvailableMoves(tile);
		if(!moves.contains(currentMove)) {  // Corner
			moves.remove(currentMove.opposite());
			currentMove = moves.get(0);
		}
		return maze.getNextDecisionPoint(tile, currentMove);
	}
	
	private static final Point[][] powerPillPositions = new Point[][] {
		new Point[]{new Point(3,3),		new Point(3,28),	new Point(28,3),	new Point(28,28)},
		new Point[]{new Point(3,5),		new Point(3,27),	new Point(28,5),	new Point(28,27)},
		new Point[]{new Point(3,4),		new Point(3,24),	new Point(28,4),	new Point(28,24)},
		new Point[]{new Point(3,4),		new Point(3,28),	new Point(28,4),	new Point(28,28)}
	};
	
	private static final Point[][] pillPositions = new Point[][] {
		new Point[]{new Point(3,2),   	new Point(3,4),   	new Point(3,5),   	new Point(3,24),
				    new Point(3,25),  	new Point(3,26),  	new Point(3,27),  	new Point(3,29),
				    new Point(3,30),  	new Point(4,2),   	new Point(4,5),   	new Point(4,24),
				    new Point(4,30),  	new Point(5,2),   	new Point(5,5),   	new Point(5,6),
				    new Point(5,7),   	new Point(5,8),   	new Point(5,9),   	new Point(5,10),
				    new Point(5,11),  	new Point(5,12),  	new Point(5,13),  	new Point(5,14),
				    new Point(5,15),  	new Point(5,16),  	new Point(5,17),  	new Point(5,18),
				    new Point(5,19),  	new Point(5,20),  	new Point(5,21),  	new Point(5,22),
				    new Point(5,23),  	new Point(5,24),  	new Point(5,30),  	new Point(6,2),
				    new Point(6,5),   	new Point(6,21),  	new Point(6,24),  	new Point(6,30),
				    new Point(7,2),   	new Point(7,5),   	new Point(7,21),  	new Point(7,24),
				    new Point(7,30),  	new Point(8,2),   	new Point(8,3),   	new Point(8,4),
				    new Point(8,5),   	new Point(8,6),   	new Point(8,7),   	new Point(8,8),
				    new Point(8,9),   	new Point(8,21),  	new Point(8,24),  	new Point(8,25),
				    new Point(8,26), 	new Point(8,27),  	new Point(8,28),  	new Point(8,29),
				    new Point(8,30),  	new Point(9,5),   	new Point(9,9),   	new Point(9,21),
				    new Point(9,24),  	new Point(9,30),  	new Point(10,5),  	new Point(10,9),
				    new Point(10,21), 	new Point(10,24), 	new Point(10,30), 	new Point(11,2),
				    new Point(11,3),  	new Point(11,4),  	new Point(11,5),  	new Point(11,9),
				    new Point(11,21), 	new Point(11,22), 	new Point(11,23), 	new Point(11,24),
				    new Point(11,27), 	new Point(11,28), 	new Point(11,29), 	new Point(11,30),
				    new Point(12,2),  	new Point(12,5),  	new Point(12,9),  	new Point(12,24),
				    new Point(12,27), 	new Point(12,30), 	new Point(13,2),  	new Point(13,5),
				    new Point(13,9),  	new Point(13,24), 	new Point(13,27), 	new Point(13,30),
				    new Point(14,2),  	new Point(14,5),  	new Point(14,6),  	new Point(14,7),
				    new Point(14,8),  	new Point(14,9),  	new Point(14,24), 	new Point(14,25),
				    new Point(14,26), 	new Point(14,27), 	new Point(14,30), 	new Point(15,2),
				    new Point(15,5),  	new Point(15,30), 	new Point(16,2),  	new Point(16,5),
				    new Point(16,30), 	new Point(17,2),  	new Point(17,5),  	new Point(17,6),
				    new Point(17,7),  	new Point(17,8),  	new Point(17,9),  	new Point(17,24),
				    new Point(17,25), 	new Point(17,26), 	new Point(17,27), 	new Point(17,30),
				    new Point(18,2),  	new Point(18,5),  	new Point(18,9),  	new Point(18,24),
				    new Point(18,27), 	new Point(18,30), 	new Point(19,2),  	new Point(19,5),
				    new Point(19,9),  	new Point(19,24), 	new Point(19,27), 	new Point(19,30),
				    new Point(20,2),  	new Point(20,3),  	new Point(20,4),  	new Point(20,5),
				    new Point(20,9),  	new Point(20,21), 	new Point(20,22), 	new Point(20,23),
				    new Point(20,24), 	new Point(20,27), 	new Point(20,28), 	new Point(20,29),
				    new Point(20,30), 	new Point(21,5),  	new Point(21,9),  	new Point(21,21),
				    new Point(21,24), 	new Point(21,30), 	new Point(22,5),  	new Point(22,9),
				    new Point(22,21), 	new Point(22,24), 	new Point(22,30), 	new Point(23,2),
				    new Point(23,3),  	new Point(23,4),  	new Point(23,5),  	new Point(23,6),
				    new Point(23,7),  	new Point(23,8),  	new Point(23,9),  	new Point(23,21),
				    new Point(23,24), 	new Point(23,25), 	new Point(23,26), 	new Point(23,27),
				    new Point(23,28), 	new Point(23,29), 	new Point(23,30), 	new Point(24,2),
				    new Point(24,5),  	new Point(24,21), 	new Point(24,24), 	new Point(24,30),
				    new Point(25,2),  	new Point(25,5),  	new Point(25,21), 	new Point(25,24),
				    new Point(25,30), 	new Point(26,2),  	new Point(26,5),  	new Point(26,6),
				    new Point(26,7),  	new Point(26,8),  	new Point(26,9),  	new Point(26,10),
				    new Point(26,11), 	new Point(26,12), 	new Point(26,13), 	new Point(26,14),
				    new Point(26,15), 	new Point(26,16), 	new Point(26,17), 	new Point(26,18),
				    new Point(26,19), 	new Point(26,20), 	new Point(26,21), 	new Point(26,22),
				    new Point(26,23), 	new Point(26,24), 	new Point(26,30), 	new Point(27,2),
				    new Point(27,5),  	new Point(27,24), 	new Point(27,30), 	new Point(28,2),
				    new Point(28,4),  	new Point(28,5),  	new Point(28,24), 	new Point(28,25),
				    new Point(28,26), 	new Point(28,27), 	new Point(28,29), 	new Point(28,30)},
		new Point[]{new Point(3,6),   	new Point(3,7),		new Point(3,8),		new Point(3,9),
				    new Point(3,10),  	new Point(3,11),	new Point(3,14),	new Point(3,15),
				    new Point(3,16),  	new Point(3,17),	new Point(3,28),	new Point(3,29),
				    new Point(3,30),  	new Point(4,5),		new Point(4,11),	new Point(4,14),
				    new Point(4,17),	new Point(4,27),	new Point(4,30),	new Point(5,5),
				    new Point(5,11),	new Point(5,14),	new Point(5,17),	new Point(5,18),
				    new Point(5,19),	new Point(5,20),	new Point(5,21),	new Point(5,22),
				    new Point(5,23),	new Point(5,24),	new Point(5,25),	new Point(5,26),
				    new Point(5,27),	new Point(5,30),	new Point(6,5),		new Point(6,8),
				    new Point(6,9),		new Point(6,10),	new Point(6,11),	new Point(6,14),
				    new Point(6,21),	new Point(6,24),	new Point(6,30),	new Point(7,5),
				    new Point(7,8),		new Point(7,11),	new Point(7,14),	new Point(7,21),
				    new Point(7,24),	new Point(7,30),	new Point(8,5),		new Point(8,8),
				    new Point(8,11),	new Point(8,12),	new Point(8,13),	new Point(8,14),
				    new Point(8,15),	new Point(8,16),	new Point(8,17),	new Point(8,18),
				    new Point(8,19),	new Point(8,20),	new Point(8,21),	new Point(8,24),
				    new Point(8,25),	new Point(8,26),	new Point(8,27),	new Point(8,28),
				    new Point(8,29),	new Point(8,30),	new Point(9,5),		new Point(9,8),
				    new Point(9,21),	new Point(9,27),	new Point(9,30),	new Point(10,5),
				    new Point(10,8),	new Point(10,21),	new Point(10,27),	new Point(10,30),
				    new Point(11,2),	new Point(11,3),	new Point(11,4),	new Point(11,5),
				    new Point(11,6),	new Point(11,7),	new Point(11,8),	new Point(11,21),
				    new Point(11,24),	new Point(11,25),	new Point(11,26),	new Point(11,27),
				    new Point(11,30),	new Point(12,2),	new Point(12,5),	new Point(12,21),
				    new Point(12,24),	new Point(12,27),	new Point(12,30),	new Point(13,2),
				    new Point(13,5),	new Point(13,21),	new Point(13,22),	new Point(13,23),
				    new Point(13,24),	new Point(13,27),	new Point(13,30),	new Point(14,2),
				    new Point(14,5),	new Point(14,6),	new Point(14,7),	new Point(14,8),
				    new Point(14,9),	new Point(14,27),	new Point(14,28),	new Point(14,29),
				    new Point(14,30),	new Point(15,2),	new Point(15,9),	new Point(15,30),
				    new Point(16,2),	new Point(16,9),	new Point(16,30),	new Point(17,2),
				    new Point(17,5),	new Point(17,6),	new Point(17,7),	new Point(17,8),
				    new Point(17,9),	new Point(17,27),	new Point(17,28),	new Point(17,29),
				    new Point(17,30),	new Point(18,2),	new Point(18,5),	new Point(18,21),
				    new Point(18,22),	new Point(18,23),	new Point(18,24),	new Point(18,27),
				    new Point(18,30),	new Point(19,2),	new Point(19,5),	new Point(19,21),
				    new Point(19,24),	new Point(19,27),	new Point(19,30),	new Point(20,2),
				    new Point(20,3),	new Point(20,4),	new Point(20,5),	new Point(20,6),
				    new Point(20,7),	new Point(20,8),	new Point(20,21),	new Point(20,24),
				    new Point(20,25),	new Point(20,26),	new Point(20,27),	new Point(20,30),
				    new Point(21,5),	new Point(21,8),	new Point(21,21),	new Point(21,27),
				    new Point(21,30),	new Point(22,5),	new Point(22,8),	new Point(22,21),
				    new Point(22,27),	new Point(22,30),	new Point(23,5),	new Point(23,8),
				    new Point(23,11),	new Point(23,12),	new Point(23,13),	new Point(23,14),
				    new Point(23,15),	new Point(23,16),	new Point(23,17),	new Point(23,18),
				    new Point(23,19),	new Point(23,20),	new Point(23,21),	new Point(23,24),
				    new Point(23,25),	new Point(23,26),	new Point(23,27),	new Point(23,28),
				    new Point(23,29),	new Point(23,30),	new Point(24,5),	new Point(24,8),
				    new Point(24,11),	new Point(24,14),	new Point(24,21),	new Point(24,24),
				    new Point(24,30),	new Point(25,5),	new Point(25,8),	new Point(25,9),
				    new Point(25,10),	new Point(25,11),	new Point(25,14),	new Point(25,21),
				    new Point(25,24),	new Point(25,30),	new Point(26,5),	new Point(26,11),
				    new Point(26,14),	new Point(26,17),	new Point(26,18),	new Point(26,19),
				    new Point(26,20),	new Point(26,21),	new Point(26,22),	new Point(26,23),
				    new Point(26,24),	new Point(26,25),	new Point(26,26),	new Point(26,27),
				    new Point(26,30),	new Point(27,5),	new Point(27,11),	new Point(27,14),
				    new Point(27,17),	new Point(27,27),	new Point(27,30),	new Point(28,6),
				    new Point(28,7),	new Point(28,8),	new Point(28,9),	new Point(28,10),
				    new Point(28,11),	new Point(28,14),	new Point(28,15),	new Point(28,16),
				    new Point(28,17),	new Point(28,28),	new Point(28,29),	new Point(28,30)},
		new Point[]{new Point(3,2),		new Point(3,3),		new Point(3,5),		new Point(3,6),
					new Point(3,7),		new Point(3,10),	new Point(3,11),	new Point(3,12),
					new Point(3,13),	new Point(3,14),	new Point(3,15),	new Point(3,16),
					new Point(3,17),	new Point(3,18),	new Point(3,19),	new Point(3,20),
					new Point(3,21),	new Point(3,25),	new Point(3,26),	new Point(3,27),
					new Point(3,28),	new Point(3,29),	new Point(3,30),	new Point(4,2),
					new Point(4,7),		new Point(4,10),	new Point(4,21),	new Point(4,24),
					new Point(4,27),	new Point(4,30),	new Point(5,2),		new Point(5,7),
					new Point(5,10),	new Point(5,21),	new Point(5,22),	new Point(5,23),
					new Point(5,24),	new Point(5,27),	new Point(5,30),	new Point(6,2),
					new Point(6,5),		new Point(6,6),		new Point(6,7),		new Point(6,8),
					new Point(6,9),		new Point(6,10),	new Point(6,21),	new Point(6,27),
					new Point(6,30),	new Point(7,2),		new Point(7,5),		new Point(7,21),
					new Point(7,27),	new Point(7,30),	new Point(8,2),		new Point(8,5),
					new Point(8,21),	new Point(8,22),	new Point(8,23),	new Point(8,24),
					new Point(8,25),	new Point(8,26),	new Point(8,27),	new Point(8,28),
					new Point(8,29),	new Point(8,30),	new Point(9,2),		new Point(9,5),
					new Point(9,6),		new Point(9,7),		new Point(9,8),		new Point(9,9),
					new Point(9,24),	new Point(10,2),	new Point(10,5),	new Point(10,9),
					new Point(10,24),	new Point(11,2),	new Point(11,3),	new Point(11,4),
					new Point(11,5),	new Point(11,9),	new Point(11,21),	new Point(11,22),
					new Point(11,23),	new Point(11,24),	new Point(11,27),	new Point(11,28),
					new Point(11,29),	new Point(11,30),	new Point(12,5),	new Point(12,9),
					new Point(12,21),	new Point(12,24),	new Point(12,27),	new Point(12,30),
					new Point(13,5),	new Point(13,9),	new Point(13,21),	new Point(13,24),
					new Point(13,27),	new Point(13,30),	new Point(14,2),	new Point(14,3),
					new Point(14,4),	new Point(14,5),	new Point(14,6),	new Point(14,7),
					new Point(14,8),	new Point(14,9),	new Point(14,21),	new Point(14,24),
					new Point(14,25),	new Point(14,26),	new Point(14,27),	new Point(14,30),
					new Point(15,2),	new Point(15,9),	new Point(15,30),	new Point(16,2),
					new Point(16,9),	new Point(16,30),	new Point(17,2),	new Point(17,3),
					new Point(17,4),	new Point(17,5),	new Point(17,6),	new Point(17,7),
					new Point(17,8),	new Point(17,9),	new Point(17,21),	new Point(17,24),
					new Point(17,25),	new Point(17,26),	new Point(17,27),	new Point(17,30),
					new Point(18,5),	new Point(18,9),	new Point(18,21),	new Point(18,24),
					new Point(18,27),	new Point(18,30),	new Point(19,5),	new Point(19,9),
					new Point(19,21),	new Point(19,24),	new Point(19,27),	new Point(19,30),
					new Point(20,2),	new Point(20,3),	new Point(20,4),	new Point(20,5),
					new Point(20,9),	new Point(20,21),	new Point(20,22),	new Point(20,23),
					new Point(20,24),	new Point(20,27),	new Point(20,28),	new Point(20,29),
					new Point(20,30),	new Point(21,2),	new Point(21,5),	new Point(21,9),
					new Point(21,24),	new Point(22,2),	new Point(22,5),	new Point(22,6),
					new Point(22,7),	new Point(22,8),	new Point(22,9),	new Point(22,24),
					new Point(23,2),	new Point(23,5),	new Point(23,21),	new Point(23,22),
					new Point(23,23),	new Point(23,24),	new Point(23,25),	new Point(23,26),
					new Point(23,27),	new Point(23,28),	new Point(23,29),	new Point(23,30),
					new Point(24,2),	new Point(24,5),	new Point(24,21),	new Point(24,27),
					new Point(24,30),	new Point(25,2),	new Point(25,5),	new Point(25,6),
					new Point(25,7),	new Point(25,8),	new Point(25,9),	new Point(25,10),
					new Point(25,21),	new Point(25,27),	new Point(25,30),	new Point(26,2),
					new Point(26,7),	new Point(26,10),	new Point(26,21),	new Point(26,22),
					new Point(26,23),	new Point(26,24),	new Point(26,27),	new Point(26,30),
					new Point(27,2),	new Point(27,7),	new Point(27,10),	new Point(27,21),
					new Point(27,24),	new Point(27,27),	new Point(27,30),	new Point(28,2),
					new Point(28,3),	new Point(28,5),	new Point(28,6),	new Point(28,7),
					new Point(28,10),	new Point(28,11),	new Point(28,12),	new Point(28,13),
					new Point(28,14),	new Point(28,15),	new Point(28,16),	new Point(28,17),
					new Point(28,18),	new Point(28,19),	new Point(28,20),	new Point(28,21),
					new Point(28,25),	new Point(28,26),	new Point(28,27),	new Point(28,28),
					new Point(28,29),	new Point(28,30)},
		new Point[]{new Point(3,2),		new Point(3,3),		new Point(3,5),		new Point(3,6),
					new Point(3,7),		new Point(3,8),		new Point(3,9),		new Point(3,24),
					new Point(3,25),	new Point(3,26),	new Point(3,27),	new Point(3,29),
					new Point(3,30),	new Point(4,2),		new Point(4,9),		new Point(4,24),
					new Point(4,30),	new Point(5,2),		new Point(5,9),		new Point(5,10),
					new Point(5,11),	new Point(5,12),	new Point(5,19),	new Point(5,20),
					new Point(5,21),	new Point(5,22),	new Point(5,23),	new Point(5,24),
					new Point(5,30),	new Point(6,2),		new Point(6,3),		new Point(6,4),
					new Point(6,5),		new Point(6,6),		new Point(6,9),		new Point(6,12),
					new Point(6,19),	new Point(6,24),	new Point(6,27),	new Point(6,28),
					new Point(6,29),	new Point(6,30),	new Point(7,2),		new Point(7,6),
					new Point(7,9),		new Point(7,12),	new Point(7,19),	new Point(7,24),
					new Point(7,27),	new Point(7,30),	new Point(8,2),		new Point(8,6),
					new Point(8,7),		new Point(8,8),		new Point(8,9),		new Point(8,12),
					new Point(8,13),	new Point(8,14),	new Point(8,15),	new Point(8,16),
					new Point(8,17),	new Point(8,18),	new Point(8,19),	new Point(8,20),
					new Point(8,21),	new Point(8,24),	new Point(8,25),	new Point(8,26),
					new Point(8,27),	new Point(8,30),	new Point(9,2),		new Point(9,6),
					new Point(9,21),	new Point(9,24),	new Point(9,30),	new Point(10,2),
					new Point(10,6),	new Point(10,21),	new Point(10,24),	new Point(10,30),
					new Point(11,2),	new Point(11,3),	new Point(11,4),	new Point(11,5),
					new Point(11,6),	new Point(11,7),	new Point(11,8),	new Point(11,9),
					new Point(11,21),	new Point(11,22),	new Point(11,23),	new Point(11,24),
					new Point(11,25),	new Point(11,26),	new Point(11,27),	new Point(11,30),
					new Point(12,2),	new Point(12,9),	new Point(12,27),	new Point(12,30),
					new Point(13,2),	new Point(13,9),	new Point(13,27),	new Point(13,30),
					new Point(14,2),	new Point(14,5),	new Point(14,6),	new Point(14,7),
					new Point(14,8),	new Point(14,9),	new Point(14,27),	new Point(14,28),
					new Point(14,29),	new Point(14,30),	new Point(15,2),	new Point(15,5),
					new Point(15,27),	new Point(16,2),	new Point(16,5),	new Point(16,27),
					new Point(17,2),	new Point(17,5),	new Point(17,6),	new Point(17,7),
					new Point(17,8),	new Point(17,9),	new Point(17,27),	new Point(17,28),
					new Point(17,29),	new Point(17,30),	new Point(18,2),	new Point(18,9),
					new Point(18,27),	new Point(18,30),	new Point(19,2),	new Point(19,9),
					new Point(19,27),	new Point(19,30),	new Point(20,2),	new Point(20,3),
					new Point(20,4),	new Point(20,5),	new Point(20,6),	new Point(20,7),
					new Point(20,8),	new Point(20,9),	new Point(20,21),	new Point(20,22),
					new Point(20,23),	new Point(20,24),	new Point(20,25),	new Point(20,26),
					new Point(20,27),	new Point(20,30),	new Point(21,2),	new Point(21,6),
					new Point(21,21),	new Point(21,24),	new Point(21,30),	new Point(22,2),
					new Point(22,6),	new Point(22,21),	new Point(22,24),	new Point(22,30),
					new Point(23,2),	new Point(23,6),	new Point(23,7),	new Point(23,8),
					new Point(23,9),	new Point(23,12),	new Point(23,13),	new Point(23,14),
					new Point(23,15),	new Point(23,16),	new Point(23,17),	new Point(23,18),
					new Point(23,19),	new Point(23,20),	new Point(23,21),	new Point(23,24),
					new Point(23,25),	new Point(23,26),	new Point(23,27),	new Point(23,30),
					new Point(24,2),	new Point(24,6),	new Point(24,9),	new Point(24,12),
					new Point(24,19),	new Point(24,24),	new Point(24,27),	new Point(24,30),
					new Point(25,2),	new Point(25,3),	new Point(25,4),	new Point(25,5),
					new Point(25,6),	new Point(25,9),	new Point(25,12),	new Point(25,19),
					new Point(25,24),	new Point(25,27),	new Point(25,28),	new Point(25,29),
					new Point(25,30),	new Point(26,2),	new Point(26,9),	new Point(26,10),
					new Point(26,11),	new Point(26,12),	new Point(26,19),	new Point(26,20),
					new Point(26,21),	new Point(26,22),	new Point(26,23),	new Point(26,24),
					new Point(26,30),	new Point(27,2),	new Point(27,9),	new Point(27,24),
					new Point(27,30),	new Point(28,2),	new Point(28,3),	new Point(28,5),
					new Point(28,6),	new Point(28,7),	new Point(28,8),	new Point(28,9),
					new Point(28,24),	new Point(28,25),	new Point(28,26),	new Point(28,27),
					new Point(28,29),	new Point(28,30)}
	};
	
	public int getCurrentMazeID() {
		return mazeID;
	}

	public int getPillCount() {
		int count = 0;
		Point[] pills = pillPositions[mazeID];
		for(Point p : pills) {
			if(getTile(p).hasPill()) count++;
		}
		pills = powerPillPositions[mazeID];
		for(Point p : pills) {
			if(getTile(p).hasPill()) count++;
		}
		return count;
	}

	public boolean isLegalTilePoint(Point tile) {
		return tile.x >=0 && tile.x < WIDTH && tile.y > 0 && tile.y < HEIGHT;
	}
}
