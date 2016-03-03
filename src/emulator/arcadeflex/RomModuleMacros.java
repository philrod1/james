package emulator.arcadeflex;

import java.util.ArrayList;

public class RomModuleMacros {

	public static RomModule[] rommodule_macro = null;
	/* start of memory region */

	private static ArrayList<RomModule> arload = new ArrayList<RomModule>();

	public static void ROM_REGION(int offset) {
		arload.add(new RomModule(null, offset, 0));
	}

	/* ROM to load */
	public static void ROM_LOAD(String name, int offset, int size, int crc) {
		arload.add(new RomModule(name, offset, size, crc));
	}

	/* continue loading the previous ROM to a new address */
	public static void ROM_CONTINUE(int offset, int length) {
		arload.add(new RomModule(null, offset, length));
	}

	/* restart loading the previous ROM to a new address */
	public static void ROM_RELOAD(int offset, int length) {
		arload.add(new RomModule("-1", offset, length));
	}

	/* load the ROM at even/odd addresses. Useful with 16 bit games */
	public static void ROM_LOAD_EVEN(String name, int offset, int length, int checksum) {
		arload.add(new RomModule(name, offset & ~1, length | 0x80000000,
				checksum));
	}

	public static void ROM_LOAD_ODD(String name, int offset, int length, int checksum) {
		arload.add(new RomModule(name, offset | 1, length | 0x80000000, checksum));
	}

	/* end of table */
	public static void ROM_END() {
		arload.add(new RomModule(null, 0, 0));
		rommodule_macro = arload.toArray(new RomModule[arload.size()]);
		arload.clear();
	}

}
