package emulator.cpu.z80;


import java.io.Serializable;

/*
 * The Z80 registers. HALT is set to 1 when the CPU is halted, the refresh
 * register is calculated as follows: refresh=(Regs.R&127)|(Regs.R2&128)
 */
public class Z80Regs implements Serializable {

	public int AF2, BC2, DE2, HL2;
	public int IFF1, IFF2, HALT, IM, I, R, R2;
	public int AF, PC, SP;
	public int A, F;
	public Z80Pair BC = new Z80Pair();
	public Z80Pair DE = new Z80Pair();
	public Z80Pair HL = new Z80Pair();
	public Z80Pair IX = new Z80Pair();
	public Z80Pair IY = new Z80Pair();
	public int pending_irq = 0;
	public int pending_nmi;

}
