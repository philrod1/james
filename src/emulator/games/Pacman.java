package emulator.games;

import static emulator.arcadeflex.RomModuleMacros.ROM_END;
import static emulator.arcadeflex.RomModuleMacros.ROM_LOAD;
import static emulator.arcadeflex.RomModuleMacros.ROM_REGION;
import emulator.arcadeflex.FileStuff;
import emulator.arcadeflex.RomModuleMacros;
import emulator.memory.CharPtr;
import emulator.memory.MemoryMappedIO;
import emulator.memory.Pointers.RomLoadPtr;
import emulator.video.GenericVideo;
import emulator.video.GfxDecodeInfo;
import emulator.video.GfxLayout;
import emulator.video.Pengo;
import emulator.video.Rectangle;

public class Pacman {

	private final char[] RAM;
	private final MemoryMappedIO[] io;
	private final CharPtr videoram;
	private final CharPtr colourram;
	private final CharPtr[] spriteram;
	private final char[][] memoryRegions;

	public Pacman() {

		romLoader.handler();
		memoryRegions = FileStuff.readroms(
				RomModuleMacros.rommodule_macro, "roms/mspacman", 2);

		RAM = memoryRegions[0];
		videoram  = new CharPtr(RAM, 0x4000);
		colourram = new CharPtr(RAM, 0x4400);
		spriteram = new CharPtr[]{ new CharPtr(RAM, 0x4ff0), new CharPtr(RAM, 0x5060) };
		
		MemoryMappedIO in0 = new MemoryMappedIO(0x5000, (char) 0xFF);
		MemoryMappedIO in1 = new MemoryMappedIO(0x5040, (char) 0xFF);
		MemoryMappedIO dsw = new MemoryMappedIO(0x5080, (char) 0b11001000);
		io = new MemoryMappedIO[]{in0,in1,dsw};
		
	}


	private final GfxLayout charLayout = new GfxLayout(
			8, 8, 256, 2,
			new int[] {0, 4 }, 
			new int[] { 56, 48, 40, 32, 24, 16, 8, 0 },
			new int[] { 64, 65, 66, 67,  0,  1, 2, 3 }, 128);

	private final GfxLayout spriteLayout = new GfxLayout(
			16, 16, 64, 2,
			new int[] { 0, 4 }, /*
								 * the two bitplanes for 4 pixels are packed
								 * into one byte
								 */
			new int[] { 312, 304, 296, 288, 280, 272, 264, 256,
						 56,  48,  40,  32,  24,  16,   8,   0 },
			new int[] {  64,  65,  66,  67, 128, 129, 130, 131,
						192, 193, 194, 195,   0,   1,   2,   3 }, 
			512);

	private final GfxDecodeInfo[] gfxDecodeInfo = {
			new GfxDecodeInfo(1, 0x0000, charLayout, 0, 32),
			new GfxDecodeInfo(1, 0x1000, spriteLayout, 0, 32),
			new GfxDecodeInfo(-1) /* end of array */
	};

	private final char[] colourData = {
			/* palette */
			0x00, 0x07, 0x66, 0xEF,
			0x00, 0xF8, 0xEA, 0x6F,
			0x00, 0x3F, 0x00, 0xC9,
			0x38, 0xAA, 0xAF, 0xF6,
			/* colour lookup table */
			0x00, 0x00, 0x00, 0x00, 0x00, 0x0F, 0x0B, 0x01,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x0F, 0x0B, 0x03,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x0F,	0x0B, 0x05,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x0F, 0x0B, 0x07,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x0B, 0x01, 0x09,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x0F, 0x00, 0x0E, 0x00, 0x01, 0x0C, 0x0F,
			0x00, 0x0E, 0x00, 0x0B, 0x00, 0x0C, 0x0B, 0x0E,
			0x00, 0x0C, 0x0F, 0x01, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x01, 0x02, 0x0F, 0x00, 0x07, 0x0C, 0x02,
			0x00, 0x09, 0x06, 0x0F, 0x00, 0x0D, 0x0C, 0x0F,
			0x00, 0x05, 0x03, 0x09, 0x00, 0x0F, 0x0B, 0x00,
			0x00, 0x0E, 0x00, 0x0B, 0x00, 0x0E,	0x00, 0x0B,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x0F, 0x0E, 0x01,
			0x00, 0x0F, 0x0B, 0x0E, 0x00, 0x0E, 0x00, 0x0F };

	/***************************************************************************
	 * Game driver
	 ***************************************************************************/

	private final RomLoadPtr romLoader = new RomLoadPtr() {

		public void handler() {
			ROM_REGION(0x10000); /* 64k for code */
			ROM_LOAD("boot1", 0x0000, 0x1000, 0xd16b31b7);
			ROM_LOAD("boot2", 0x1000, 0x1000, 0x0d32de5e);
			ROM_LOAD("boot3", 0x2000, 0x1000, 0x1821ee0b);
			ROM_LOAD("boot4", 0x3000, 0x1000, 0x165a9dd8);
			ROM_LOAD("boot5", 0x8000, 0x1000, 0x8c3e6de6);
			ROM_LOAD("boot6", 0x9000, 0x1000, 0x368cb165);

			ROM_REGION(0x2000); /*
								 * temporary space for graphics (disposed after
								 * conversion)
								 */
			ROM_LOAD("5e", 0x0000, 0x1000, 0x5c281d01);
			ROM_LOAD("5f", 0x1000, 0x1000, 0x615af909);
			ROM_END();
		}
	};

	public int getClock() {
		return 3072000;
	}

	public int getFPS() {
		return 60;
	}

	public GenericVideo getVideo() {
		Pengo pengo = new Pengo(28 * 8, 36 * 8, false, 1, new Rectangle(0 * 8,
				28 * 8 - 1, 0 * 8, 36 * 8 - 1), colourData, 16,
				gfxDecodeInfo, videoram, new int[]{1024}, colourram,
				spriteram, new int[]{16,0,0});
		pengo.init(memoryRegions);
		return pengo;
	}

	public char[] getMemory() {
		return RAM;
	}

	public MemoryMappedIO[] getIO() {
		return io;
	}
}
