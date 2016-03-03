package emulator.video;

public class Rectangle {

	public int min_x, max_x;
	public int min_y, max_y;
	
	public Rectangle() {}

	public Rectangle(int minx, int maxx, int miny, int maxy) {
		min_x = minx;
		max_x = maxx;
		min_y = miny;
		max_y = maxy;
	}
	
	@Override
	public String toString() {
		return "Rectangle (" + min_x + "," + min_y + ") (" + max_x + "," + max_y +")";
	}
}