package prebuild;

import java.awt.Point;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import ai.common.MoveDistance;
import ai.common.Node;
import ai.common.NodeDistance;
import ai.common.MOVE;

/*
 * Maze data is pre-computed here.  You should run this is the Mazes fail to
 * load, or if you make changes to the Node or Maze classes (which you shouldn't
 * be doing).
 */
public class RebuildMazeData {

	private static final int WIDTH = 32, HEIGHT = 32;
	
	private static final Point[][] fakeIntersections = new Point[][] {
		{ new Point(8,2),  new Point(23,2),
		  new Point(3,3),  new Point(28,3), 
		  new Point(3,28), new Point(28,28),
		  new Point(11,2), new Point(20,2) },
		  
		{ new Point(8,2),  new Point(23,2), 
		  new Point(3,5),  new Point(28,5),
		  new Point(3,17), new Point(28,17),
		  new Point(3,27), new Point(28,27),
		  new Point(14,9), new Point(17,9) },
		  
		{ new Point(3,4),   new Point(28,4),
		  new Point(11,2),  new Point(20,2),
		  new Point(3,24),  new Point(28,24),
		  new Point(3,30),  new Point(8,30),
		  new Point(23,30), new Point(28,30),
		  new Point(11,30), new Point(20,30) },
		  
		{ new Point(3,4),   new Point(28,4),
		  new Point(14,5),  new Point(17,5),
		  new Point(14,24), new Point(17,24), 
		  new Point(3,28),  new Point(28,28) }
	};

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		for(int mazeID = 1 ; mazeID < 5 ; mazeID++) {
			try {
				char[][] ram = new char[WIDTH][HEIGHT];
				String data = readFile("res/maze" + mazeID + "_vram.txt", Charset.defaultCharset());
				String[] lines = data.split("\n");
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
				e.printStackTrace();
			}
		}
	}
	
	public static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}

	public static void buildGraph(char[][] ram, int mazeID) {
		Node[][] graph = new Node[WIDTH][HEIGHT];
		System.out.println("Building maze " + mazeID);
		for (int x = 0; x < WIDTH; x++) {
			for (int y = 0; y < HEIGHT; y++) {
				switch (ram[x][y]) {
				case 0x40:
				case 0x10:
				case 0x14:
					graph[x][y] = new Node(x, y, ram[x][y]);
					break;
				default:
					graph[x][y] = null;
				}
			}
		}
		for (int x = 0; x < WIDTH; x++) {
			for (int y = 0; y < HEIGHT; y++) {
				Node node = graph[x][y];
				if (node != null) {
					for (MOVE move : MOVE.values()) {
						Point p = node.getNeighbourPosition(move);
						if (p != null) {
							Node neighbour = graph[p.x][p.y];
							if (neighbour != null) {
								node.setNeighbour(move, neighbour);
							}
						}
						p = null;
					}
				}
			}
		}
		List<Node> junctions = new LinkedList<Node>();
		List<Node> corners = new LinkedList<Node>();
		List<Node> decisions = new LinkedList<Node>();
		
		for (Node[] line : graph) {
			for (Node node : line) {
				if(node != null) {
					node.setIntersection();
					if(node.isIntersection()) {
						junctions.add(node);
						decisions.add(node);
					}
					node.setCorner();
					if(node.isCorner()) {
						corners.add(node);
					}
				}
			}
		}
		
		for(Point fi : fakeIntersections[mazeID-1]) {
			graph[fi.x][fi.y].setPacmanDecisionPoint(true);
			decisions.add(graph[fi.x][fi.y]);
		}

		long start = System.currentTimeMillis();
		System.out.println("Start floyd " + mazeID);
		int[][] dist = floydWarshall(graph);
		System.out.println("Start junctions");
		
		for (Node[] line : graph) {
			for (Node node : line) {
				if(node != null) {
					for(MOVE move : node.getAvailableMoves()) {
						node.blockAllExcept(move);
						
						Node nearestJunction = null;
						int bestDist = 1000000;
						for(Node junction : junctions) {
							if(node != junction) {
								int distance = aStarDistance(node, junction, dist);
								if(distance < bestDist) {
									bestDist = distance;
									nearestJunction = junction;
								}
							}
						}
						node.setNextJunction(move, nearestJunction);
						
						Node nearestCorner = null;
						bestDist = 1000000;
						for(Node corner : corners) {
							if(node != corner) {
								int distance = aStarDistance(node, corner, dist);
								if(distance < bestDist) {
									bestDist = distance;
									nearestCorner = corner;
								}
							}
						}
						node.setNextCorner(move, nearestCorner);
						
						Node dp = null;
						bestDist = 1000000;
						for(Node decision : decisions) {
							if(node != decision) {
								int distance = aStarDistance(node, decision, dist);
								if(distance < bestDist) {
									bestDist = distance;
									dp = decision;
								}
							}
						}
						node.setNextPacmanDecisionPoint(move, dp);
						
						node.unblockAllMoves();
					}
				}
			}
		}
		System.out.println("Calculate distances1");
		Map<Node, Map<Node, MoveDistance>> distances = calculateDistances(graph, dist);
		System.out.println("Calculate distances2");
		Map<Point, Map<Point, int[]>> moveDistances = calculateMoveDistances(graph, dist);
		System.out.println("Saving");
		try {
			OutputStream file = new FileOutputStream("res/moves_" + mazeID	+ ".dat");
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			output.writeObject(distances);
			output.close();

			file = new FileOutputStream("res/graph_" + mazeID + ".dat");
			buffer = new BufferedOutputStream(file);
			output = new ObjectOutputStream(buffer);
			output.writeObject(graph);
			output.close();

			file = new FileOutputStream("res/dist_" + mazeID + ".dat");
			buffer = new BufferedOutputStream(file);
			output = new ObjectOutputStream(buffer);
			output.writeObject(dist);
			output.close();
			
			file = new FileOutputStream("res/dist2_" + mazeID + ".dat");
			buffer = new BufferedOutputStream(file);
			output = new ObjectOutputStream(buffer);
			output.writeObject(moveDistances);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done! " + ((System.currentTimeMillis() - start)));
	}

	private static int[][] floydWarshall(Node[][] graph) {
		int max = 10000;
		int n = WIDTH * HEIGHT;
		int[][] dist = new int[n][n];
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				dist[x][y] = max;
			}
		}
		for (int x = 0; x < WIDTH; x++) {
			for (int y = 0; y < HEIGHT; y++) {
				Node node = graph[x][y];
				if (node != null) {
					dist[x * HEIGHT + y][x * HEIGHT + y] = 0;
					for (Node neighbour : node.getNeighbours()) {
						if (neighbour != null) {
							Point p = neighbour.getPosition();
							dist[p.x * HEIGHT + p.y][x * HEIGHT + y] = 1;
							dist[x * HEIGHT + y][p.x * HEIGHT + p.y] = 1;
						}
					}
				}
			}
		}
		for (int x1 = 0; x1 < WIDTH; x1++) {
			for (int y1 = 0; y1 < HEIGHT; y1++) {
				for (int x2 = 0; x2 < WIDTH; x2++) {
					for (int y2 = 0; y2 < HEIGHT; y2++) {
						for (int x3 = 0; x3 < WIDTH; x3++) {
							for (int y3 = 0; y3 < HEIGHT; y3++) {
								int k = x1 * HEIGHT + y1;
								int i = x2 * HEIGHT + y2;
								int j = x3 * HEIGHT + y3;
								dist[i][j] = Math.min(dist[i][j], dist[i][k] + dist[k][j]);
							}
						}
					}
				}
			}
		}
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				if (dist[x][y] >= max) {
					dist[x][y] = -1;
				}
			}
		}
		return dist;
	}

	private static Map<Node, Map<Node, MoveDistance>> calculateDistances(Node[][] graph, int[][] dist) {
		Map<Node, Map<Node, MoveDistance>> distances = new HashMap<Node, Map<Node, MoveDistance>>();
		for (int x = 0; x < WIDTH; x++) {
			for (int y = 0; y < HEIGHT; y++) {
				Node node = graph[x][y];
				if (node != null) {
					distances.put(node, new HashMap<Node, MoveDistance>());
					for (int x2 = 0; x2 < WIDTH; x2++) {
						for (int y2 = 0; y2 < HEIGHT; y2++) {
							Node node2 = graph[x2][y2];
							if (node2 != null) {
								int d = 100000;
								MOVE dir = null;
								for (MOVE move : node.getAvailableMoves()) {
									node.blockAllExcept(move);
									int asd = aStarDistance(node, node2, dist);
									node.unblockAllMoves();
									if (asd >= 0 && asd < d) {
										d = asd;
										dir = move;
									}
								}
								if (dir != null) {
									distances.get(node).put(node2, new MoveDistance(dir, d));
								}
							}
						}
					}
				}
			}
		}
		return distances;
	}
	
	private static Map<Point, Map<Point, int[]>> calculateMoveDistances(Node[][] graph, int[][] dist) {
		Map<Point, Map<Point, int[]>> distances = new HashMap<Point, Map<Point, int[]>>();
		for (int x = 0; x < WIDTH; x++) {
			for (int y = 0; y < HEIGHT; y++) {
				Node node = graph[x][y];
				if (node != null) {
					distances.put(node.getPosition(), new HashMap<Point, int[]>());
					for (int x2 = 0; x2 < WIDTH; x2++) {
						for (int y2 = 0; y2 < HEIGHT; y2++) {
							Node node2 = graph[x2][y2];
							if (node2 != null) {
								int[] d = new int[]{10000,10000,10000,10000};
								for (MOVE move : node.getAvailableMoves()) {
									node.blockAllExcept(move);
									d[move.ordinal()] = aStarDistance(node, node2, dist);
									node.unblockAllMoves();
								}
								distances.get(node.getPosition()).put(node2.getPosition(), d);
							}
						}
					}
				}
			}
		}
		return distances;
	}

	private static int aStarDistance(Node node, Node node2, int[][] dist) {
		Queue<NodeDistance> agenda = new PriorityQueue<NodeDistance>();
		List<Node> visited = new LinkedList<>();

		if (node != null && node2 != null) {
			agenda.offer(new NodeDistance(node, 0, 0, null));
		}
		while (!agenda.isEmpty()) {
			NodeDistance current = agenda.poll();
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
					int d = distancePath(node2, neighbour, dist);
					if (d >= 0) {
						NodeDistance nd = new NodeDistance(neighbour, current.g + 1, d, current);
						if (!(agenda.contains(nd) || visited.contains(neighbour))) {
							agenda.offer(nd);
						}
					}
				}
			}
			visited.add(current.node);
		}
		return -1;
	}

	private static int distancePath(Point tile1, Point tile2, int[][] dist) {
		return dist[tile1.x * HEIGHT + tile1.y][tile2.x * HEIGHT + tile2.y];
	}

	private static int distancePath(Node n1, Node n2, int[][] dist) {
		return distancePath(n1.getPosition(), n2.getPosition(), dist);
	}
}
