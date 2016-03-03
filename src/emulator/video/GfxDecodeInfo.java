package emulator.video;

import java.io.Serializable;

public class GfxDecodeInfo implements Serializable {

	private static final long serialVersionUID = -920320820157305281L;
	public int memoryRegion;     // memory region where the data resides (usually 1) -1 marks the end of the array
	public int start;            // beginning of data data to decode (offset in RAM[])
	public GfxLayout gfxLayout;
	public int colourCodesStart; // offset in the colour lookup table where colour codes start
	public int totalColourCodes; // total number of colour codes
	
	public GfxDecodeInfo(int memoryRegion, int start, GfxLayout gfxLayout, int colourCodesStart, int totalColourCodes) {
		this.memoryRegion     = memoryRegion;
		this.start            = start;
		this.gfxLayout        = gfxLayout;
		this.colourCodesStart = colourCodesStart;
		this.totalColourCodes = totalColourCodes;
	}

	public GfxDecodeInfo(int start, GfxLayout gfxLayout, int colourCodesStart, int totalColourCodes) {
		this.start = start;
		this.gfxLayout = gfxLayout;
		this.colourCodesStart = colourCodesStart;
		this.totalColourCodes = totalColourCodes;
	}

	public GfxDecodeInfo(int start) {
		this(start, start, null, 0, 0);
	}
}