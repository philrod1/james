package emulator.cpu;

import emulator.cpu.z80.Z80Regs;


public class CPUContext {

	public Z80Regs regs;
	public int iCount;
	
	public CPUContext (Z80Regs regs, int iCount) {
		this.regs = regs;
		this.iCount = iCount;
	}

}
