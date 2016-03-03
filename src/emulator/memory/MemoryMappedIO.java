package emulator.memory;

public class MemoryMappedIO {
	
	public final int address;
	public char value;
	
	public MemoryMappedIO (int address, char value) {
		this.address = address;
		this.value = value;
	}
	
	public void and(char value) {
		this.value &= value;
	}
	
	@Override
	public String toString() {
		return "[" + Integer.toHexString(address) + "] = " + Integer.toHexString(value);
	}
}
