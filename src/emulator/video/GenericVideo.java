/*
This file is part of Arcadeflex.

Arcadeflex is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Arcadeflex is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Arcadeflex.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * ported to v0.28
 * ported to v0.27
 *
 *
 *
 */

package emulator.video;

import emulator.memory.CharPtr;
import emulator.memory.Pointers.VhConvertColourPromPtr;

public abstract class GenericVideo {

	public static final int TRANSPARENCY_NONE = 0;
	public static final int TRANSPARENCY_PEN = 1;
	public static final int TRANSPARENCY_COLOR = 2;
    public static final int TRANSPARENCY_THROUGH = 3;
    
    public static final int ORIENTATION_DEFAULT 	= 0;
    public static final int ORIENTATION_FLIP_X 		= 1;	/* mirror everything in the X direction */
    public static final int ORIENTATION_FLIP_Y 		= 2;	/* mirror everything in the Y direction */
    public static final int ORIENTATION_SWAP_XY    	= 4;	/* mirror along the top-left/bottom-rigth diagonal */
    public static final int ORIENTATION_ROTATE_90  	= ORIENTATION_SWAP_XY | ORIENTATION_FLIP_X;	/* rotate clockwise 90 degrees */
    public static final int ORIENTATION_ROTATE_180 	= ORIENTATION_FLIP_X  | ORIENTATION_FLIP_Y;	/* rotate 180 degrees */
    public static final int ORIENTATION_ROTATE_270 	= ORIENTATION_SWAP_XY | ORIENTATION_FLIP_Y;	/* rotate counter-clockwise 90 degrees */
    /* IMPORTANT: to perform more than one transformation, DO NOT USE |, use ^ instead. */
    /* For example, to rotate 90 degrees counterclockwise and flip horizontally, use: */
    /* ORIENTATION_ROTATE_270 ^ ORIENTATION_FLIP_X */
    /* FLIP is performed *after* SWAP_XY. */
    
    /* bit 0 of the video attributes indicates raster or vector video hardware */
    public static final int VIDEO_TYPE_RASTER = 0;
    public static final int VIDEO_TYPE_VECTOR = 1;

    /* bit 1 of the video attributes indicates whether or not dirty rectangles will work */
    public static final int VIDEO_SUPPORTS_DIRTY = 2;

    /* bit 2 of the video attributes indicates whether or not the driver modifies the palette */
    public static final int VIDEO_MODIFIES_PALETTE = 4;
    
    public static final int MAX_COLOR_TUPLE = 16;
	public static final int MAX_COLOR_CODES = 256;
	public static final int MAX_PENS = 256;
	public static final int MAX_GFX_ELEMENTS = 10;
	public static final int MAX_MEMORY_REGIONS = 10;

	protected GfxElement[] gfx;
	protected GfxDecodeInfo[] gfxDecodeInfo;
	protected int totalColours;
	private final char[] remappedTable = new char[MAX_COLOR_TUPLE*MAX_COLOR_CODES];

	private VhConvertColourPromPtr convertColourRom;
	
	public char[] colourRom;
	private int[] palette = new int[256];
	private char[] cPalette = new char[256];
	public char[] colourTable;
	
	
	//TODO - make private
	protected Screen screen;
	
	private int orientation;
	
    private char[] pens = new char[256];
	private int width;
	private int height;
	private int firstFreePen = 0;

	public GenericVideo (int width, int height, int orientation, char[] color_prom,
			int totalColors, GfxDecodeInfo[] gfxdecodeinfo) {
		this.gfxDecodeInfo = gfxdecodeinfo;
		this.totalColours = totalColors;
		this.colourRom = color_prom;
		this.width = width;
		this.height = height;
		this.orientation = orientation;
	}
	
	public int init(char[][] memoryRegion) {
		int i;
		char convPalette[] = new char[3 * MAX_PENS];
		char convTable[] = new char[MAX_COLOR_TUPLE * MAX_COLOR_CODES];
		gfx = new GfxElement[MAX_GFX_ELEMENTS];
		/*
		 * convert the gfx ROMs into character sets. This is done BEFORE calling
		 * the driver's convert_color_prom() routine because it might need to check the
		 * Machine->gfx[] data
		 */
		if (gfxDecodeInfo != null) {
			for (i = 0; i < MAX_GFX_ELEMENTS && gfxDecodeInfo[i].memoryRegion != -1; i++) {
				gfx[i] = decodeGfx(
						new CharPtr(
								memoryRegion[gfxDecodeInfo[i].memoryRegion],
								gfxDecodeInfo[i].start),
								gfxDecodeInfo[i].gfxLayout);
				
				gfx[i].colourTable = new CharPtr(remappedTable, gfxDecodeInfo[i].colourCodesStart);
				gfx[i].totalColours = gfxDecodeInfo[i].totalColourCodes;
			}
		}
		/* convert the palette */
		if(convertColourRom != null) {
			convertColourRom.handler(convPalette, convTable, colourRom);
			cPalette = convPalette;
			colourTable = convTable;
		}

		createDisplay();

		for (i = 0; i < totalColours; i++) {
			pens[i] = (char) obtainPen(cPalette[3 * i],
					cPalette[3 * i + 1], cPalette[3 * i + 2]);
		}

		for (i = 0; i < 128; i++) {
			remappedTable[i] = pens[colourTable[i]];
		}

		/* free the graphics ROMs, they are no longer needed */
		memoryRegion[1] = null;
		return 0;
	}

