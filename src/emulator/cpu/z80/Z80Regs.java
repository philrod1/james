package emulator.cpu.z80;

public class Z80Regs {

	public int af2, bc2, de2, hl2;
	public int iff1, iff2, halt, im, i, r, r2;
	public int af, pc, sp;
	public int a, f;
	public int pending_irq, pending_nmi;
	public Z80Pair
			bc = new Z80Pair(),
			de = new Z80Pair(),
			hl = new Z80Pair(),
			ix = new Z80Pair(),
			iy = new Z80Pair();

	public Z80Regs copy() {
		Z80Regs copy = new Z80Regs();
		copy.af = af;
		copy.pc = pc;
		copy.sp = sp;
		copy.a = a;
		copy.f = f;
		copy.bc.setW(bc.w);
		copy.de.setW(de.w);
		copy.hl.setW(hl.w);
		copy.ix.setW(ix.w);
		copy.iy.setW(iy.w);
		copy.af2 = af2;
		copy.bc2 = bc2;
		copy.de2 = de2;
		copy.hl2 = hl2;
		copy.iff1 = iff1;
		copy.iff2 = iff2;
		copy.halt = halt;
		copy.im = im;
		copy.i = i;
		copy.r = r;
		copy.r2 = r2;
		copy.pending_irq = pending_irq;
		copy.pending_nmi = pending_nmi;
		return copy;
	}

}
