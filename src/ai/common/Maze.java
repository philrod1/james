package ai.common;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.*;

import javax.swing.JPanel;

import ai.common.MOVE;
import ai.common.Game.GHOST;


public class Maze {

	public static final int WIDTH = 32, HEIGHT = 32;
	private Node[][] graph;
	private int[][] dist;
	private Map<Node, Map<Node, MoveDistance>> distances;
	private Map<Point, Map<Point, int[]>> allDistances;
	public final int mazeId;

	@SuppressWarnings("unchecked")
	public Maze (int mazeId) {
		this.mazeId = mazeId;
		try {
			
			InputStream file = new FileInputStream("res/moves_" + mazeId + ".dat");
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream(buffer);
			
			distances = (HashMap<Node, Map<Node, MoveDistance>>) input.readObject();
			input.close();
			
			file = new FileInputStream("res/graph_" + mazeId + ".dat");
			buffer = new BufferedInputStream(file);
			input = new ObjectInputStream(buffer);
			
			graph = (Node[][]) input.readObject();
			input.close();
			
			file = new FileInputStream("res/dist_" + mazeId + ".dat");
			buffer = new BufferedInputStream(file);
			input = new ObjectInputStream(buffer);
			
			dist = (int[][]) input.readObject();
			input.close();
			
			file = new FileInputStream("res/dist2_" + mazeId + ".dat");
			buffer = new BufferedInputStream(file);
			input = new ObjectInputStream(buffer);
			
			allDistances = (Map<Point, Map<Point, int[]>>) input.readObject();
			input.close();
			
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public List<MOVE> getAvailableMoves(Point tile) {
		if (tile.x < WIDTH && tile.y < HEIGHT && tile.x >= 0 && tile.y >=0 && graph != null) {
			List<MOVE> moves = graph[tile.x][tile.y].getAvailableMoves();
			if(moves == null) {
				return new LinkedList<>();
			} else {
				return moves;
			}
		} else {
			return new LinkedList<>();
		}
	}
	
	public int aStarDistance(Node node, Node node2) {
		
		Queue<NodeDistance> agenda = new PriorityQueue<NodeDistance>();
		Set<NodeDistance> agendaSet = new HashSet<>();
		Set<Node> visited = new HashSet<>();
		
		if(node != null && node2 != null) {
			NodeDistance nd = new NodeDistance(node, 0, 0, null);
			agenda.offer(nd);
			agendaSet.add(nd);
		}
		
		while (!agenda.isEmpty()) {
		
			NodeDistance current = agenda.poll();
			agendaSet.remove(current);
			visited.add(current.node);
			
			if (current.node.equals(node2)) {
				int count = 0;
				while (current.parent != null) {
					current = current.parent;
					count++;
				}
				return count;
			}
			
			for (Node neighbour : current.node.getNeighbours()) {
				if (neighbour != null) {
					int d = distancePath(node2, neighbour);
					if(d>=0) {
						NodeDistance nd = new NodeDistance(neighbour, current.g+1, d, current);
						if (!agendaSet.contains(nd) && !visited.contains(neighbour)) {
							agenda.offer(nd);
							agendaSet.add(nd);
						}
					}
				}
			}
			
		}
		return -1;
	}
	
	public int[] getAllDistances(Point p1, Point p2) {
		try {
			return allDistances.get(p1).get(p2);
		} catch (Exception e) {
			return new int[]{10000,10000,10000,10000}; // null?
		}
	}
	
	public Node getTileNeighbour(Point p, MOVE move) {
		Node n = graph[p.x][p.y].getNeighbour(move);
		if(n == null) {
			List<MOVE> moves = getAvailableMoves(p);
			moves.remove(move);
			return getTileNeighbour(p, moves.get(0));
		} else {
			return n;
		}
	}
	
	public Node getNode(Point p) {
		if(p.x < 32 && p.y < 32) {
			return graph[p.x][p.y];
		}
		return null;
	}

	public Point getNearestPill(Point p, Game game) {
		double minDistance = Double.POSITIVE_INFINITY;
		Point nearestPill = null;
		for (Node[] line : graph) {
			for (Node node : line) {
				if (node != null && (node.hasPill(game) || node.hasPowerPill(game))) {
					double distance = distancePath(p, node.getPosition());
					if (distance < minDistance) {
						minDistance = distance;
						nearestPill = node.getPosition();
					}
				}
			}
		}
		return nearestPill;
	}
	
	public int distancePath(Point tile1, Point tile2) {
		try {
			return dist[tile1.x * HEIGHT + tile1.y][tile2.x * HEIGHT + tile2.y];
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int distancePath(Point p, Point t, MOVE move) {
		try {
			Map<Point, int[]> start = allDistances.get(p);
			int[] dest = start.get(t);
			if (dest == null) {
				return 0;
			}
			return dest[move.ordinal()];
		} catch (Exception e) {
			System.out.println(p + " " + move + " ----- " + t + " " + mazeId);
			e.printStackTrace();
			return 100000;
		}
	}
	
	public int distancePath(Node n1, Node n2) {
		return distancePath(n1.getPosition(), n2.getPosition());
	}
	
	public MOVE getMoveTowardsAStar(Node n1, Node n2) {
		if (n1 == null || n2 == null) return null;
		int d = 100000;
		MOVE dir = null;
		for(MOVE move : n1.getAvailableMoves()) {
			List<MOVE> blocked = n1.blockAllExcept(move);
			int asd = aStarDistance(n1, n2);
			for(MOVE m : blocked) {
				n1.unblock(m);
			}
			if(asd >= 0 && asd < d) {
				d = asd;
				dir = move;
			}
		}
		return dir;
	}
	
	public MOVE getMoveTowardsAStar(Point p1, Point p2) {
		return getMoveTowardsAStar(graph[p1.x][p1.y], graph[p2.x][p2.y]);
	}
	
	public MOVE getMoveTowards(Node n1, Node n2) {
		try {
			return distances.get(n1).get(n2).move;
		} catch (NullPointerException e) {
			System.err.println("Null pointer exception (" + n1 + " and " + n2 + ") in maze " + mazeId);
			e.printStackTrace();
			return null;
		}
	}
	
	public MOVE getMoveTowards(Point p1, Point p2) {
		return getMoveTowards(graph[p1.x][p1.y], graph[p2.x][p2.y]);
	}

	public int aStarDistance(Point p1, Point p2) {
		return aStarDistance(graph[p1.x][p1.y], graph[p2.x][p2.y]);
	}

	public Node getNode(int x, int y) {
		return graph[x][y];
	}

	public List<Point> getPillPositions(Game game) {
		List<Point> pills = new LinkedList<Point>();
		for (int x = 0 ; x < WIDTH ; x++) {
			for (int y = 0 ; y < HEIGHT ; y++) {
				Node node = graph[x][y];
				if (node != null && (node.hasPill(game) || node.hasPowerPill(game))) {
					pills.add(node.getPosition());
				}
			}
		}
		return pills;
	}

	public List<Point> getNormalPillPositions(Game game) {
		List<Point> pills = new LinkedList<Point>();
		for (int x = 0 ; x < WIDTH ; x++) {
			for (int y = 0 ; y < HEIGHT ; y++) {
				Node node = graph[x][y];
				if (node != null && node.hasPill(game)) {
					pills.add(node.getPosition());
				}
			}
		}
		return pills;
	}

	public List<Point> getPowerPillPositions(Game game) {
		List<Point> pills = new LinkedList<Point>();
		for (int x = 0 ; x < WIDTH ; x++) {
			for (int y = 0 ; y < HEIGHT ; y++) {
				Node node = graph[x][y];
				if (node != null && node.hasPowerPill(game)) {
					pills.add(node.getPosition());
				}
			}
		}
		return pills;
	}

	public boolean isIntersection(Point tile) {
		try {
			Node node = graph[tile.x][tile.y];
			if(node != null) {
				return node.isIntersection();
			}
		} catch (Exception e) {
			
		}
		return false;
	}

	public boolean isCorner(Point tile) {
		Node node = graph[tile.x][tile.y];
		if (node != null) {
			return node.isCorner();
		}
		return false;
	}

	public Node getNextNode(Node node, Node target) {
		MOVE move = getMoveTowards(node, target);
		return node.getNeighbour(move);
	}

	public Point getNextJunction(Point origin, MOVE move) {
		Node node = getNode(origin);
		if(node == null) {
			return null;
		}
		Node next = node.getNextJunction(move);
		if(next == null) {
			System.err.println("No junction for " + origin + " going " + move);
			return origin;  // null?
		}
		return next.getPosition();
	
	}

	public Point getNextCorner(Point origin, MOVE move) {
		return getNode(origin).getNextCorner(move).getPosition();
	}
	
	public Point getNextCornerOrJunction(Point origin, MOVE move) {
		Point corner = null;
		Point junction = null;
		try {
			corner =  getNode(origin).getNextCorner(move).getPosition();
			junction = getNode(origin).getNextJunction(move).getPosition();
			int dc = distancePath(origin, corner);
			if(distancePath(origin, junction) < dc) {
				return junction;
			} else {
				return corner;
			}
		} catch (NullPointerException e) {
			if (corner == null) {
				if(junction == null) {
					return origin;
				} else {
					return junction;
				}
			} else {
				return corner;
			}
		}
	}

	public boolean pathContainsPowerPill(Point p, Point t, Game game) {
		Node target = getNode(t);
		if(getNode(p).hasPowerPill(game) || target.hasPowerPill(game)) {
			return true;
		}
		while(!p.equals(t)) {
			p = getNextNode(getNode(p), target).getPosition();
			if(getNode(p).hasPowerPill(game)) {
				return true;
			}
		}
		return false;
	}

	public Point getNextTile(Point p, MOVE move) {
		return getNode(p).getNeighbourPosition(move);
	}

	public Point getNextDecisionPoint(Point origin, MOVE move) {
		Node node = getNode(origin);
		if(node == null) {
			System.out.println("Maze.getNextDecisionPoint() - bad node");
			return origin; // null?
		}
		Node next = node.getNextPacmanDecisionPoint(move);
		if(next == null) {
			System.err.println("No decision point for pacman at " + node + " going " + move);
			List<MOVE> moves = getAvailableMoves(origin);
			moves.remove(move.opposite());
			return getNextDecisionPoint(origin, moves.get(0));
		}
		return next.getPosition();
	}

	public boolean isValidTile(Point tilePosition) {
		try {
			Node n = graph[tilePosition.x][tilePosition.y];
			return n != null;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				Node t = graph[x][y];
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

}