	private int obtainPen(int red, int green, int blue) {
		int res = firstFreePen ;
		paletteSetColour(res, red, green, blue);
		firstFreePen = (firstFreePen + 3) % 256;
		return res;
	}

	private void paletteSetColour(int c, int r, int g, int b) {
		int rgb = r << 16 | g << 8 | b;
		palette[c] = rgb;
	}

	private void createDisplay() {
		screen = new BufferedImageScreen(width, height);
		if ((orientation & ORIENTATION_SWAP_XY) != 0) {
			width  ^= height;
			height ^= width;
			width  ^= height;
		}
	}

	private Bitmap createBitmap(int width, int height, boolean flipXY) {

		if (flipXY) {
			width  ^= height;
			height ^= width;
			width  ^= height;
		}

		Bitmap bitmap = new Bitmap();
		bitmap.width = width;
		bitmap.height = height;

		bitmap.line = new char[height][];
		for (int i = 0; i < height; i++) {
			bitmap.line[i] = new char[width];
		}

		return bitmap;
	}
	
	/***************************************************************************
	 * Draw graphic elements in the specified bitmap.
	 ***************************************************************************/
	public void drawgfx(GfxElement gfx, int code,
			int color, boolean flipX, boolean flipY, int sx, int sy, Rectangle clip,
			int transparency, int transparent_color) {
		int ox, oy, ex, ey, x, y, start, dy, col;
		Rectangle myclip = new Rectangle();
		if (gfx == null) {
			return;
		}
		if ((orientation & ORIENTATION_SWAP_XY) != 0) {
			boolean tempFlip;
			int temp;

			temp = sx;
			sx = sy;
			sy = temp;

			tempFlip = flipX;
			flipX = flipY;
			flipY = tempFlip;

			if (clip != null) {
				myclip.min_x = clip.min_y;
				myclip.max_x = clip.max_y;
				myclip.min_y = clip.min_x;
				myclip.max_y = clip.max_x;
				clip = myclip;
			}
		}
		if ((orientation & ORIENTATION_FLIP_X) != 0) {
			sx = width - gfx.width - sx;
			if (clip != null) {
				int temp;

				temp = clip.min_x;
				myclip.min_x = width - 1 - clip.max_x;
				myclip.max_x = width - 1 - temp;
				myclip.min_y = clip.min_y;
				myclip.max_y = clip.max_y;
				clip = myclip;
			}
		}
		if ((orientation & ORIENTATION_FLIP_Y) != 0) {

			sy = height - gfx.height - sy;
			if (clip != null) {
				int temp;
				myclip.min_x = clip.min_x;
				myclip.max_x = clip.max_x;
				temp = clip.min_y;
				myclip.min_y = height - 1 - clip.max_y;
				myclip.max_y = height - 1 - temp;
				clip = myclip;
			}
		}
		/* check bounds */
		ox = sx;
		oy = sy;
		ex = sx + gfx.width - 1;
		if (sx < 0) {
			sx = 0;
		}
		if (clip != null && sx < clip.min_x) {
			sx = clip.min_x;
		}
		if (ex >= width) {
			ex = width - 1;
		}
		if (clip != null && ex > clip.max_x) {
			ex = clip.max_x;
		}
		if (sx > ex) {
			return;
		}
		ey = sy + gfx.height - 1;
		if (sy < 0) {
			sy = 0;
		}
		if (clip != null && sy < clip.min_y) {
			sy = clip.min_y;
		}
		if (ey >= height) {
			ey = height - 1;
		}
		if (clip != null && ey > clip.max_y) {
			ey = clip.max_y;
		}
		if (sy > ey) {
			return;
		}

		if (flipY) {
			start = (code % gfx.totalElements) 
					* gfx.height + gfx.height - 1 - (sy - oy);
			dy = -1;
		} else /* normal */{
			start = (code % gfx.totalElements) * gfx.height + (sy - oy);
			dy = 1;
		}

		/* if necessary, remap the transparent color */
		if (transparency == TRANSPARENCY_COLOR
				|| transparency == TRANSPARENCY_THROUGH) {
			transparent_color = pens[transparent_color];
		}

		if (gfx.colourTable != null) {
			CharPtr paldata = new CharPtr(gfx.colourTable,
					gfx.colourGranularity * (color % gfx.totalColours));
			
			switch (transparency) {
			case TRANSPARENCY_NONE:
				for (y = sy; y <= ey; y++) {
					for (x = sx; x <= ex - 7; x += 8) {
						screen.setPixel(x,   y, palette[paldata.read(gfx.bitmap.line[start][0])]);
						screen.setPixel(x+1, y, palette[paldata.read(gfx.bitmap.line[start][1])]);
						screen.setPixel(x+2, y, palette[paldata.read(gfx.bitmap.line[start][2])]);
						screen.setPixel(x+3, y, palette[paldata.read(gfx.bitmap.line[start][3])]);
						screen.setPixel(x+4, y, palette[paldata.read(gfx.bitmap.line[start][4])]);
						screen.setPixel(x+5, y, palette[paldata.read(gfx.bitmap.line[start][5])]);
						screen.setPixel(x+6, y, palette[paldata.read(gfx.bitmap.line[start][6])]);
						screen.setPixel(x+7, y, palette[paldata.read(gfx.bitmap.line[start][7])]);
					}
					start += dy;
				}
				break;

			case TRANSPARENCY_PEN:
				break;

			case TRANSPARENCY_COLOR:
				if (flipX) {
					for (y = sy; y <= ey; y++) {
						int j = 15 + (ox - sx);
						for (x = sx ; x <= ex ; x++) {
							col = paldata.read(gfx.bitmap.line[start][j--]);
							if (col != transparent_color) {
								screen.setPixel(x, y, palette[col]);
							}
						}
						start += dy;
					}
				} else {
					for (y = sy; y <= ey; y++) {
						int j = -(ox - sx);
						for (x = sx ; x <= ex ; x++) {
							col = paldata.read(gfx.bitmap.line[start][j++]);
							if (col != transparent_color) {
								screen.setPixel(x, y, palette[col]);
							}
						}
						start += dy;
					}
				}
				break;

			case TRANSPARENCY_THROUGH:
				break;
			}
		}
	}


