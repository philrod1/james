package emulator.memory;

import emulator.video.Bitmap;


public class Pointers {
	
	public static abstract interface ReadHandlerPtr {
		public abstract int handler(int offset);
	}

	public static abstract interface WriteHandlerPtr {
		public abstract void handler(int offset, int data);
	}

	public static abstract interface InitMachinePtr {
		public abstract void handler();
	}

	public static abstract interface InterruptPtr {
		public abstract int handler();
	}

	public static abstract interface VhConvertColourPromPtr {
		public abstract void handler(char[] palette, char[] colortable, char[] color_prom);
	}

	public static abstract interface VhInitPtr {
		public abstract int handler(String gamename);
	}

	public static abstract interface VhStartPtr {
		public abstract int handler();
	}

	public static abstract interface VhStopPtr {
		public abstract void handler();
	}

	public static abstract interface VhUpdatePtr {
		public abstract void handler(Bitmap bitmap);
	}

	public static abstract interface ShInitPtr {
		public abstract int handler(String gamename);
	}

	public static abstract interface ShStartPtr {
		public abstract int handler();
	}

	public static abstract interface ShStopPtr {
		public abstract void handler();
	}

	public static abstract interface ShUpdatePtr {
		public abstract void handler();
	}

	public static abstract interface DecodePtr {
		public abstract void handler();
	}

	public static abstract interface HiscoreLoadPtr {
		public abstract int handler();
	}

	public static abstract interface HiscoreSavePtr {
		public abstract void handler();
	}

	public static abstract interface ConversionPtr {
		public abstract int handler(int data);
	}

	public static abstract interface RomLoadPtr {
		public abstract void handler();
	}

	public static abstract interface InputPortPtr {
		public abstract void handler();
	}
}
