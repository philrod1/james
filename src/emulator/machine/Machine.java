package emulator.machine;


public interface Machine {
	void step();
	int memoryRead(int address);
	void memoryWrite(int address, int data);
	void portWrite(int port, char value);
	char portRead(int port);
	Snapshot getSnapshot();
	void syncToSnapshot(Snapshot snapshot);
}