	public void updateDisplay() {
		screen.blit();
	}
	
	private GfxElement decodeGfx(CharPtr src, GfxLayout gl) {

		int c;
		Bitmap bm;
		GfxElement gfx;

		gfx = new GfxElement();

		if ((orientation & ORIENTATION_SWAP_XY) != 0) {
			gfx.width = gl.height;
			gfx.height = gl.width;
			bm = createBitmap(gl.total * gfx.height, gfx.width, true);
		} else {
			gfx.width = gl.width;
			gfx.height = gl.height;
			bm = createBitmap(gfx.width, gl.total * gfx.height, false);
		}

		gfx.totalElements = gl.total;
		gfx.colourGranularity = 1 << gl.planes;
		gfx.bitmap = bm;

		for (c = 0; c < gl.total; c++)
			decodeChar(gfx, c, src, gl);

		return gfx;
	}
	
	private void decodeChar(GfxElement gfx, int num, CharPtr src, GfxLayout gl) {
		int plane;

		for (plane = 0; plane < gl.planes; plane++) {
			int offs, y;

			offs = num * gl.charIncrement + gl.planeOffset[plane];

			for (y = 0; y < gfx.height; y++) {
				int x;
				char[] dp;

				dp = gfx.bitmap.line[num * gfx.height + y];

				for (x = 0; x < gfx.width; x++) {
					int xoffs, yoffs;

					if (plane == 0)
						dp[x] = 0;
					else
						dp[x] <<= 1;

					xoffs = x;
					yoffs = y;

					if ((orientation & ORIENTATION_FLIP_X) != 0)
						xoffs = gfx.width - 1 - xoffs;

					if ((orientation & ORIENTATION_FLIP_Y) != 0)
						yoffs = gfx.height - 1 - yoffs;

					if ((orientation & ORIENTATION_SWAP_XY) != 0) {
						int temp;

						temp = xoffs;
						xoffs = yoffs;
						yoffs = temp;
					}

					dp[x] += readbit(src, offs + gl.yOffset[yoffs] + gl.xOffset[xoffs]);
				}
			}
		}
	}
	
	private int readbit(CharPtr src, int bitnum) {
		return (src.read(bitnum / 8) >> (7 - bitnum % 8)) & 1;
	}

	protected void setConvertColorProm(VhConvertColourPromPtr convertColourRom) {
		this.convertColourRom = convertColourRom;
	}
	
	public Screen getScreen() {
		return screen;
	}
	
	public abstract void screenRefresh();
	
}