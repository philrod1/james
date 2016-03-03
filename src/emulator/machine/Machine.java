package emulator.machine;


public interface Machine {
	void step();
	int memoryRead(int address);
	void memoryWrite(int address, int data);
	void ioWrite(int port, char value);
	char ioRead(int port);
	Snapshot getSnapshot();
	void syncToSnapshot(Snapshot snapshot);
}
