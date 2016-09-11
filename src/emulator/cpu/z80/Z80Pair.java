package emulator.cpu.z80;


public class Z80Pair {

	public int H, L, W;

	public void SetH(int val) {
		H = val;
		W = (H << 8) | L;
	}

	public void SetL(int val) {
		L = val;
		W = (H << 8) | L;
	}

	public void SetW(int val) {
		W = val;
		H = W >> 8;
		L = W & 0xFF;
	}

	public void AddH(int val) {
		H = (H + val) & 0xFF;
		W = (H << 8) | L;
	}

	public void AddW(int val) {
		W = (W + val) & 0xFFFF;
		H = W >> 8;
		L = W & 0xFF;
	}

	public void AddL(int val) {
		L = (L + val) & 0xFF;
		W = (H << 8) | L;
	}
	
	@Override
	public String toString() {
		return H + " " + L + " " + W;
	}
}