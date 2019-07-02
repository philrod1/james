package emulator.machine;

import java.util.Random;

import emulator.cpu.z80.Z80;
import emulator.games.Pacman;
import emulator.memory.MemoryMappedIO;

public class PartMachine implements Machine {
	
	private final int cycles;
	protected final int fps;
	private final Z80 cpu;
	private final char[] memory;
	private final MemoryMappedIO[] io;
	private final Random rng;
	protected Snapshot snapshot;
	
	public PartMachine(Pacman pacman) {
		fps = pacman.getFPS();
		cpu = new Z80(this);
		rng = new Random();
		memory = pacman.getMemory();
		/* more random */
		memory[0x4dc9] = (char) rng.nextInt(0x5000);
		memory[0x4dca] = (char) rng.nextInt(0x5000);
		io = pacman.getIO();
		cycles = pacman.getClock() / pacman.getFPS();
	}

	@Override
	public void step() {
		for(MemoryMappedIO mmio : io) {
			memory[mmio.address] = mmio.value;
		}
		cpu.execute(cycles);
		cpu.causeInterrupt(0);
		snapshot = new Snapshot(getRam(), cpu.getRegs());
	}

	private char[] getRam() {
		char[] ram = new char[4096];
		System.arraycopy(memory, 0x4000, ram, 0, 4096);
		return ram;
	}

	@Override
	public int memoryRead(int address) {
		return memory[address];
	}

	@Override
	public void memoryWrite(int address, int data) {
		//Memory mapped IO. This will probably mess up the sound, but we don't use sound.
		if(address == 0x5000 || address == 0x5040 || address == 0x5080)
			return;
		memory[address] = (char) data;
	}

	@Override
	public void portWrite(int port, char value) {
		io[port].value = value;
	}

	@Override
	public char portRead(int port) {
		return io[port].value;
	}

	@Override
	public Snapshot getSnapshot() {
		return snapshot;
	}

	@Override
	public void syncToSnapshot(Snapshot snapshot) {
		this.snapshot = snapshot;
		snapshot.RAM[0x0dc9] = (char) rng.nextInt(0x10000);
		snapshot.RAM[0x0dca] = (char) rng.nextInt(0x10000);
		memCopy(memory, snapshot.RAM, 0x4000, 4096);
		cpu.setRegs(snapshot.regs);
	}
	
	private synchronized void memCopy(char[] dest, char[] src, int offset, int limit) {
		if (limit >= 0) System.arraycopy(src, 0, dest, offset, limit);
	}
}