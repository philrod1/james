package emulator.memory;


public class CharPtr {

	private char[] RAM;
	private int base;

	public CharPtr() {

	}
	
	public CharPtr(char[] RAM) {
		set(RAM, 0);
	}

	public CharPtr(char[] RAM, int b) {
		set(RAM, b);
	}

	public CharPtr(CharPtr pointer, int base) {
		set(pointer.RAM, pointer.base + base);
	}

	public void set(char[] RAM, int base) {
		this.RAM = RAM;
		this.base = base;
	}

	public void set(CharPtr pointer, int base) {
		set(pointer.RAM, pointer.base + base);
	}

	public char read(int offset) {
		return RAM[base + offset];
	}

	public char read() {
		return RAM[base];
	}

	public char readDec() {
		return RAM[base--];
	}

	public char readInc() {
		return RAM[base++];
	}

	public void write(int offset, int value) {
		RAM[base + offset] = (char) value;
	}

	public void write(int value) {
		RAM[base] = (char) value;
	}

	public void writeInc(int value) {
		RAM[base++] = (char) value;
	}

	public void and(int value) {
		RAM[base] &= (char) value;
	}

	public void or(int value) {
		RAM[base] |= (char) value;
	}

	public void dec() {
		--base;
	}

	public void inc() {
		++base;
	}

	public void inc(int count) {
		base += count;
	}
}