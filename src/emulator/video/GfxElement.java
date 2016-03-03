package emulator.video;

import java.io.Serializable;

import emulator.memory.CharPtr;

public class GfxElement implements Serializable {

	private static final long serialVersionUID = 4550980839138384798L;
	public int width, height;
	public Bitmap bitmap; /* graphic data */
	public int totalElements; /* total number of characters/sprites */
	public int colourGranularity; 	/* number of colours for each colour code */
	public CharPtr colourTable; /* map colour codes to screen pens */
								/* if this is 0, the function does a verbatim copy */
	public int totalColours;
	
	public GfxElement(int width, int height, Bitmap bitmap, 
			int totalElements, int colourGranularity, 
			CharPtr colourTable, int totalColours) {
		this.width = width;
		this.height = height;
		this.bitmap = bitmap;
		this.totalElements = totalElements;
		this.colourGranularity = colourGranularity;
		this.colourTable = colourTable;
		this.totalColours = totalColours;
	}

	public GfxElement(int width, int height, int totalElements, int colourGranularity, int totalColours) {
		this(width, height, null, totalElements, colourGranularity, null, totalColours);
	}

	public GfxElement() {
	};


}