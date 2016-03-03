package emulator.video;


public class GfxLayout {

	public final int width, height; /* width and height of chars/sprites 					*/
	public final int total; 		/* total number of chars/sprites in the ROM				*/
	public final int planes; 		/* number of bitplanes 									*/
	public final int[] planeOffset; /* start of every bitplane 								*/
	public final int[] xOffset; 	/* coordinates of the bit corresponding to the pixel 	*/
	public final int[] yOffset; 	/* of the given coordinates 							*/
	public final int charIncrement; /* distance between two consecutive characters/sprites 	*/
	
	public GfxLayout(int width, int height, 
			int total, int planes, 
			int[] planeOffset, int[] xOffset, 
			int[] yOffset, int charIncrement) {
		this.width = width;
		this.height = height;
		this.total = total;
		this.planes = planes;
		this.planeOffset = planeOffset;
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		this.charIncrement = charIncrement;
	}

}
