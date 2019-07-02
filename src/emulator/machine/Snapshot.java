package emulator.machine;

import java.awt.Point;
import java.io.Serializable;

import emulator.cpu.z80.Z80Regs;

public class Snapshot implements Serializable {

	private static final long serialVersionUID = -6423426747622876164L;
	public final char[] RAM;
	public final Z80Regs regs;
	
	public Snapshot (char[] RAM, Z80Regs regs) {
		this.RAM = RAM;
		this.regs = regs;
	}

	public Point getPacman() {
		return new Point(new Point(255-RAM[0x0d09], RAM[0x0d08]));
	}
}
