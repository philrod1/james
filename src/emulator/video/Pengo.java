package emulator.video;

import emulator.memory.CharPtr;
import emulator.memory.Pointers.VhConvertColourPromPtr;
import emulator.memory.Pointers.WriteHandlerPtr;

public class Pengo extends GenericVideo {

	private int gfxBank;
	private boolean flipScreen;
	private int xOffset;
	private final Rectangle visibleArea;
	
	private final CharPtr videoRam;
	private final int[] videoRamSize;
	private final CharPtr colourRam;
	private final CharPtr[] spriteRam;
	private final int[] spriteRamSize;
	
	public Screen getScreen() {
		return screen;
	}
	
	public WriteHandlerPtr pengoFlipscreen = new WriteHandlerPtr() {
		public void handler(int offset, int data) {
			flipScreen = (data & 1) == 1;
		}
	};

	public Pengo (int width, int height, boolean flipScreen, 
			int xOffset, Rectangle visibleArea, char[] colourRom, 
			int totalColors, GfxDecodeInfo[] gfxDecodeInfo, CharPtr videoRam,
			int[] videoRamSize, CharPtr colourRam, CharPtr[] spriteRam, int[] spriteRamSize) {
		super(width, height, GenericVideo.ORIENTATION_DEFAULT, colourRom, totalColors, gfxDecodeInfo);
		gfxBank = 0;
		this.flipScreen = flipScreen;
		this.xOffset = xOffset;
		this.visibleArea = visibleArea;
		this.videoRam = videoRam;
		this.videoRamSize = videoRamSize;
		this.colourRam = colourRam;
		this.spriteRam = spriteRam;
		this.spriteRamSize = spriteRamSize;
		super.setConvertColorProm(pengoConvertColourRom);
	}

	public static final Rectangle SPRITE_VISIBLE_AREA =
			new Rectangle(0 * 8, 28 * 8 - 1, 2 * 8, 34 * 8 - 1);


	public int totalColours(int gfxn) {
		return gfx[gfxn].totalColours * gfx[gfxn].colourGranularity;
	}

	public int colour(char[] colortable, int gfxn, int offset) {
		return colortable[gfxDecodeInfo[gfxn].colourCodesStart + offset];
	}

	public VhConvertColourPromPtr pengoConvertColourRom = new VhConvertColourPromPtr() {

		public void handler(char[] paletteTable, char[] colourTable, char[] colourRomTable) {
			int i;
			CharPtr colour = new CharPtr(colourRomTable);
			CharPtr palette = new CharPtr(paletteTable);
			for (i = 0; i < totalColours; i++) {
				int bit0, bit1, bit2;

				/* red component */
				bit0 = (colour.read() >> 0) & 1;
				bit1 = (colour.read() >> 1) & 1;
				bit2 = (colour.read() >> 2) & 1;
				int red = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
				palette.writeInc(red);

				/* green component */
				bit0 = (colour.read() >> 3) & 1;
				bit1 = (colour.read() >> 4) & 1;
				bit2 = (colour.read() >> 5) & 1;
				int green = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
				palette.writeInc(green);
				/* blue component */
				bit0 = 0;
				bit1 = (colour.read() >> 6) & 1;
				bit2 = (colour.read() >> 7) & 1;
				int blue = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
				palette.writeInc(blue);

				colour.inc();
			}
			/* color_prom now points to the beginning of the lookup table */
			/* character lookup table */
			/* sprites use the same color lookup table as characters */
			for (i = 0; i < totalColours(0); i++)
				colourTable[gfxDecodeInfo[0].colourCodesStart + i] = 
					colour.readInc();

			if (gfx[2] != null) {
				for (i = 0; i < totalColours(2); i++) {

					if (colour.read() != 0) {
						colourTable[gfxDecodeInfo[2].colourCodesStart + i] = 
								(char) (colour.read() + 0x10);
					} else {
						colourTable[gfxDecodeInfo[2].colourCodesStart + i] = 0;
					}
					colour.inc();
				}
			}
		}
	};


	public void pengoFlipscreen (boolean flipscreen) {
		this.flipScreen = flipscreen;
	}
	
	public void pengo_gfxbank_w (int data) {
		gfxBank = data & 1;
	}

	/***************************************************************************
	 * Draw the game screen in the given osd_bitmap. Do NOT call
	 * osd_update_display() from this function, it will be called by the main
	 * emulation engine.
	 ***************************************************************************/
	public void screenRefresh () {
			int offs;

			/*
			 * for every character in the Video RAM, check if it has been
			 * modified since last time and update it accordingly.
			 */
			for (offs = 0; offs < videoRamSize[0]; offs++) {
				int sx, sy, mx, my;

				mx = offs / 32;
				my = offs % 32;

				if (mx <= 1) {
					sx = 29 - my;
					sy = mx + 34;
				} else if (mx >= 30) {
					sx = 29 - my;
					sy = mx - 30;
				} else {
					sx = 29 - mx;
					sy = my + 2;
				}
				if (flipScreen) {
					sx = 27 - sx;
					sy = 35 - sy;
				}

				drawgfx(
						gfx[2 * gfxBank],
						videoRam.read(offs), 
						colourRam.read(offs),
						flipScreen, 
						flipScreen, 
						8 * sx, 
						8 * sy,
						visibleArea, 
						TRANSPARENCY_NONE, 
						0
					);
			}

			/*
			 * Draw the sprites. Note that it is important to draw them exactly
			 * in this order, to have the correct priorities.
			 * sprites #0 and #7 are not used */
			for (offs = spriteRamSize[0] - 2; offs > 2 * 2; offs -= 2) {
				drawgfx(
						gfx[1 + 2 * gfxBank],
						spriteRam[0].read(offs) >> 2, 
						spriteRam[0].read(offs + 1),
						(spriteRam[0].read(offs) & 2) > 0, 
						(spriteRam[0].read(offs) & 1) > 0,
						239 - spriteRam[1].read(offs),
						272 - spriteRam[1].read(offs + 1), 
						SPRITE_VISIBLE_AREA,
						TRANSPARENCY_COLOR, 
						0
					);
			}
			/*
			 * In the Pac Man based games (NOT Pengo) the first two sprites must
			 * be offset one pixel to the left to get a more correct placement
			 */
			for (offs = 2 * 2; offs >= 0; offs -= 2) {
				drawgfx(gfx[1 + 2 * gfxBank],
						spriteRam[0].read(offs) >> 2, 
						spriteRam[0].read(offs + 1),
						(spriteRam[0].read(offs) & 2) > 0, 
						(spriteRam[0].read(offs) & 1) > 0, 
						239 - xOffset - spriteRam[1].read(offs),
						272 - spriteRam[1].read(offs + 1), 
						SPRITE_VISIBLE_AREA,
						TRANSPARENCY_COLOR, 0);
			}
		
	}

	public CharPtr getSpriteram(int bank) {
		return spriteRam[bank];
	}

	public int[] getSpriteram_size() {
		return spriteRamSize;
	}

	public void saveScreenShot() {
		screen.saveScreenShot();
	}
}