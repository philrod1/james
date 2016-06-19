package emulator.cpu.z80;


public class Z80Pair {

	public int h, l, w;

	public void setH(int value) {
		h = value;
		w = (h << 8) | l;
	}

	public void setL(int value) {
		l = value;
		w = (h << 8) | l;
	}

	public void setW(int value) {
		w = value;
		h = w >> 8;
		l = w & 0xFF;
	}

	public void addH(int value) {
		h = (h + value) & 0xFF;
		w = (h << 8) | l;
	}

	public void addW(int value) {
		w = (w + value) & 0xFFFF;
		h = w >> 8;
		l = w & 0xFF;
	}

	public void addL(int value) {
		l = (l + value) & 0xFF;
		w = (h << 8) | l;
	}
	
	@Override
	public String toString() {
		return h + " " + l + " " + w;
	}
}