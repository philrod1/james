package emulator.cpu.z80;


import emulator.machine.Machine;

public class Z80 {

	private static final int IGNORE_INT = -1; /* Ignore interrupt */
//	private static final int NMI_INT = -2;    /* Execute NMI */

	private static final int S_FLAG = 0b10000000;
	private static final int Z_FLAG = 0b01000000;
	private static final int H_FLAG = 0b00010000;
	private static final int P_FLAG = 0b00000100;
	private static final int N_FLAG = 0b00000010;
	private static final int C_FLAG = 0b00000001;

	private int iCount = 0;
	private final Z80Regs regs = new Z80Regs();
	private int[] pTable   = new int[512];
	private int[] zsTable  = new int[512];
	private int[] zspTable = new int[512];
	private final Machine machine;

	private final Op notImplemented = () -> {
		System.err.println("Not implemented");
		incPC();
		return 0;
	};

	public Z80(Machine machine) {
		this.machine = machine;
		initTables();
	}

	public int execute(int cycles) {
		iCount = cycles;
		do {
			if (regs.pending_nmi != 0 || regs.pending_irq != IGNORE_INT) {
				interrupt();
			}
			regs.r++;
			int i = readByte(regs.pc);
			incPC();
			iCount -= op_main[i].call();
		} while (iCount > 0);
		return cycles - iCount;
	}

	private final Op[] op_main = {
		/* 00 */ () -> 																   4       , // NO_OP
		/* 01 */ () -> { regs.bc.setW(readWord()); 								return 10	; }, // LD_BC_WORD
		/* 02 */ () -> { writeByte(regs.bc.w, regs.a); 							return 7	; }, // LD_XBC_A
		/* 03 */ () -> { regs.bc.addW(1); 										return 6	; }, // INC_BC
		/* 04 */ () -> { regs.bc.setH(inc(regs.bc.h)); 							return 4	; }, // INC_B
		/* 05 */ () -> { regs.bc.setH(dec(regs.bc.h)); 							return 4	; }, // DEC_B
		/* 06 */ () -> { regs.bc.setH(readByte()); 								return 7	; }, // LD_B_BYTE
		/* 07 */ () -> { rlca(); 												return 4	; }, // RLCA
		/* 08 */ () -> { exAFAF(); 												return 4	; }, // EX_AF_AF
		/* 09 */ () -> { regs.hl.setW(addWord(regs.hl.w, regs.bc.w)); 			return 11	; }, // ADD_HL_BC
		/* 0A */ () -> { regs.a = readMem(regs.bc.w); 							return 7	; }, // LD_A_XBC
		/* 0B */ () -> { regs.bc.addW(-1); 										return 6	; }, // DEC_BC
		/* 0C */ () -> { regs.bc.setL(inc(regs.bc.l)); 							return 4	; }, // INC_C
		/* 0D */ () -> { regs.bc.setL(dec(regs.bc.l)); 							return 4	; }, // DEC_C
		/* 0E */ () -> { regs.bc.setL(readByte()); 								return 7	; }, // LD_C_BYTE
		/* 0F */ () -> { rrca(); 												return 4	; }, // RRCA
		/* 10 */ () -> { regs.bc.addH(-1); jumpRelative(regs.bc.h!=0);			return 8	; }, // DJNZ
		/* 11 */ () -> { regs.de.setW(readWord()); 								return 10	; }, // LD_DE_WORD
		/* 12 */ () -> { writeByte(regs.de.w, regs.a); 							return 7	; }, // LD_XDE_A
		/* 13 */ () -> { regs.de.addW(1); 										return 6	; }, // INC_DE
		/* 14 */ () -> { regs.de.setH(inc(regs.de.h)); 							return 4	; }, // INC_D
		/* 15 */ () -> { regs.de.setH(dec(regs.de.h)); 							return 4	; }, // DEC_D
		/* 16 */ () -> { regs.de.setH(readByte()); 								return 7	; }, // LD_D_BYTE
		/* 17 */ () -> { rla(); 												return 4	; }, // RLA
		/* 18 */ () -> { jumpRelative(); 										return 7	; }, // JR
		/* 19 */ () -> { regs.hl.setW(addWord(regs.hl.w, regs.de.w)); 			return 11	; }, // ADD_HL_DE
		/* 1A */ () -> { regs.a = readMem(regs.de.w); 							return 7	; }, // LD_A_XDE
		/* 1B */ () -> { regs.de.addW(-1); 										return 6	; }, // DEC_DE
		/* 1C */ () -> { regs.de.setL(inc(regs.de.l)); 							return 4	; }, // INC_E
		/* 1D */ () -> { regs.de.setL(dec(regs.de.l)); 							return 4	; }, // DEC_E
		/* 1E */ () -> { regs.de.setL(readByte()); 								return 7	; }, // LD_E_BYTE
		/* 1F */ () -> { rra(); 												return 4	; }, // RRA
		/* 20 */ () -> { jumpRelative(notZero()); 								return 7	; }, // JR_NZ
		/* 21 */ () -> { regs.hl.setW(readWord()); 								return 10	; }, // LD_HL_WORD
		/* 22 */ () -> { writeWord(readWord(), regs.hl.w); 						return 16	; }, // LD_XWORD_HL
		/* 23 */ () -> { regs.hl.addW(1); 										return 6	; }, // INC_HL
		/* 24 */ () -> { regs.hl.setH(inc(regs.hl.h)); 							return 4	; }, // INC_H
		/* 25 */ () -> { regs.hl.setH(dec(regs.hl.h)); 							return 4	; }, // DEC_H
		/* 26 */ () -> { regs.hl.setH(readByte()); 								return 7	; }, // LD_H_BYTE
		/* 27 */ () -> { daa(); 												return 4	; }, // DAA
		/* 28 */ () -> { jumpRelative(zero()); 									return 7	; }, // JR_Z
		/* 29 */ () -> { regs.hl.setW(addWord(regs.hl.w, regs.hl.w)); 			return 11	; }, // ADD_HL_HL
		/* 2A */ () -> { regs.hl.setW(readWord(readWord())); 					return 16	; }, // LD_HL_XWORD
		/* 2B */ () -> { regs.hl.addW(-1); 										return 6	; }, // DEC_HL
		/* 2C */ () -> { regs.hl.setL(inc(regs.hl.l)); 							return 4	; }, // INC_L
		/* 2D */ () -> { regs.hl.setL(dec(regs.hl.l)); 							return 4	; }, // DEC_L
		/* 2E */ () -> { regs.hl.setL(readByte()); 								return 7	; }, // LD_L_BYTE
		/* 2F */ () -> { regs.a ^= 0xFF; regs.f |= (H_FLAG | N_FLAG); 			return 4	; }, // CPL
		/* 30 */ () -> { jumpRelative(notCarry()); 								return 7	; }, // JR_NC
		/* 31 */ () -> { regs.sp = readWord(); 									return 10	; }, // LD_SP_WORD
		/* 32 */ () -> { writeByte(readWord(), regs.a); 						return 13	; }, // LD_XBYTE_A
		/* 33 */ () -> { regs.sp = (regs.sp + 1) & 0xFFFF; 						return 6	; }, // INC_SP
		/* 34 */ () -> { writeByte(regs.hl.w, inc(readMem(regs.hl.w))); 		return 11	; }, // INC_XHL
		/* 35 */ () -> { writeByte(regs.hl.w, dec(readMem(regs.hl.w))); 		return 11	; }, // DEC_XHL
		/* 36 */ () -> { writeByte(regs.hl.w, readByte()); 						return 10	; }, // LD_XHL_BYTE
		/* 37 */ () -> { regs.f = (regs.f & 0xEC) | C_FLAG; 					return 4	; }, // SCF
		/* 38 */ () -> { jumpRelative(carry()); 								return 7	; }, // JR_C
		/* 39 */ () -> { regs.hl.setW(addWord(regs.hl.w, regs.sp)); 			return 11	; }, // ADD_HL_SP
		/* 3A */ () -> { regs.a = readMem(readWord()); 							return 13	; }, // LD_A_XBYTE
		/* 3B */ () -> { regs.sp = (regs.sp - 1) & 0xFFFF; 						return 6	; }, // DEC_SP
		/* 3C */ () -> { regs.a = inc(regs.a); 									return 4	; }, // INC_A
		/* 3D */ () -> { regs.a = dec(regs.a); 									return 4	; }, // DEC_A
		/* 3E */ () -> { regs.a = readByte(); 									return 7	; }, // LD_A_BYTE
		/* 3F */ () -> { regs.f = ((regs.f&0xED)|((regs.f&1)<<4))^1; 			return 4	; }, // CCF
		/* 40 */ () ->  															   4	   , // LD_B_B
		/* 41 */ () -> { regs.bc.setH(regs.bc.l); 								return 4	; }, // LD_B_C
		/* 42 */ () -> { regs.bc.setH(regs.de.h); 								return 4	; }, // LD_B_D
		/* 43 */ () -> { regs.bc.setH(regs.de.l); 								return 4	; }, // LD_B_E
		/* 44 */ () -> { regs.bc.setH(regs.hl.h); 								return 4	; }, // LD_B_H
		/* 45 */ () -> { regs.bc.setH(regs.hl.l); 								return 4	; }, // LD_B_L
		/* 46 */ () -> { regs.bc.setH(readXHL()); 								return 7	; }, // LD_B_XHL
		/* 47 */ () -> { regs.bc.setH(regs.a); 									return 4	; }, // LD_B_A
		/* 48 */ () -> { regs.bc.setL(regs.bc.h); 								return 4	; }, // LD_C_B
		/* 49 */ () -> 																   4       , // LD_C_C
		/* 4A */ () -> { regs.bc.setL(regs.de.h); 								return 4	; }, // LD_C_D
		/* 4B */ () -> { regs.bc.setL(regs.de.l); 								return 4	; }, // LD_C_E
		/* 4C */ () -> { regs.bc.setL(regs.hl.h); 								return 4	; }, // LD_C_H
		/* 4D */ () -> { regs.bc.setL(regs.hl.l); 								return 4	; }, // LD_C_L
		/* 4E */ () -> { regs.bc.setL(readXHL()); 								return 7	; }, // LD_C_XHL
		/* 4F */ () -> { regs.bc.setL(regs.a); 									return 4	; }, // LD_C_A
		/* 50 */ () -> { regs.de.setH(regs.bc.h); 								return 4	; }, // LD_D_B
		/* 51 */ () -> { regs.de.setH(regs.bc.l); 								return 4	; }, // LD_D_C
		/* 52 */ () -> 																   4       , // LD_D_D
		/* 53 */ () -> { regs.de.setH(regs.de.l); 								return 4	; }, // LD_D_E
		/* 54 */ () -> { regs.de.setH(regs.hl.h); 								return 4	; }, // LD_D_H
		/* 55 */ () -> { regs.de.setH(regs.hl.l); 								return 4	; }, // LD_D_L
		/* 56 */ () -> { regs.de.setH(readXHL()); 								return 7	; }, // LD_D_XHL
		/* 57 */ () -> { regs.de.setH(regs.a); 									return 4	; }, // LD_D_A
		/* 58 */ () -> { regs.de.setL(regs.bc.h); 								return 4	; }, // LD_E_B
		/* 59 */ () -> { regs.de.setL(regs.bc.l); 								return 4	; }, // LD_E_C
		/* 5A */ () -> { regs.de.setL(regs.de.h); 								return 4	; }, // LD_E_D
		/* 5B */ () -> 																   4       , // LD_E_E
		/* 5C */ () -> { regs.de.setL(regs.hl.h); 								return 4	; }, // LD_E_H
		/* 5D */ () -> { regs.de.setL(regs.hl.l); 								return 4	; }, // LD_E_L
		/* 5E */ () -> { regs.de.setL(readXHL()); 								return 7	; }, // LD_E_XHL
		/* 5F */ () -> { regs.de.setL(regs.a); 									return 4	; }, // LD_E_A
		/* 60 */ () -> { regs.hl.setH(regs.bc.h); 								return 4	; }, // LD_H_B
		/* 61 */ () -> { regs.hl.setH(regs.bc.l); 								return 4	; }, // LD_H_C
		/* 62 */ () -> { regs.hl.setH(regs.de.h); 								return 4	; }, // LD_H_D
		/* 63 */ () -> { regs.hl.setH(regs.de.l); 								return 4	; }, // LD_H_E
		/* 64 */ () -> 																   4       , // LD_H_H
		/* 65 */ () -> { regs.hl.setH(regs.hl.l); 								return 4	; }, // LD_H_L
		/* 66 */ () -> { regs.hl.setL(readXHL()); 								return 7	; }, // LD_H_XHL
		/* 67 */ () -> { regs.hl.setH(regs.a); 									return 4	; }, // LD_H_A
		/* 68 */ () -> { regs.hl.setL(regs.bc.h); 								return 4	; }, // LD_L_B
		/* 69 */ () -> { regs.hl.setL(regs.bc.l); 								return 4	; }, // LD_L_C
		/* 6A */ () -> { regs.hl.setL(regs.de.h); 								return 4	; }, // LD_L_D
		/* 6B */ () -> { regs.hl.setL(regs.de.l); 								return 4	; }, // LD_L_E
		/* 6C */ () -> { regs.hl.setL(regs.hl.h); 								return 4	; }, // LD_L_H
		/* 6D */ () -> 																   4	   , // LD_L_L
		/* 6E */ () -> { regs.hl.setL(readXHL()); 								return 7	; }, // LD_L_XHL
		/* 6F */ () -> { regs.hl.setL(regs.a); 									return 4	; }, // LD_L_A
		/* 70 */ () -> { writeByte(regs.hl.w, regs.bc.h); 						return 7	; }, // LD_XHL_B
		/* 71 */ () -> { writeByte(regs.hl.w, regs.bc.l); 						return 7	; }, // LD_XHL_C
		/* 72 */ () -> { writeByte(regs.hl.w, regs.de.h); 						return 7	; }, // LD_XHL_D
		/* 73 */ () -> { writeByte(regs.hl.w, regs.de.l); 						return 7	; }, // LD_XHL_E
		/* 74 */ () -> { writeByte(regs.hl.w, regs.hl.h); 						return 7	; }, // LD_XHL_H
		/* 75 */ () -> { writeByte(regs.hl.w, regs.hl.l); 						return 7	; }, // LD_XHL_L
		/* 76 */ () -> { halt(); 												return 4	; }, // HALT
		/* 77 */ () -> { writeByte(regs.hl.w, regs.a); 							return 7	; }, // LD_XHL_A
		/* 78 */ () -> { regs.a = regs.bc.h; 									return 4	; }, // LD_A_B
		/* 79 */ () -> { regs.a = regs.bc.l; 									return 4	; }, // LD_A_C
		/* 7A */ () -> { regs.a = regs.de.h; 									return 4	; }, // LD_A_D
		/* 7B */ () -> { regs.a = regs.de.l; 									return 4	; }, // LD_A_E
		/* 7C */ () -> { regs.a = regs.hl.h; 									return 4	; }, // LD_A_H
		/* 7D */ () -> { regs.a = regs.hl.l; 									return 4	; }, // LD_A_L
		/* 7E */ () -> { regs.a = readXHL(); 									return 7	; }, // LD_A_XHL
		/* 7F */ () -> 																   4	   , // LD_A_A
		/* 80 */ () -> { add(regs.bc.h); 										return 4	; }, // ADD_A_B
		/* 81 */ () -> { add(regs.bc.l); 										return 4	; }, // ADD_A_C
		/* 82 */ () -> { add(regs.de.h); 										return 4	; }, // ADD_A_D
		/* 83 */ () -> { add(regs.de.l); 										return 4	; }, // ADD_A_E
		/* 84 */ () -> { add(regs.hl.h); 										return 4	; }, // ADD_A_H
		/* 85 */ () -> { add(regs.hl.l); 										return 4	; }, // ADD_A_L
		/* 86 */ () -> { add(readXHL()); 										return 7	; }, // ADD_A_XHL
		/* 87 */ () -> { add(regs.a); 											return 4	; }, // ADD_A_A
		/* 88 */ () -> { addCarry(regs.bc.h); 									return 4	; }, // ADC_A_B
		/* 89 */ () -> { addCarry(regs.bc.l); 									return 4	; }, // ADC_A_C
		/* 8A */ () -> { addCarry(regs.de.h); 									return 4	; }, // ADC_A_D
		/* 8B */ () -> { addCarry(regs.de.l); 									return 4	; }, // ADC_A_E
		/* 8C */ () -> { addCarry(regs.hl.h); 									return 4	; }, // ADC_A_H
		/* 8D */ () -> { addCarry(regs.hl.l); 									return 4	; }, // ADC_A_L
		/* 8E */ () -> { addCarry(readXHL()); 									return 7	; }, // ADC_A_XHL
		/* 8F */ () -> { addCarry(regs.a); 										return 4	; }, // ADC_A_A
		/* 90 */ () -> { sub(regs.bc.h); 										return 4	; }, // SUB_B
		/* 91 */ () -> { sub(regs.bc.l); 										return 4	; }, // SUB_C
		/* 92 */ () -> { sub(regs.de.h); 										return 4	; }, // SUB_D
		/* 93 */ () -> { sub(regs.de.l); 										return 4	; }, // SUB_E
		/* 94 */ () -> { sub(regs.hl.h); 										return 4	; }, // SUB_H
		/* 95 */ () -> { sub(regs.hl.l); 										return 4	; }, // SUB_L
		/* 96 */ () -> { sub(readXHL()); 										return 7	; }, // SUB_XHL
		/* 97 */ () -> { regs.a = 0; regs.f = Z_FLAG | N_FLAG; 					return 4	; }, // SUB_A
		/* 98 */ () -> { subCarry(regs.bc.h); 									return 4	; }, // SBC_A_B
		/* 99 */ () -> { subCarry(regs.bc.l); 									return 4	; }, // SBC_A_C
		/* 9A */ () -> { subCarry(regs.de.h); 									return 4	; }, // SBC_A_D
		/* 9B */ () -> { subCarry(regs.de.l); 									return 4	; }, // SBC_A_E
		/* 9C */ () -> { subCarry(regs.hl.h); 									return 4	; }, // SBC_A_H
		/* 9D */ () -> { subCarry(regs.hl.l); 									return 4	; }, // SBC_A_L
		/* 9E */ () -> { subCarry(readXHL()); 									return 7	; }, // SBC_A_XHL
		/* 9F */ () -> { subCarry(regs.a); 										return 4	; }, // SBC_A_A
		/* A0 */ () -> { and(regs.bc.h); 										return 4	; }, // AND_B
		/* A1 */ () -> { and(regs.bc.l); 										return 4	; }, // AND_C
		/* A2 */ () -> { and(regs.de.h); 										return 4	; }, // AND_D
		/* A3 */ () -> { and(regs.de.l); 										return 4	; }, // AND_E
		/* A4 */ () -> { and(regs.hl.h); 										return 4	; }, // AND_H
		/* A5 */ () -> { and(regs.hl.l); 										return 4	; }, // AND_L
		/* A6 */ () -> { and(readXHL()); 										return 7	; }, // AND_XHL
		/* A7 */ () -> { regs.f = zspTable[regs.a] | H_FLAG; 					return 4	; }, // AND_A
		/* A8 */ () -> { xor(regs.bc.h); 										return 4	; }, // XOR_B
		/* A9 */ () -> { xor(regs.bc.l); 										return 4	; }, // XOR_C
		/* AA */ () -> { xor(regs.de.h); 										return 4	; }, // XOR_D
		/* AB */ () -> { xor(regs.de.l); 										return 4	; }, // XOR_E
		/* AC */ () -> { xor(regs.hl.h); 										return 4	; }, // XOR_H
		/* AD */ () -> { xor(regs.hl.l); 										return 4	; }, // XOR_L
		/* AE */ () -> { xor(readXHL()); 										return 7	; }, // XOR_XHL
		/* AF */ () -> { regs.a = 0; regs.f = Z_FLAG | P_FLAG; 					return 4	; }, // XOR_A
		/* B0 */ () -> { or(regs.bc.h); 										return 4	; }, // OR_B
		/* B1 */ () -> { or(regs.bc.l); 										return 4	; }, // OR_C
		/* B2 */ () -> { or(regs.de.h); 										return 4	; }, // OR_D
		/* B3 */ () -> { or(regs.de.l); 										return 4	; }, // OR_E
		/* B4 */ () -> { or(regs.hl.h); 										return 4	; }, // OR_H
		/* B5 */ () -> { or(regs.hl.l); 										return 4	; }, // OR_L
		/* B6 */ () -> { or(readXHL()); 										return 7	; }, // OR_XHL
		/* B7 */ () -> { regs.f = zspTable[regs.a]; 							return 4	; }, // OR_A
		/* B8 */ () -> { compare(regs.bc.h); 									return 4	; }, // CP_B
		/* B9 */ () -> { compare(regs.bc.l); 									return 4	; }, // CP_C
		/* BA */ () -> { compare(regs.de.h); 									return 4	; }, // CP_D
		/* BB */ () -> { compare(regs.de.l); 									return 4	; }, // CP_E
		/* BC */ () -> { compare(regs.hl.h); 									return 4	; }, // CP_H
		/* BD */ () -> { compare(regs.hl.l); 									return 4	; }, // CP_L
		/* BE */ () -> { compare(readXHL()); 									return 7	; }, // CP_XHL
		/* BF */ () -> { compare(regs.a); 										return 4	; }, // CP_A
		/* C0 */ () -> { ret(notZero()); 										return 5	; }, // RET_NZ
		/* C1 */ () -> { regs.bc.setW(pop()); 									return 10	; }, // POP_BC
		/* C2 */ () -> { jump(notZero()); 										return 10	; }, // JP_NZ
		/* C3 */ () -> { jp(); 													return 10	; }, // JP
		/* C4 */ () -> { call(notZero()); 										return 10	; }, // CALL_NZ
		/* C5 */ () -> { push(regs.bc.w); 										return 11	; }, // PUSH_BC
		/* C6 */ () -> { add(readByte()); 										return 7	; }, // ADD_A_BYTE
		/* C7 */ () -> { reset(0); 												return 11	; }, // RST_00
		/* C8 */ () -> { ret(zero()); 											return 5	; }, // RET_Z
		/* C9 */ () -> { ret(); 												return 4	; }, // RET
		/* CA */ () -> { jump(zero()); 											return 10	; }, // JP_Z
		/* CB */ () -> { cb(); 													return 0	; }, // CB
		/* CC */ () -> { call(zero()); 											return 10	; }, // CALL_Z
		/* CD */ () -> { call(); 												return 10	; }, // CALL
		/* CE */ () -> { addCarry(readByte()); 									return 7	; }, // ADC_A_BYTE
		/* CF */ () -> { reset(0x08); 											return 11	; }, // RST_08
		/* D0 */ () -> { ret(notCarry()); 										return 5	; }, // RET_NC
		/* D1 */ () -> { regs.de.setW(pop()); 									return 10	; }, // POP_DE
		/* D2 */ () -> { jump(notCarry()); 										return 10	; }, // JP_NC
		/* D3 */ () -> { portOut(readByte(), regs.a); 							return 11	; }, // OUT_BYTE_A
		/* D4 */ () -> { call(notCarry()); 										return 10	; }, // CALL_NC
		/* D5 */ () -> { push(regs.de.w); 										return 11	; }, // PUSH_DE
		/* D6 */ () -> { sub(readByte()); 										return 7	; }, // SUB_BYTE
		/* D7 */ () -> { reset(0x10); 											return 11	; }, // RST_10
		/* D8 */ () -> { ret(carry()); 											return 5	; }, // RET_C
		/* D9 */ () -> { exx(); 												return 4	; }, // EXX
		/* DA */ () -> { jump(carry()); 										return 10	; }, // JP_C
		/* DB */ () -> { regs.a = portIn(readByte()); 							return 11	; }, // IN_A_BYTE
		/* DC */ () -> { call(carry()); 										return 10	; }, // CALL_C
		/* DD */ () -> { dd(); 													return 0	; }, // DD
		/* DE */ () -> { subCarry(readByte()); 									return 7	; }, // SBC_A_BYTE
		/* DF */ () -> { reset(0x18); 											return 11	; }, // RST_18
		/* E0 */ () -> { ret(notSign()); 										return 5	; }, // RET_PO
		/* E1 */ () -> { regs.hl.setW(pop()); 									return 10	; }, // POP_HL
		/* E2 */ () -> { jump(notSign()); 										return 10	; }, // JP_PO
		/* E3 */ () -> { exXspHL(); 											return 19	; }, // EX_XSP_HL
		/* E4 */ () -> { call(po());											return 10	; }, // CALL_PO
		/* E5 */ () -> { push(regs.hl.w); 										return 11	; }, // PUSH_HL
		/* E6 */ () -> { and(readByte()); 										return 7	; }, // AND_BYTE
		/* E7 */ () -> { reset(0x20); 											return 11	; }, // RST_20
		/* E8 */ () -> { ret(sign()); 											return 5	; }, // RET_PE
		/* E9 */ () -> { regs.pc = regs.hl.w; 									return 4	; }, // JP_HL
		/* EA */ () -> { jump(sign()); 											return 10	; }, // JP_PE
		/* EB */ () -> { exDEHL(); 												return 4	; }, // EX_DE_HL
		/* EC */ () -> { call(pe());					 						return 10	; }, // CALL_PE
		/* ED */ () -> { ed(); 													return 0	; }, // ED
		/* EE */ () -> { xor(readByte()); 										return 7	; }, // XOR_BYTE
		/* EF */ () -> { reset(0x28); 											return 11	; }, // RST_28
		/* F0 */ () -> { ret(notSign()); 										return 5	; }, // RET_P
		/* F1 */ () -> { popAF(); 												return 10	; }, // POP_AF
		/* F2 */ () -> { jump(notSign()); 										return 10	; }, // JP_P
		/* F3 */ () -> { regs.iff1 = regs.iff2 = 0; 							return 4	; }, // DI
		/* F4 */ () -> { call(notSign()); 										return 10	; }, // CALL_P
		/* F5 */ () -> { push((regs.a << 8) | regs.f); 							return 11	; }, // PUSH_AF
		/* F6 */ () -> { or(readByte()); 										return 7	; }, // OR_BYTE
		/* F7 */ () -> { reset(0x30); 											return 11	; }, // RST_30
		/* F8 */ () -> { ret(sign()); 											return 5	; }, // RET_M
		/* F9 */ () -> { regs.sp = regs.hl.w; 									return 6	; }, // LD_SP_HL
		/* FA */ () -> { jump(sign()); 											return 10	; }, // JP_M
		/* FB */ () -> { ei(); 													return 4	; }, // EI
		/* FC */ () -> { call(sign()); 											return 10	; }, // CALL_M
		/* FD */ () -> { fd(); 													return 0	; }, // FD
		/* FE */ () -> { compare(readByte()); 									return 7	; }, // CP_BYTE
		/* FF */ () -> { reset(0x38); 											return 11	; }  // RST_38
	};

	private final Op[] ed = {
			null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
			null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
			null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
			null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
		/* 40 */ () -> { regs.bc.setH(in()); 									return 12 ; }, // IN_B_C
		/* 41 */ () -> { portOut(regs.bc.l, regs.bc.h); 						return 12 ; }, // OUT_C_B
		/* 42 */ () -> { subWordCarry(regs.bc.w); 								return 15 ; }, // SBC_HL_BC
		/* 43 */ () -> { writeWord(readWord(), regs.bc.w); 						return 20 ; }, // LD_XWORD_BC
		/* 44 */ () -> { int i = regs.a; regs.a = 0; sub(i); 					return 8  ; }, // NEG
		/* 45 */ () -> { regs.iff1 = regs.iff2;  ret(); 						return 8  ; }, // RETN
		/* 46 */ () -> { regs.im = 0; 											return 8  ; }, // IM_0
		/* 47 */ () -> { regs.i = regs.a; 										return 9  ; }, // LD_I_A
		/* 48 */ () -> { regs.bc.setL(in()); 									return 12 ; }, // IN_C_C
		/* 49 */ () -> { portOut(regs.bc.l, regs.bc.l); 						return 12 ; }, // OUT_C_C
		/* 4A */ () -> { addWordCarry(regs.bc.w); 								return 15 ; }, // ADC_HL_BC
		/* 4B */ () -> { regs.bc.setW(readWord(readWord())); 					return 20 ; }, // LD_BC_XWORD
		/* 4C */ () -> { int i = regs.a; regs.a = 0; sub(i); 					return 8  ; }, // NEG
		/* 4D */ () -> { ret(); 												return 8  ; }, // RETI
		/* 4E */ () -> { regs.im = 0; 											return 8  ; }, // IM_0/1
		/* 4F */ () -> { regs.r = regs.r2 = regs.a; 							return 9  ; }, // LD_R_A
		/* 50 */ () -> { regs.de.setH(in()); 									return 12 ; }, // IN_D_C
		/* 51 */ () -> { portOut(regs.bc.l, regs.de.h); 						return 12 ; }, // OUT_C_D
		/* 52 */ () -> { subWordCarry(regs.de.w); 								return 15 ; }, // SBC_HL_DE
		/* 53 */ () -> { writeWord(readWord(), regs.de.w); 						return 20 ; }, // LD_XWORD_DE
		/* 54 */ () -> { int i = regs.a; regs.a = 0; sub(i); 					return 8  ; }, // NEG
		/* 55 */ () -> { regs.iff1 = regs.iff2;  ret(); 						return 8  ; }, // RETN
		/* 56 */ () -> { regs.im = 1; 											return 8  ; }, // IM_1
		/* 57 */ () -> { ldAI(); 												return 9  ; }, // LD_A_I
		/* 58 */ () -> { regs.de.setL(in()); 									return 12 ; }, // IN_E_C
		/* 59 */ () -> { portOut(regs.bc.l, regs.de.l); 						return 12 ; }, // OUT_C_E
		/* 5A */ () -> { addWordCarry(regs.de.w); 								return 15 ; }, // ADC_HL_DE
		/* 5B */ () -> { regs.de.setW(readWord(readWord())); 					return 20 ; }, // LD_DE_XWORD
		/* 5C */ () -> { int i = regs.a; regs.a = 0; sub(i); 					return 8  ; }, // NEG
		/* 5D */ () -> { ret(); 												return 8  ; }, // RETI
		/* 5E */ () -> { regs.im = 2; 											return 8  ; }, // IM_2
		/* 5F */ () -> { ldAR(); 												return 9  ; }, // LD_A_R
		/* 60 */ () -> { regs.hl.setH(in()); 									return 12 ; }, // IN_H_C
		/* 61 */ () -> { portOut(regs.bc.l, regs.hl.h); 						return 12 ; }, // OUT_C_H
		/* 62 */ () -> { subWordCarry(regs.hl.w); 								return 15 ; }, // SBD_HL_HL
		/* 63 */ () -> { writeWord(readWord(), regs.hl.w); 						return 20 ; }, // LD_XWORD_HL
		/* 64 */ () -> { int i = regs.a; regs.a = 0; sub(i); 					return 8  ; }, // NEG
		/* 65 */ () -> { regs.iff1 = regs.iff2;  ret(); 						return 8  ; }, // RETN
		/* 66 */ () -> { regs.im = 0; 											return 8  ; }, // IM_0
		/* 67 */ () -> { rrd(); 												return 18 ; }, // RRD
		/* 68 */ () -> { regs.hl.setL(in()); 									return 12 ; }, // IN_L_C
		/* 69 */ () -> { portOut(regs.bc.l, regs.hl.l); 						return 12 ; }, // OUT_C_L
		/* 6A */ () -> { addWordCarry(regs.hl.w); 								return 15 ; }, // ADC_HL_HL
		/* 6B */ () -> { regs.hl.setW(readWord(readWord())); 					return 20 ; }, // LD_HL_XWORD
		/* 6C */ () -> { int i = regs.a; regs.a = 0; sub(i); 					return 8  ; }, // NEG
		/* 6D */ () -> { ret(); 												return 8  ; }, // RETI
		/* 6E */ () -> { regs.im = 0; 											return 8  ; }, // IM_0/1
		/* 6F */ () -> { rld(); 												return 18 ; }, // RLD
		/* 70 */ () -> 																   12    , // IN_C
		/* 71 */ () -> { portOut(regs.bc.l, 0); 								return 12 ; }, // OUT_C_0
		/* 72 */ () -> { subWordCarry(regs.sp); 								return 15 ; }, // SBC_HL_SP
		/* 73 */ () -> { writeWord(readWord(), regs.sp); 						return 20 ; }, // LD_XWORD_SP
		/* 74 */ () -> { int i = regs.a; regs.a = 0; sub(i); 					return 8  ; }, // NEG
		/* 75 */ () -> { regs.iff1 = regs.iff2;  ret(); 						return 8  ; }, // RETN
		/* 76 */ () -> { regs.im = 1; 											return 8  ; }, // IM_1
		/* 77 */ null,
		/* 78 */ () -> { regs.a = in(); 										return 12 ; }, // IN_A_C
		/* 79 */ () -> { portOut(regs.bc.l, regs.a); 							return 12 ; }, // OUT_C_A
		/* 7A */ () -> { addWordCarry(regs.sp); 								return 15 ; }, // ADC_HL_SP
		/* 7B */ () -> { regs.sp = readWord(readWord()); 						return 20 ; }, // LD_SP_XWORD
		/* 7C */ () -> { int i = regs.a; regs.a = 0; sub(i); 					return 8  ; }, // NEG
		/* 7D */ () -> { ret(); 												return 8  ; }, // RETI
		/* 7E */ () -> { regs.im = 2; 											return 8  ; }, // IM_2
			null,null,null,null,null,null,null,null,null,null,null,
			null,null,null,null,null,null,null,null,null,null,null,
			null,null,null,null,null,null,null,null,null,null,null,
		/* A0 */ () -> { ldi(); 												return 16 ; }, // LDI
		/* A1 */ () -> { cpi(); 												return 16 ; }, // CPI
		/* A2 */ () -> { ini(); 												return 16 ; }, // INI
		/* A3 */ () -> { outi(); 												return 16 ; }, // OUTI
			null,null,null,null,
		/* A8 */ () -> { ldd();  												return 16 ; }, // LDD
		/* A9 */ () -> { cpd();  												return 16 ; }, // CPD
		/* AA */ () -> { ind();  												return 16 ; }, // IND
		/* AB */ () -> { outd(); 												return 16 ; }, // OUTD
			null,null,null,null,
		/* B0 */ () -> { ldir(); 												return 0  ; }, // LDIR
		/* B1 */ () -> { cpir(); 												return 0  ; }, // CPIR
		/* B2 */ () -> { inir(); 												return 0  ; }, // INIR
		/* B3 */ () -> { otir(); 												return 0  ; }, // OTIR
			null,null,null,null,
		/* B8 */ () -> { lddr(); 												return 0  ; }, // LDDR
		/* B9 */ () -> { cpdr(); 												return 0  ; }, // CPDR
		/* BA */ () -> { indr(); 												return 0  ; }, // INDR
		/* BB */ () -> { otdr(); 												return 0  ; }, // OTDR
	};

	private final Op[] cb = {
		/* 00 */ () -> { regs.bc.setH(rlc(regs.bc.h)); 							return 8  ; }, // RLC_B
		/* 01 */ () -> { regs.bc.setL(rlc(regs.bc.l)); 							return 8  ; }, // RLC_C
		/* 02 */ () -> { regs.de.setH(rlc(regs.de.h)); 							return 8  ; }, // RLC_D
		/* 03 */ () -> { regs.de.setL(rlc(regs.de.l)); 							return 8  ; }, // RLC_E
		/* 04 */ () -> { regs.hl.setH(rlc(regs.hl.h)); 							return 8  ; }, // RLC_H
		/* 05 */ () -> { regs.hl.setL(rlc(regs.hl.l)); 							return 8  ; }, // RLC_L
		/* 06 */ () -> { writeByte(regs.hl.w, rlc(readMem(regs.hl.w))); 		return 15 ; }, // RLC_XHL
		/* 07 */ () -> { regs.a = rlc(regs.a); 									return 8  ; }, // RLC_A
		/* 08 */ () -> { regs.bc.setH(rrc(regs.bc.h)); 							return 8  ; }, // RRC_B
		/* 09 */ () -> { regs.bc.setL(rrc(regs.bc.l)); 							return 8  ; }, // RRC_C
		/* 0A */ () -> { regs.de.setH(rrc(regs.de.h)); 							return 8  ; }, // RRC_D
		/* 0B */ () -> { regs.de.setL(rrc(regs.de.l)); 							return 8  ; }, // RRC_E
		/* 0C */ () -> { regs.hl.setH(rrc(regs.hl.h)); 							return 8  ; }, // RRC_H
		/* 0D */ () -> { regs.hl.setL(rrc(regs.hl.l)); 							return 8  ; }, // RRC_L
		/* 0E */ () -> { writeByte(regs.hl.w, rrc(readMem(regs.hl.w))); 		return 15 ; }, // RRC_XHL
		/* 0F */ () -> { regs.a = rrc(regs.a); 									return 8  ; }, // RRC_A
		/* 10 */ () -> { regs.bc.setH(rl(regs.bc.h)); 							return 8  ; }, // RL_B
		/* 11 */ () -> { regs.bc.setL(rl(regs.bc.l)); 							return 8  ; }, // RL_C
		/* 12 */ () -> { regs.de.setH(rl(regs.de.h)); 							return 8  ; }, // RL_D
		/* 13 */ () -> { regs.de.setL(rl(regs.de.l)); 							return 8  ; }, // RL_E
		/* 14 */ () -> { regs.hl.setH(rl(regs.hl.h)); 							return 8  ; }, // RL_H
		/* 15 */ () -> { regs.hl.setL(rl(regs.hl.l)); 							return 8  ; }, // RL_L
		/* 16 */ () -> { writeByte(regs.hl.w, rl(readMem(regs.hl.w))); 			return 15 ; }, // RL_XHL
		/* 17 */ () -> { regs.a = rl(regs.a); 									return 8  ; }, // RL_A
		/* 18 */ () -> { regs.bc.setH(rr(regs.bc.h)); 							return 8  ; }, // RR_B
		/* 19 */ () -> { regs.bc.setL(rr(regs.bc.l)); 							return 8  ; }, // RR_C
		/* 1A */ () -> { regs.de.setH(rr(regs.de.h)); 							return 8  ; }, // RR_D
		/* 1B */ () -> { regs.de.setL(rr(regs.de.l)); 							return 8  ; }, // RR_E
		/* 1C */ () -> { regs.hl.setH(rr(regs.hl.h)); 							return 8  ; }, // RR_H
		/* 1D */ () -> { regs.hl.setL(rr(regs.hl.l)); 							return 8  ; }, // RR_L
		/* 1E */ () -> { writeByte(regs.hl.w, rr(readMem(regs.hl.w))); 			return 15 ; }, // RR_XHL
		/* 1F */ () -> { regs.a = rr(regs.a); 									return 8  ; }, // RR_A
		/* 20 */ () -> { regs.bc.setH(sla(regs.bc.h)); 							return 8  ; }, // SLA_B
		/* 21 */ () -> { regs.bc.setL(sla(regs.bc.l)); 							return 8  ; }, // SLA_C
		/* 22 */ () -> { regs.de.setH(sla(regs.de.h)); 							return 8  ; }, // SLA_D
		/* 23 */ () -> { regs.de.setL(sla(regs.de.l)); 							return 8  ; }, // SLA_E
		/* 24 */ () -> { regs.hl.setH(sla(regs.hl.h)); 							return 8  ; }, // SLA_H
		/* 25 */ () -> { regs.hl.setL(sla(regs.hl.l)); 							return 8  ; }, // SLA_L
		/* 26 */ () -> { writeByte(regs.hl.w, sla(readMem(regs.hl.w))); 		return 15 ; }, // SLA_XHL
		/* 27 */ () -> { regs.a = sla(regs.a); 									return 8  ; }, // SLA_A
		/* 28 */ () -> { regs.bc.setH(sra(regs.bc.h)); 							return 8  ; }, // SRA_B
		/* 29 */ () -> { regs.bc.setL(sra(regs.bc.l)); 							return 8  ; }, // SRA_C
		/* 2A */ () -> { regs.de.setH(sra(regs.de.h)); 							return 8  ; }, // SRA_D
		/* 2B */ () -> { regs.de.setL(sra(regs.de.l)); 							return 8  ; }, // SRA_E
		/* 2C */ () -> { regs.hl.setH(sra(regs.hl.h)); 							return 8  ; }, // SRA_H
		/* 2D */ () -> { regs.hl.setL(sra(regs.hl.l)); 							return 8  ; }, // SRA_L
		/* 2E */ 		 null														         , //
		/* 2F */ () -> { regs.a = sra(regs.a); 		   							return 8  ; }, // SRA_A
		/* 30 */ () -> { regs.bc.setH(sll(regs.bc.h)); 							return 8  ; }, //
		/* 31 */ () -> { regs.bc.setL(sll(regs.bc.l)); 							return 8  ; }, //
		/* 32 */ () -> { regs.de.setH(sll(regs.de.h)); 							return 8  ; }, //
		/* 33 */ () -> { regs.de.setL(sll(regs.de.l)); 							return 8  ; }, //
		/* 34 */ () -> { regs.hl.setH(sll(regs.hl.h)); 							return 8  ; }, //
		/* 35 */ () -> { regs.hl.setL(sll(regs.hl.l)); 							return 8  ; }, //
		/* 36 */ 		 null															     , //
		/* 37 */ () -> { regs.a = sll(regs.a); 		   							return 8  ; }, //
		/* 38 */ () -> { regs.bc.setH(slr(regs.bc.h)); 							return 8  ; }, // SRL_B
		/* 39 */ () -> { regs.bc.setL(slr(regs.bc.l)); 							return 8  ; }, // SRL_C
		/* 3A */ () -> { regs.de.setH(slr(regs.de.h)); 							return 8  ; }, // SRL_D
		/* 3B */ () -> { regs.de.setL(slr(regs.de.l)); 							return 8  ; }, // SRL_E
		/* 3C */ () -> { regs.hl.setH(slr(regs.hl.h)); 							return 8  ; }, // SRL_H
		/* 3D */ () -> { regs.hl.setL(slr(regs.hl.l)); 							return 8  ; }, // SRL_L
		/* 3E */ 		 null														         , //
		/* 3F */ () -> { regs.a = slr(regs.a);  								return 8  ; }, // SRL_A
		/* 40 */ () -> { testBit(0, regs.bc.h); 								return 8  ; }, // BIT_0_B
		/* 41 */ () -> { testBit(0, regs.bc.l); 								return 8  ; }, // BIT_0_C
		/* 42 */ () -> { testBit(0, regs.de.h); 								return 8  ; }, // BIT_0_D
		/* 43 */ () -> { testBit(0, regs.de.l); 								return 8  ; }, // BIT_0_E
		/* 44 */ () -> { testBit(0, regs.hl.h); 								return 8  ; }, // BIT_0_H
		/* 45 */ () -> { testBit(0, regs.hl.l); 								return 8  ; }, // BIT_0_L
		/* 46 */ () -> { testBit(0, readXHL()); 								return 12 ; }, // BIT_0_XHL
		/* 47 */ () -> { testBit(0, regs.a);    								return 8  ; }, // BIT_0_A
		/* 48 */ () -> { testBit(1, regs.bc.h); 								return 8  ; }, // BIT_1_B
		/* 49 */ () -> { testBit(1, regs.bc.l); 								return 8  ; }, // BIT_1_C
		/* 4A */ () -> { testBit(1, regs.de.h); 								return 8  ; }, // BIT_1_D
		/* 4B */ () -> { testBit(1, regs.de.l); 								return 8  ; }, // BIT_1_E
		/* 4C */ () -> { testBit(1, regs.hl.h); 								return 8  ; }, // BIT_1_H
		/* 4D */ () -> { testBit(1, regs.hl.l); 								return 8  ; }, // BIT_1_L
		/* 4E */ () -> { testBit(1, readXHL()); 								return 12 ; }, // BIT_1_XHL
		/* 4F */ () -> { testBit(1, regs.a);    								return 8  ; }, // BIT_1_A
		/* 50 */ () -> { testBit(2, regs.bc.h); 								return 8  ; }, // BIT_2_B
		/* 51 */ () -> { testBit(2, regs.bc.l); 								return 8  ; }, // BIT_2_C
		/* 52 */ () -> { testBit(2, regs.de.h); 								return 8  ; }, // BIT_2_D
		/* 53 */ () -> { testBit(2, regs.de.l); 								return 8  ; }, // BIT_2_E
		/* 54 */ () -> { testBit(2, regs.hl.h); 								return 8  ; }, // BIT_2_H
		/* 55 */ () -> { testBit(2, regs.hl.l); 								return 8  ; }, // BIT_2_L
		/* 56 */ () -> { testBit(2, readXHL()); 								return 12 ; }, // BIT_2_XHL
		/* 57 */ () -> { testBit(2, regs.a);    								return 8  ; }, // BIT_2_A
		/* 58 */ () -> { testBit(3, regs.bc.h); 								return 8  ; }, // BIT_3_B
		/* 59 */ () -> { testBit(3, regs.bc.l); 								return 8  ; }, // BIT_3_C
		/* 5A */ () -> { testBit(3, regs.de.h); 								return 8  ; }, // BIT_3_D
		/* 5B */ () -> { testBit(3, regs.de.l); 								return 8  ; }, // BIT_3_E
		/* 5C */ () -> { testBit(3, regs.hl.h); 								return 8  ; }, // BIT_3_H
		/* 5D */ () -> { testBit(3, regs.hl.l); 								return 8  ; }, // BIT_3_L
		/* 5E */ () -> { testBit(3, readXHL()); 								return 12 ; }, // BIT_3_XHL
		/* 5F */ () -> { testBit(3, regs.a);    								return 8  ; }, // BIT_3_A
		/* 60 */ () -> { testBit(4, regs.bc.h); 								return 8  ; }, // BIT_4_B
		/* 61 */ () -> { testBit(4, regs.bc.l); 								return 8  ; }, // BIT_4_C
		/* 62 */ () -> { testBit(4, regs.de.h); 								return 8  ; }, // BIT_4_D
		/* 63 */ () -> { testBit(4, regs.de.l); 								return 8  ; }, // BIT_4_E
		/* 64 */ () -> { testBit(4, regs.hl.h); 								return 8  ; }, // BIT_4_H
		/* 65 */ () -> { testBit(4, regs.hl.l); 								return 8  ; }, // BIT_4_L
		/* 66 */ () -> { testBit(4, readXHL()); 								return 12 ; }, // BIT_4_XHL
		/* 67 */ () -> { testBit(4, regs.a);    								return 8  ; }, // BIT_4_A
		/* 68 */ () -> { testBit(5, regs.bc.h); 								return 8  ; }, // BIT_5_B
		/* 69 */ () -> { testBit(5, regs.bc.l); 								return 8  ; }, // BIT_5_C
		/* 6A */ () -> { testBit(5, regs.de.h); 								return 8  ; }, // BIT_5_D
		/* 6B */ () -> { testBit(5, regs.de.l); 								return 8  ; }, // BIT_5_E
		/* 6C */ () -> { testBit(5, regs.hl.h); 								return 8  ; }, // BIT_5_H
		/* 6D */ () -> { testBit(5, regs.hl.l); 								return 8  ; }, // BIT_5_L
		/* 6E */ () -> { testBit(5, readXHL()); 								return 12 ; }, // BIT_5_XHL
		/* 6F */ () -> { testBit(5, regs.a);    								return 8  ; }, // BIT_5_A
		/* 70 */ () -> { testBit(6, regs.bc.h); 								return 8  ; }, // BIT_6_B
		/* 71 */ () -> { testBit(6, regs.bc.l); 								return 8  ; }, // BIT_6_C
		/* 72 */ () -> { testBit(6, regs.de.h); 								return 8  ; }, // BIT_6_D
		/* 73 */ () -> { testBit(6, regs.de.l); 								return 8  ; }, // BIT_6_E
		/* 74 */ () -> { testBit(6, regs.hl.h); 								return 8  ; }, // BIT_6_H
		/* 75 */ () -> { testBit(6, regs.hl.l); 								return 8  ; }, // BIT_6_L
		/* 76 */ () -> { testBit(6, readXHL()); 								return 12 ; }, // BIT_6_XHL
		/* 77 */ () -> { testBit(6, regs.a);    								return 8  ; }, // BIT_6_A
		/* 78 */ () -> { testBit(7, regs.bc.h); 								return 8  ; }, // BIT_7_B
		/* 79 */ () -> { testBit(7, regs.bc.l); 								return 8  ; }, // BIT_7_C
		/* 7A */ () -> { testBit(7, regs.de.h); 								return 8  ; }, // BIT_7_D
		/* 7B */ () -> { testBit(7, regs.de.l); 								return 8  ; }, // BIT_7_E
		/* 7C */ () -> { testBit(7, regs.hl.h); 								return 8  ; }, // BIT_7_H
		/* 7D */ () -> { testBit(7, regs.hl.l); 								return 8  ; }, // BIT_7_L
		/* 7E */ () -> { testBit(7, readXHL()); 								return 12 ; }, // BIT_7_XHL
		/* 7F */ () -> { testBit(7, regs.a);    								return 8  ; }, // BIT_7_A
		/* 80 */ () -> { regs.bc.setH(resetBit(0, regs.bc.h)); 					return 8  ; }, // RES_0_B
		/* 81 */ () -> { regs.bc.setL(resetBit(0, regs.bc.l)); 					return 8  ; }, // RES_0_C
		/* 82 */ () -> { regs.de.setH(resetBit(0, regs.de.h)); 					return 8  ; }, // RES_0_D
		/* 83 */ () -> { regs.de.setL(resetBit(0, regs.de.l)); 					return 8  ; }, // RES_0_E
		/* 84 */ () -> { regs.hl.setH(resetBit(0, regs.hl.h)); 					return 8  ; }, // RES_0_H
		/* 85 */ () -> { regs.hl.setL(resetBit(0, regs.hl.l)); 					return 8  ; }, // RES_0_L
		/* 86 */ () -> { writeByte(regs.hl.w, resetBit(0, readMem(regs.hl.w))); return 15 ; }, // RES_0_XHL
		/* 87 */ () -> { regs.a = resetBit(0, regs.a); 							return 8  ; }, // RES_0_A
		/* 88 */ () -> { regs.bc.setH(resetBit(1, regs.bc.h)); 					return 8  ; }, // RES_1_B
		/* 89 */ () -> { regs.bc.setL(resetBit(1, regs.bc.l)); 					return 8  ; }, // RES_1_C
		/* 8A */ () -> { regs.de.setH(resetBit(1, regs.de.h)); 					return 8  ; }, // RES_1_D
		/* 8B */ () -> { regs.de.setL(resetBit(1, regs.de.l)); 					return 8  ; }, // RES_1_E
		/* 8C */ () -> { regs.hl.setH(resetBit(1, regs.hl.h)); 					return 8  ; }, // RES_1_H
		/* 8D */ () -> { regs.hl.setL(resetBit(1, regs.hl.l)); 					return 8  ; }, // RES_1_L
		/* 8E */ () -> { writeByte(regs.hl.w, resetBit(1, readMem(regs.hl.w))); return 15 ; }, // RES_1_XHL
		/* 8F */ () -> { regs.a = resetBit(1, regs.a); 							return 8  ; }, // RES_1_A
		/* 90 */ () -> { regs.bc.setH(resetBit(2, regs.bc.h)); 					return 8  ; }, // RES_2_B
		/* 91 */ () -> { regs.bc.setL(resetBit(2, regs.bc.l)); 					return 8  ; }, // RES_2_C
		/* 92 */ () -> { regs.de.setH(resetBit(2, regs.de.h)); 					return 8  ; }, // RES_2_D
		/* 93 */ () -> { regs.de.setL(resetBit(2, regs.de.l)); 					return 8  ; }, // RES_2_E
		/* 94 */ () -> { regs.hl.setH(resetBit(2, regs.hl.h)); 					return 8  ; }, // RES_2_H
		/* 95 */ () -> { regs.hl.setL(resetBit(2, regs.hl.l)); 					return 8  ; }, // RES_2_L
		/* 96 */ () -> { writeByte(regs.hl.w, resetBit(2, readMem(regs.hl.w))); return 15 ; }, // RES_2_XHL
		/* 97 */ () -> { regs.a = resetBit(2, regs.a); 							return 8  ; }, // RES_2_A
		/* 98 */ () -> { regs.bc.setH(resetBit(3, regs.bc.h)); 					return 8  ; }, // RES_3_B
		/* 99 */ () -> { regs.bc.setL(resetBit(3, regs.bc.l)); 					return 8  ; }, // RES_3_C
		/* 9A */ () -> { regs.de.setH(resetBit(3, regs.de.h)); 					return 8  ; }, // RES_3_D
		/* 9B */ () -> { regs.de.setL(resetBit(3, regs.de.l)); 					return 8  ; }, // RES_3_E
		/* 9C */ () -> { regs.hl.setH(resetBit(3, regs.hl.h)); 					return 8  ; }, // RES_3_H
		/* 9D */ () -> { regs.hl.setL(resetBit(3, regs.hl.l)); 					return 8  ; }, // RES_3_L
		/* 9E */ () -> { writeByte(regs.hl.w, resetBit(3, readMem(regs.hl.w))); return 15 ; }, // RES_3_XHL
		/* 9F */ () -> { regs.a = resetBit(3, regs.a); 							return 8  ; }, // RES_3_A
		/* A0 */ () -> { regs.bc.setH(resetBit(4, regs.bc.h)); 					return 8  ; }, // RES_4_B
		/* A1 */ () -> { regs.bc.setL(resetBit(4, regs.bc.l)); 					return 8  ; }, // RES_4_C
		/* A2 */ () -> { regs.de.setH(resetBit(4, regs.de.h)); 					return 8  ; }, // RES_4_D
		/* A3 */ () -> { regs.de.setL(resetBit(4, regs.de.l)); 					return 8  ; }, // RES_4_E
		/* A4 */ () -> { regs.hl.setH(resetBit(4, regs.hl.h)); 					return 8  ; }, // RES_4_H
		/* A5 */ () -> { regs.hl.setL(resetBit(4, regs.hl.l)); 					return 8  ; }, // RES_4_L
		/* A6 */ () -> { writeByte(regs.hl.w, resetBit(4, readMem(regs.hl.w))); return 15 ; }, // RES_4_XHL
		/* A7 */ () -> { regs.a = resetBit(4, regs.a); 							return 8  ; }, // RES_4_A
		/* A8 */ () -> { regs.bc.setH(resetBit(5, regs.bc.h)); 					return 8  ; }, // RES_5_B
		/* A9 */ () -> { regs.bc.setL(resetBit(5, regs.bc.l)); 					return 8  ; }, // RES_5_C
		/* AA */ () -> { regs.de.setH(resetBit(5, regs.de.h)); 					return 8  ; }, // RES_5_D
		/* AB */ () -> { regs.de.setL(resetBit(5, regs.de.l)); 					return 8  ; }, // RES_5_E
		/* AC */ () -> { regs.hl.setH(resetBit(5, regs.hl.h)); 					return 8  ; }, // RES_5_H
		/* AD */ () -> { regs.hl.setL(resetBit(5, regs.hl.l)); 					return 8  ; }, // RES_5_L
		/* AE */ () -> { writeByte(regs.hl.w, resetBit(5, readMem(regs.hl.w))); return 15 ; }, // RES_5_XHL
		/* AF */ () -> { regs.a = resetBit(5, regs.a); 						    return 8  ; }, // RES_5_A
		/* B0 */ () -> { regs.bc.setH(resetBit(6, regs.bc.h)); 					return 8  ; }, // RES_6_B
		/* B1 */ () -> { regs.bc.setL(resetBit(6, regs.bc.l)); 					return 8  ; }, // RES_6_C
		/* B2 */ () -> { regs.de.setH(resetBit(6, regs.de.h)); 					return 8  ; }, // RES_6_D
		/* B3 */ () -> { regs.de.setL(resetBit(6, regs.de.l)); 					return 8  ; }, // RES_6_E
		/* B4 */ () -> { regs.hl.setH(resetBit(6, regs.hl.h)); 					return 8  ; }, // RES_6_H
		/* B5 */ () -> { regs.hl.setL(resetBit(6, regs.hl.l)); 					return 8  ; }, // RES_6_L
		/* B6 */ () -> { writeByte(regs.hl.w, resetBit(6, readMem(regs.hl.w))); return 15 ; }, // RES_6_XHL
		/* B7 */ () -> { regs.a = resetBit(6, regs.a); 							return 8  ; }, // RES_6_A
		/* B8 */ () -> { regs.bc.setH(resetBit(7, regs.bc.h)); 					return 8  ; }, // RES_7_B
		/* B9 */ () -> { regs.bc.setL(resetBit(7, regs.bc.l)); 					return 8  ; }, // RES_7_C
		/* BA */ () -> { regs.de.setH(resetBit(7, regs.de.h)); 					return 8  ; }, // RES_7_D
		/* BB */ () -> { regs.de.setL(resetBit(7, regs.de.l)); 					return 8  ; }, // RES_7_E
		/* BC */ () -> { regs.hl.setH(resetBit(7, regs.hl.h)); 					return 8  ; }, // RES_7_H
		/* BD */ () -> { regs.hl.setL(resetBit(7, regs.hl.l)); 					return 8  ; }, // RES_7_L
		/* BE */ () -> { writeByte(regs.hl.w, resetBit(7, readMem(regs.hl.w))); return 15 ; }, // RES_7_XHL
		/* BF */ () -> { regs.a = resetBit(7, regs.a); 							return 8  ; }, // RES_7_A
		/* C0 */ () -> { regs.bc.setH(setBit(0, regs.bc.h)); 					return 8  ; }, // SET_0_B
		/* C1 */ () -> { regs.bc.setL(setBit(0, regs.bc.l)); 					return 8  ; }, // SET_0_C
		/* C2 */ () -> { regs.de.setH(setBit(0, regs.de.h)); 					return 8  ; }, // SET_0_D
		/* C3 */ () -> { regs.de.setL(setBit(0, regs.de.l)); 					return 8  ; }, // SET_0_E
		/* C4 */ () -> { regs.hl.setH(setBit(0, regs.hl.h)); 					return 8  ; }, // SET_0_H
		/* C5 */ () -> { regs.hl.setL(setBit(0, regs.hl.l)); 					return 8  ; }, // SET_0_L
		/* C6 */ () -> { writeByte(regs.hl.w, setBit(0, readMem(regs.hl.w)));   return 15 ; }, // SET_0_XHL
		/* C7 */ () -> { regs.a = setBit(0, regs.a); 							return 8  ; }, // SET_0_A
		/* C8 */ () -> { regs.bc.setH(setBit(1, regs.bc.h)); 					return 8  ; }, // SET_1_B
		/* C9 */ () -> { regs.bc.setL(setBit(1, regs.bc.l)); 					return 8  ; }, // SET_1_C
		/* CA */ () -> { regs.de.setH(setBit(1, regs.de.h)); 					return 8  ; }, // SET_1_D
		/* CB */ () -> { regs.de.setL(setBit(1, regs.de.l)); 					return 8  ; }, // SET_1_E
		/* CC */ () -> { regs.hl.setH(setBit(1, regs.hl.h)); 					return 8  ; }, // SET_1_H
		/* CD */ () -> { regs.hl.setL(setBit(1, regs.hl.l)); 					return 8  ; }, // SET_1_L
		/* CE */ () -> { writeByte(regs.hl.w, setBit(1, readMem(regs.hl.w)));   return 15 ; }, // SET_1_XHL
		/* CF */ () -> { regs.a = setBit(1, regs.a); 							return 8  ; }, // SET_1_A
		/* D0 */ () -> { regs.bc.setH(setBit(2, regs.bc.h)); 					return 8  ; }, // SET_2_B
		/* D1 */ () -> { regs.bc.setL(setBit(2, regs.bc.l)); 					return 8  ; }, // SET_2_C
		/* D2 */ () -> { regs.de.setH(setBit(2, regs.de.h)); 					return 8  ; }, // SET_2_D
		/* D3 */ () -> { regs.de.setL(setBit(2, regs.de.l)); 					return 8  ; }, // SET_2_E
		/* D4 */ () -> { regs.hl.setH(setBit(2, regs.hl.h)); 					return 8  ; }, // SET_2_H
		/* D5 */ () -> { regs.hl.setL(setBit(2, regs.hl.l)); 					return 8  ; }, // SET_2_L
		/* D6 */ () -> { writeByte(regs.hl.w, setBit(2, readMem(regs.hl.w)));   return 15 ; }, // SET_2_XHL
		/* D7 */ () -> { regs.a = setBit(2, regs.a); 							return 8  ; }, // SET_2_A
		/* D8 */ () -> { regs.bc.setH(setBit(3, regs.bc.h)); 					return 8  ; }, // SET_3_B
		/* D9 */ () -> { regs.bc.setL(setBit(3, regs.bc.l)); 					return 8  ; }, // SET_3_C
		/* DA */ () -> { regs.de.setH(setBit(3, regs.de.h)); 					return 8  ; }, // SET_3_D
		/* DB */ () -> { regs.de.setL(setBit(3, regs.de.l)); 					return 8  ; }, // SET_3_E
		/* DC */ () -> { regs.hl.setH(setBit(3, regs.hl.h)); 					return 8  ; }, // SET_3_H
		/* DD */ () -> { regs.hl.setL(setBit(3, regs.hl.l)); 					return 8  ; }, // SET_3_L
		/* DE */ () -> { writeByte(regs.hl.w, setBit(3, readMem(regs.hl.w)));   return 15 ; }, // SET_3_XHL
		/* DF */ () -> { regs.a = setBit(3, regs.a); 							return 8  ; }, // SET_3_A
		/* E0 */ () -> { regs.bc.setH(setBit(4, regs.bc.h)); 					return 8  ; }, // SET_4_B
		/* E1 */ () -> { regs.bc.setL(setBit(4, regs.bc.l)); 					return 8  ; }, // SET_4_C
		/* E2 */ () -> { regs.de.setH(setBit(4, regs.de.h)); 					return 8  ; }, // SET_4_D
		/* E3 */ () -> { regs.de.setL(setBit(4, regs.de.l)); 					return 8  ; }, // SET_4_E
		/* E4 */ () -> { regs.hl.setH(setBit(4, regs.hl.h)); 					return 8  ; }, // SET_4_H
		/* E5 */ () -> { regs.hl.setL(setBit(4, regs.hl.l)); 					return 8  ; }, // SET_4_L
		/* E6 */ () -> { writeByte(regs.hl.w, setBit(4, readMem(regs.hl.w)));   return 15 ; }, // SET_4_XHL
		/* E7 */ () -> { regs.a = setBit(4, regs.a); 							return 8  ; }, // SET_4_A
		/* E8 */ () -> { regs.bc.setH(setBit(5, regs.bc.h)); 					return 8  ; }, // SET_5_B
		/* E9 */ () -> { regs.bc.setL(setBit(5, regs.bc.l)); 					return 8  ; }, // SET_5_C
		/* EA */ () -> { regs.de.setH(setBit(5, regs.de.h)); 					return 8  ; }, // SET_5_D
		/* EB */ () -> { regs.de.setL(setBit(5, regs.de.l)); 					return 8  ; }, // SET_5_E
		/* EC */ () -> { regs.hl.setH(setBit(5, regs.hl.h)); 					return 8  ; }, // SET_5_H
		/* ED */ () -> { regs.hl.setL(setBit(5, regs.hl.l)); 					return 8  ; }, // SET_5_L
		/* EE */ () -> { writeByte(regs.hl.w, setBit(5, readMem(regs.hl.w)));   return 15 ; }, // SET_5_XHL
		/* EF */ () -> { regs.a = setBit(5, regs.a); 							return 8  ; }, // SET_5_A
		/* F0 */ () -> { regs.bc.setH(setBit(6, regs.bc.h)); 					return 8  ; }, // SET_6_B
		/* F1 */ () -> { regs.bc.setL(setBit(6, regs.bc.l)); 					return 8  ; }, // SET_6_C
		/* F2 */ () -> { regs.de.setH(setBit(6, regs.de.h)); 					return 8  ; }, // SET_6_D
		/* F3 */ () -> { regs.de.setL(setBit(6, regs.de.l)); 					return 8  ; }, // SET_6_E
		/* F4 */ () -> { regs.hl.setH(setBit(6, regs.hl.h)); 					return 8  ; }, // SET_6_H
		/* F5 */ () -> { regs.hl.setL(setBit(6, regs.hl.l)); 					return 8  ; }, // SET_6_L
		/* F6 */ () -> { writeByte(regs.hl.w, setBit(6, readMem(regs.hl.w))); 	return 15 ; }, // SET_6_XHL
		/* F7 */ () -> { regs.a = setBit(6, regs.a); 							return 8  ; }, // SET_6_A
		/* F8 */ () -> { regs.bc.setH(setBit(7, regs.bc.h)); 					return 8  ; }, // SET_7_B
		/* F9 */ () -> { regs.bc.setL(setBit(7, regs.bc.l)); 					return 8  ; }, // SET_7_C
		/* FA */ () -> { regs.de.setH(setBit(7, regs.de.h)); 					return 8  ; }, // SET_7_D
		/* FB */ () -> { regs.de.setL(setBit(7, regs.de.l)); 					return 8  ; }, // SET_7_E
		/* FC */ () -> { regs.hl.setH(setBit(7, regs.hl.h)); 					return 8  ; }, // SET_7_H
		/* FD */ () -> { regs.hl.setL(setBit(7, regs.hl.l)); 					return 8  ; }, // SET_7_L
		/* FE */ () -> { writeByte(regs.hl.w, setBit(7, readMem(regs.hl.w))); 	return 15 ; }, // SET_7_XHL
		/* FF */ () -> { regs.a = setBit(7, regs.a); 							return 8  ; }, // SET_7_A
	};

	private final Op[] dd = {
			null,null,null,null,null,null,null,null,null,
		/* 09 */ () -> { regs.ix.setW(addWord(regs.ix.w, regs.bc.w)); 			return 15 ; }, // ADD_IX_BC
			null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
		/* 19 */ () -> { regs.ix.setW(addWord(regs.ix.w, regs.de.w)); 			return 15 ; }, // ADD_IX_DE
			null,null,null,null,null,null,null,
		/* 21 */ () -> { regs.ix.setW(readWord()); 								return 14 ; }, // LD_IX_WORD
		/* 22 */ () -> { writeWord(readWord(), regs.ix.w); 						return 20 ; }, // LD_XWORD_IX
		/* 23 */ () -> { regs.ix.addW(1); 										return 10 ; }, // INC_IX
			null,null,
		/* 26 */ () -> { regs.ix.setW(readWord(readWord())); 					return 9  ; }, // LD_IXH_BYTE
			null,null,
		/* 29 */ () -> { regs.ix.setW(addWord(regs.ix.w, regs.ix.w)); 			return 15 ; }, // ADD_IX_IX
		/* 2A */ () -> { regs.ix.setW(readWord(readWord())); 					return 20 ; }, // LD_IX_XWORD
		/* 2B */ () -> { regs.ix.addW(-1); 										return 10 ; }, // DEC_IX
		/* 2C */ () -> { regs.ix.setL(inc(regs.ix.l)); 							return 9  ; }, // INC_IXL
		/* 2D */ () -> { regs.ix.setL(dec(regs.ix.l)); 							return 9  ; }, // DEC_IXL
		/* 2E */ () -> { regs.ix.setL(readByte()); 								return 9  ; }, // LD_IXL_BYTE
			null,null,null,null,null,
		/* 34 */ () -> { int j = xix(); writeByte(j, inc(readMem(j))); 			return 23 ; }, // INC_XIX
		/* 35 */ () -> { int j = xix(); writeByte(j, dec(readMem(j))); 			return 23 ; }, // DEC_XIX
		/* 36 */ () -> { writeByte(xix(), readByte()); 							return 19 ; }, // LD_XIX_BYTE
			null,null,
		/* 39 */ () -> { regs.ix.setW(addWord(regs.ix.w, regs.sp)); 			return 15 ; }, // ADD_IX_SP
			null,null,null,null,null,null,null,null,null,null,null,null,
		/* 46 */ () -> { regs.bc.setH(readXIX()); 								return 19 ; }, // LD_B_XIX
			null,null,null,null,null,
		/* 4C */ () -> { regs.bc.setL(regs.ix.h); 								return 9  ; }, // LD_C_IXH
			null,
		/* 4E */ () -> { regs.bc.setL(readXIX()); 								return 19 ; }, // LD_C_XIX
			null,null,null,null,null,null,null,
		/* 56 */ () -> { regs.de.setH(readXIX()); 								return 19 ; }, // LD_D_XIX
			null,null,null,null,null,null,
		/* 5D */ () -> { regs.de.setL(regs.ix.l); 								return 9  ; }, // LD_E_IXL
		/* 5E */ () -> { regs.de.setL(readXIX()); 								return 19 ; }, // LD_E_XIX
			null,
		/* 60 */ () -> { regs.ix.setH(regs.bc.h); 								return 9  ; }, // LD_IXH_B
		/* 61 */ () -> { regs.ix.setH(regs.bc.l); 								return 9  ; }, // LD_IXH_C
		/* 62 */ () -> { regs.ix.setH(regs.de.h); 								return 9  ; }, // LD_IXH_D
		/* 63 */ () -> { regs.ix.setH(regs.de.l); 								return 9  ; }, // LD_IXH_E
			null,null,
		/* 66 */ () -> { regs.hl.setH(readXIX()); 								return 9  ; }, // LD_H_XIX
		/* 67 */ () -> { regs.ix.setH(regs.a); 									return 9  ; }, // LD_IXH_A
			null,null,null,null,null,null,
		/* 6E */ () -> { regs.hl.setL(readXIX()); 								return 9  ; }, // LD_L_XIX
		/* 6F */ () -> { regs.ix.setL(regs.a); 									return 9  ; }, // LD_IXL_A
		/* 70 */ () -> { writeXIX(regs.bc.h); 									return 19 ; }, // LD_XIX_B
		/* 71 */ () -> { writeXIX(regs.bc.l); 									return 19 ; }, // LD_XIX_C
		/* 72 */ () -> { writeXIX(regs.de.h); 									return 19 ; }, // LD_XIX_D
		/* 73 */ () -> { writeXIX(regs.de.l); 									return 19 ; }, // LD_XIX_E
		/* 74 */ () -> { writeXIX(regs.hl.h); 									return 19 ; }, // LD_XIX_H
		/* 75 */ () -> { writeXIX(regs.hl.l); 									return 19 ; }, // LD_XIX_L
			null,
		/* 77 */ () -> { writeXIX(regs.a); 										return 19 ; }, // LD_XIX_A
			null,null,null,null,
		/* 7C */ () -> { regs.a = regs.ix.h; 									return 9  ; }, // LD_A_IXH
		/* 7D */ () -> { regs.a = regs.ix.l; 									return 9  ; }, // LD_A_IXL
		/* 7E */ () -> { regs.a = readXIX(); 									return 19 ; }, // LD_A_XIX
			null,null,null,null,null,
		/* 84 */ () -> { add(regs.ix.h); 										return 9  ; }, // ADD_A_IXH
		/* 85 */ () -> { add(regs.ix.l); 										return 9  ; }, // ADD_A_IXL
		/* 86 */ () -> { add(readXIX()); 										return 19 ; }, // ADD_A_XIX
			null,null,null,null,null,null,null,
		/* 8E */ () -> { addCarry(readXIX()); 									return 19 ; }, // ADC_A_XIX
			null,null,null,null,null,
		/* 94 */ () -> { sub(regs.ix.h); 										return 9  ; }, // SUB_IXH
		/* 95 */ () -> { sub(regs.ix.l); 										return 9  ; }, // SUB_IXL
		/* 96 */ () -> { sub(readXIX()); 										return 19 ; }, // SUB_XIX
			null,null,null,null,null,null,null,
		/* 9E */ () -> { subCarry(readXIX()); 									return 19 ; }, // SBC_A_XIX
			null,null,null,null,null,null,null,
		/* A6 */ () -> { and(readXIX());										return 19 ; }, // AND_XIX
			null,null,null,null,null,
		/* AC */ () -> { xor(regs.ix.h); 										return 9  ; }, // XOR_IXH
			null,
		/* AE */ () -> { xor(readXIX()); 										return 19 ; }, // XOR_XIX
			null,null,null,null,null,null,null,
		/* B6 */ () -> { or(readXIX()); 										return 19 ; }, // OR_XIX
			null,null,null,null,null,null,null,
		/* BE */ () -> { compare(readXIX()); 									return 19 ; }, // CP_XIX
			null,null,null,null,null,null,
			null,null,null,null,null,null,
		/* CB */ () -> { ddcb(); 												return 0  ; }, // DD_CB
			null,null,null,null,null,null,null,
			null,null,null,null,null,null,null,
			null,null,null,null,null,null,null,
		/* E1 */ () -> { regs.ix.setW(pop()); 									return 14 ; }, // POP_IX
			null,
		/* E3 */ () -> { exXspIX(); 											return 23 ; }, // EX_XSP_IX
			null,
		/* E5 */ () -> { push(regs.ix.w); 										return 15 ; }, // PUSH_IX
			null,null,null,
		/* E9 */ () -> { regs.pc = regs.ix.w; 									return 8  ; }, // JP_IX
	};

	private final Op[] ddcb = {
		/* 00 */ 		 notImplemented													   	 , //
		/* 01 */ 		 notImplemented													   	 , //
		/* 02 */ 		 notImplemented													   	 , //
		/* 03 */ 		 notImplemented													   	 , //
		/* 04 */ 		 notImplemented													   	 , //
		/* 05 */ 		 notImplemented													   	 , //
		/* 06 */ () -> { int j=xix(); writeByte(j, rlc(readMem(j)));    		return 23 ; }, // RLC_XIX
		/* 07 */ 		 notImplemented														 , //
		/* 08 */ 		 notImplemented														 , //
		/* 09 */ 		 notImplemented														 , //
		/* 0A */ 		 notImplemented														 , //
		/* 0B */ 		 notImplemented														 , //
		/* 0C */ 		 notImplemented														 , //
		/* 0D */ 		 notImplemented														 , //
		/* 0E */ () -> { int j=xix(); writeByte(j, rrc(readMem(j)));    		return 23 ; }, // RRC_XIX
		/* 0F */ 		 notImplemented														 , //
		/* 10 */ 		 notImplemented														 , //
		/* 11 */ 		 notImplemented														 , //
		/* 12 */ 		 notImplemented														 , //
		/* 13 */ 		 notImplemented														 , //
		/* 14 */ 		 notImplemented														 , //
		/* 15 */ 		 notImplemented														 , //
		/* 16 */ () -> { int j=xix(); writeByte(j, rl(readMem(j)));     		return 23 ; }, // RL_XIX
		/* 17 */ 		 notImplemented														 , //
		/* 18 */ 		 notImplemented														 , //
		/* 19 */ 		 notImplemented														 , //
		/* 1A */ 		 notImplemented														 , //
		/* 1B */ 		 notImplemented														 , //
		/* 1C */ 		 notImplemented														 , //
		/* 1D */ 		 notImplemented														 , //
		/* 1E */ () -> { int j=xix(); writeByte(j, rr(readMem(j)));     		return 23 ; }, // RR_XIX
		/* 1F */ 		 notImplemented														 , //
		/* 20 */ 		 notImplemented														 , //
		/* 21 */ 		 notImplemented														 , //
		/* 22 */ 		 notImplemented														 , //
		/* 23 */ 		 notImplemented														 , //
		/* 24 */ 		 notImplemented														 , //
		/* 25 */ 		 notImplemented														 , //
		/* 26 */ () -> { int j=xix(); writeByte(j, sla(readMem(j)));    		return 23 ; }, // SLA_XIX
		/* 27 */ 		 notImplemented														 , //
		/* 28 */ 		 notImplemented														 , //
		/* 29 */ 		 notImplemented														 , //
		/* 2A */ 		 notImplemented														 , //
		/* 2B */ 		 notImplemented														 , //
		/* 2C */ 		 notImplemented														 , //
		/* 2D */ 		 notImplemented														 , //
		/* 2E */ 		 notImplemented														 , //
		/* 2F */ 		 notImplemented														 , //
		/* 30 */ 		 notImplemented														 , //
		/* 31 */ 		 notImplemented														 , //
		/* 32 */ 		 notImplemented														 , //
		/* 33 */ 		 notImplemented														 , //
		/* 34 */ 		 notImplemented														 , //
		/* 35 */ 		 notImplemented														 , //
		/* 36 */ 		 notImplemented														 , //
		/* 37 */ 		 notImplemented														 , //
		/* 38 */ 		 notImplemented														 , //
		/* 39 */ 		 notImplemented														 , //
		/* 3A */ 		 notImplemented														 , //
		/* 3B */ 		 notImplemented														 , //
		/* 3C */ 		 notImplemented														 , //
		/* 3D */ 		 notImplemented														 , //
		/* 3E */ () -> { int j=xix(); writeByte(j, slr(readMem(j)));    		return 23 ; }, // SRL_XIX
		/* 3F */ 		 notImplemented														 , //
		/* 40 */ () -> { testBit(0, readXIX());                         		return 20 ; }, // BIT_0_XIX
		/* 41 */ () -> { testBit(0, readXIX());                         		return 20 ; }, // BIT_0_XIX
		/* 42 */ () -> { testBit(0, readXIX());                         		return 20 ; }, // BIT_0_XIX
		/* 43 */ () -> { testBit(0, readXIX());                         		return 20 ; }, // BIT_0_XIX
		/* 44 */ () -> { testBit(0, readXIX());                         		return 20 ; }, // BIT_0_XIX
		/* 45 */ () -> { testBit(0, readXIX());                         		return 20 ; }, // BIT_0_XIX
		/* 46 */ () -> { testBit(0, readXIX());                         		return 20 ; }, // BIT_0_XIX
		/* 47 */ () -> { testBit(0, readXIX());                         		return 20 ; }, // BIT_0_XIX
		/* 48 */ () -> { testBit(1, readXIX());                         		return 20 ; }, // BIT_1_XIX
		/* 49 */ () -> { testBit(1, readXIX());                         		return 20 ; }, // BIT_1_XIX
		/* 4A */ () -> { testBit(1, readXIX());                         		return 20 ; }, // BIT_1_XIX
		/* 4B */ () -> { testBit(1, readXIX());                         		return 20 ; }, // BIT_1_XIX
		/* 4C */ () -> { testBit(1, readXIX());                         		return 20 ; }, // BIT_1_XIX
		/* 4D */ () -> { testBit(1, readXIX());                         		return 20 ; }, // BIT_1_XIX
		/* 4E */ () -> { testBit(1, readXIX());                         		return 20 ; }, // BIT_1_XIX
		/* 4F */ () -> { testBit(1, readXIX());                         		return 20 ; }, // BIT_1_XIX
		/* 50 */ () -> { testBit(2, readXIX());                         		return 20 ; }, // BIT_2_XIX
		/* 51 */ () -> { testBit(2, readXIX());                         		return 20 ; }, // BIT_2_XIX
		/* 52 */ () -> { testBit(2, readXIX());                         		return 20 ; }, // BIT_2_XIX
		/* 53 */ () -> { testBit(2, readXIX());                         		return 20 ; }, // BIT_2_XIX
		/* 54 */ () -> { testBit(2, readXIX());                         		return 20 ; }, // BIT_2_XIX
		/* 55 */ () -> { testBit(2, readXIX());                         		return 20 ; }, // BIT_2_XIX
		/* 56 */ () -> { testBit(2, readXIX());                         		return 20 ; }, // BIT_2_XIX
		/* 57 */ () -> { testBit(2, readXIX());                         		return 20 ; }, // BIT_2_XIX
		/* 58 */ () -> { testBit(3, readXIX());                         		return 20 ; }, // BIT_3_XIX
		/* 59 */ () -> { testBit(3, readXIX());                         		return 20 ; }, // BIT_3_XIX
		/* 5A */ () -> { testBit(3, readXIX());                         		return 20 ; }, // BIT_3_XIX
		/* 5B */ () -> { testBit(3, readXIX());                         		return 20 ; }, // BIT_3_XIX
		/* 5C */ () -> { testBit(3, readXIX());                         		return 20 ; }, // BIT_3_XIX
		/* 5D */ () -> { testBit(3, readXIX());                         		return 20 ; }, // BIT_3_XIX
		/* 5E */ () -> { testBit(3, readXIX());                         		return 20 ; }, // BIT_3_XIX
		/* 5F */ () -> { testBit(3, readXIX());                         		return 20 ; }, // BIT_3_XIX
		/* 60 */ () -> { testBit(4, readXIX());                         		return 20 ; }, // BIT_4_XIX
		/* 61 */ () -> { testBit(4, readXIX());                         		return 20 ; }, // BIT_4_XIX
		/* 62 */ () -> { testBit(4, readXIX());                         		return 20 ; }, // BIT_4_XIX
		/* 63 */ () -> { testBit(4, readXIX());                         		return 20 ; }, // BIT_4_XIX
		/* 64 */ () -> { testBit(4, readXIX());                         		return 20 ; }, // BIT_4_XIX
		/* 65 */ () -> { testBit(4, readXIX());                         		return 20 ; }, // BIT_4_XIX
		/* 66 */ () -> { testBit(4, readXIX());                         		return 20 ; }, // BIT_4_XIX
		/* 67 */ () -> { testBit(4, readXIX());                         		return 20 ; }, // BIT_4_XIX
		/* 68 */ () -> { testBit(5, readXIX());                         		return 20 ; }, // BIT_5_XIX
		/* 69 */ () -> { testBit(5, readXIX());                         		return 20 ; }, // BIT_5_XIX
		/* 6A */ () -> { testBit(5, readXIX());                         		return 20 ; }, // BIT_5_XIX
		/* 6B */ () -> { testBit(5, readXIX());                         		return 20 ; }, // BIT_5_XIX
		/* 6C */ () -> { testBit(5, readXIX());                         		return 20 ; }, // BIT_5_XIX
		/* 6D */ () -> { testBit(5, readXIX());                         		return 20 ; }, // BIT_5_XIX
		/* 6E */ () -> { testBit(5, readXIX());                         		return 20 ; }, // BIT_5_XIX
		/* 6F */ () -> { testBit(5, readXIX());                         		return 20 ; }, // BIT_5_XIX
		/* 70 */ () -> { testBit(6, readXIX());                         		return 20 ; }, // BIT_6_XIX
		/* 71 */ () -> { testBit(6, readXIX());                         		return 20 ; }, // BIT_6_XIX
		/* 72 */ () -> { testBit(6, readXIX());                         		return 20 ; }, // BIT_6_XIX
		/* 73 */ () -> { testBit(6, readXIX());                         		return 20 ; }, // BIT_6_XIX
		/* 74 */ () -> { testBit(6, readXIX());                         		return 20 ; }, // BIT_6_XIX
		/* 75 */ () -> { testBit(6, readXIX());                         		return 20 ; }, // BIT_6_XIX
		/* 76 */ () -> { testBit(6, readXIX());                         		return 20 ; }, // BIT_6_XIX
		/* 77 */ () -> { testBit(6, readXIX());                         		return 20 ; }, // BIT_6_XIX
		/* 78 */ () -> { testBit(7, readXIX());                         		return 20 ; }, // BIT_7_XIX
		/* 79 */ () -> { testBit(7, readXIX());                         		return 20 ; }, // BIT_7_XIX
		/* 7A */ () -> { testBit(7, readXIX());                         		return 20 ; }, // BIT_7_XIX
		/* 7B */ () -> { testBit(7, readXIX());                         		return 20 ; }, // BIT_7_XIX
		/* 7C */ () -> { testBit(7, readXIX());                         		return 20 ; }, // BIT_7_XIX
		/* 7D */ () -> { testBit(7, readXIX());                         		return 20 ; }, // BIT_7_XIX
		/* 7E */ () -> { testBit(7, readXIX());                         		return 20 ; }, // BIT_7_XIX
		/* 7F */ () -> { testBit(7, readXIX());                         		return 20 ; }, // BIT_7_XIX
		/* 80 */ 		 notImplemented											 		     , //
		/* 81 */ 		 notImplemented														 , //
		/* 82 */ 		 notImplemented														 , //
		/* 83 */ 		 notImplemented														 , //
		/* 84 */ 		 notImplemented														 , //
		/* 85 */ 		 notImplemented														 , //
		/* 86 */ () -> { int j=xix(); writeByte(j, resetBit(0,readMem(j)));		return 23 ; }, // RES_0_XIX
		/* 87 */ 		 notImplemented														 , //
		/* 88 */ 		 notImplemented														 , //
		/* 89 */ 		 notImplemented														 , //
		/* 8A */ 		 notImplemented														 , //
		/* 8B */ 		 notImplemented														 , //
		/* 8C */ 		 notImplemented														 , //
		/* 8D */ 		 notImplemented														 , //
		/* 8E */ () -> { int j=xix(); writeByte(j, resetBit(1,readMem(j)));		return 23 ; }, // RES_1_XIX
		/* 8F */ 		 notImplemented														 , //
		/* 90 */ 		 notImplemented														 , //
		/* 91 */ 		 notImplemented														 , //
		/* 92 */ 		 notImplemented														 , //
		/* 93 */ 		 notImplemented														 , //
		/* 94 */ 		 notImplemented														 , //
		/* 95 */ 		 notImplemented														 , //
		/* 96 */ () -> { int j=xix(); writeByte(j, resetBit(2,readMem(j)));		return 23 ; }, // RES_2_XIX
		/* 97 */ 		 notImplemented														 , //
		/* 98 */ 		 notImplemented														 , //
		/* 99 */ 		 notImplemented														 , //
		/* 9A */ 		 notImplemented														 , //
		/* 9B */ 		 notImplemented														 , //
		/* 9C */ 		 notImplemented														 , //
		/* 9D */ 		 notImplemented														 , //
		/* 9E */ () -> { int j=xix(); writeByte(j, resetBit(3,readMem(j)));		return 23 ; }, // RES_3_XIX
		/* 9F */ 		 notImplemented														 , //
		/* A0 */ 		 notImplemented														 , //
		/* A1 */ 		 notImplemented														 , //
		/* A2 */ 		 notImplemented														 , //
		/* A3 */ 		 notImplemented														 , //
		/* A4 */ 		 notImplemented														 , //
		/* A5 */ 		 notImplemented														 , //
		/* A6 */ () -> { int j=xix(); writeByte(j, resetBit(4,readMem(j)));		return 23 ; }, // RES_4_XIX
		/* A7 */ 		 notImplemented														 , //
		/* A8 */ 		 notImplemented														 , //
		/* A9 */ 		 notImplemented														 , //
		/* AA */ 		 notImplemented														 , //
		/* AB */ 		 notImplemented														 , //
		/* AC */ 		 notImplemented														 , //
		/* AD */ 		 notImplemented														 , //
		/* AE */ () -> { int j=xix(); writeByte(j, resetBit(5,readMem(j)));		return 23 ; }, // RES_5_XIX
		/* AF */ 		 notImplemented														 , //
		/* B0 */ 		 notImplemented														 , //
		/* B1 */ 		 notImplemented														 , //
		/* B2 */ 		 notImplemented														 , //
		/* B3 */ 		 notImplemented														 , //
		/* B4 */ 		 notImplemented														 , //
		/* B5 */ 		 notImplemented														 , //
		/* B6 */ () -> { int j=xix(); writeByte(j, resetBit(6,readMem(j)));		return 23 ; }, // RES_6_XIX
		/* B7 */ 		 notImplemented														 , //
		/* B8 */ 		 notImplemented														 , //
		/* B9 */ 		 notImplemented														 , //
		/* BA */ 		 notImplemented														 , //
		/* BB */ 		 notImplemented														 , //
		/* BC */ 		 notImplemented														 , //
		/* BD */ 		 notImplemented														 , //
		/* BE */ () -> { int j=xix(); writeByte(j, resetBit(7,readMem(j)));		return 23 ; }, // RES_7_XIX
		/* BF */ 		 notImplemented														 , //
		/* C0 */ 		 notImplemented														 , //
		/* C1 */ 		 notImplemented														 , //
		/* C2 */ 		 notImplemented														 , //
		/* C3 */ 		 notImplemented														 , //
		/* C4 */ 		 notImplemented														 , //
		/* C5 */ 		 notImplemented														 , //
		/* C6 */ () -> { int j=xix(); writeByte(j, setBit(0, readMem(j)));		return 23 ; }, // SET_0_XIX
		/* C7 */ 		 notImplemented														 , //
		/* C8 */ 		 notImplemented														 , //
		/* C9 */ 		 notImplemented														 , //
		/* CA */ 		 notImplemented														 , //
		/* CB */ 		 notImplemented														 , //
		/* CC */ 		 notImplemented														 , //
		/* CD */ 		 notImplemented														 , //
		/* CE */ () -> { int j=xix(); writeByte(j, setBit(1, readMem(j)));		return 23 ; }, // SET_1_XIX
		/* CF */ 		 notImplemented														 , //
		/* D0 */ 		 notImplemented														 , //
		/* D1 */ 		 notImplemented														 , //
		/* D2 */ 		 notImplemented														 , //
		/* D3 */ 		 notImplemented														 , //
		/* D4 */ 		 notImplemented														 , //
		/* D5 */ 		 notImplemented														 , //
		/* D6 */ () -> { int j=xix(); writeByte(j,setBit(2,readMem(j)));		return 23 ; }, // SET_2_XIX
		/* D7 */ 		 notImplemented														 , //
		/* D8 */ 		 notImplemented														 , //
		/* D9 */ 		 notImplemented														 , //
		/* DA */ 		 notImplemented														 , //
		/* DB */ 		 notImplemented														 , //
		/* DC */ 		 notImplemented														 , //
		/* DD */ 		 notImplemented														 , //
		/* DE */ () -> { int j=xix(); writeByte(j, setBit(3, readMem(j)));		return 23 ; }, // SET_3_XIX
		/* DF */ 		 notImplemented														 , //
		/* E0 */ 		 notImplemented														 , //
		/* E1 */ 		 notImplemented														 , //
		/* E2 */ 		 notImplemented														 , //
		/* E3 */ 		 notImplemented														 , //
		/* E4 */ 		 notImplemented														 , //
		/* E5 */ 		 notImplemented														 , //
		/* E6 */ () -> { int j=xix(); writeByte(j, setBit(4, readMem(j)));		return 23 ; }, // SET_4_XIX
		/* E7 */ 		 notImplemented														 , //
		/* E8 */ 		 notImplemented														 , //
		/* E9 */ 		 notImplemented														 , //
		/* EA */ 		 notImplemented														 , //
		/* EB */ 		 notImplemented														 , //
		/* EC */ 		 notImplemented														 , //
		/* ED */ 		 notImplemented														 , //
		/* EE */ () -> { int j=xix(); writeByte(j, setBit(5, readMem(j)));		return 3  ; }, // SET_5_XIX
		/* EF */ 		 notImplemented														 , //
		/* F0 */ 		 notImplemented														 , //
		/* F1 */ 		 notImplemented														 , //
		/* F2 */ 		 notImplemented														 , //
		/* F3 */ 		 notImplemented														 , //
		/* F4 */ 		 notImplemented														 , //
		/* F5 */ 		 notImplemented														 , //
		/* F6 */ () -> { int j=xix(); writeByte(j, setBit(6, readMem(j)));		return 23 ; }, // SET_6_XIX
		/* F7 */ 		 notImplemented														 , //
		/* F8 */ 		 notImplemented														 , //
		/* F9 */ 		 notImplemented														 , //
		/* FA */ 		 notImplemented														 , //
		/* FB */ 		 notImplemented														 , //
		/* FC */ 		 notImplemented														 , //
		/* FD */ 		 notImplemented														 , //
		/* FE */ () -> { int j=xix(); writeByte(j, setBit(7, readMem(j)));		return 23 ; }, // SET_7_XIX
		/* FF */ 		 notImplemented														 , //
	};

	private final Op[] fd = {
		/* 00 */ 		 notImplemented														 , //
		/* 01 */ 		 notImplemented														 , //
		/* 02 */ 		 notImplemented														 , //
		/* 03 */ 		 notImplemented														 , //
		/* 04 */ 		 notImplemented														 , //
		/* 05 */ 		 notImplemented														 , //
		/* 06 */ 		 notImplemented														 , //
		/* 07 */ 		 notImplemented														 , //
		/* 08 */ 		 notImplemented														 , //
		/* 09 */ () -> { regs.iy.setW(addWord(regs.iy.w, regs.bc.w)); 			return 15 ; }, // ADD_IY_BC
		/* 0A */ 		 notImplemented														 , //
		/* 0B */ 		 notImplemented														 , //
		/* 0C */ 		 notImplemented														 , //
		/* 0D */ 		 notImplemented														 , //
		/* 0E */ 		 notImplemented														 , //
		/* 0F */ 		 notImplemented														 , //
		/* 10 */ 		 notImplemented														 , //
		/* 11 */ 		 notImplemented														 , //
		/* 12 */ 		 notImplemented														 , //
		/* 13 */ 		 notImplemented														 , //
		/* 14 */ 		 notImplemented														 , //
		/* 15 */ 		 notImplemented														 , //
		/* 16 */ 		 notImplemented														 , //
		/* 17 */ 		 notImplemented														 , //
		/* 18 */ 		 notImplemented														 , //
		/* 19 */ () -> { regs.iy.setW(addWord(regs.iy.w, regs.de.w)); 			return 15 ; }, // ADD_IY_DE
		/* 1A */ 		 notImplemented														 , //
		/* 1B */ 		 notImplemented														 , //
		/* 1C */ 		 notImplemented														 , //
		/* 1D */ 		 notImplemented														 , //
		/* 1E */ 		 notImplemented														 , //
		/* 1F */ 		 notImplemented														 , //
		/* 20 */ 		 notImplemented														 , //
		/* 21 */ () -> { regs.iy.setW(readWord()); 								return 14 ; }, // LD_IY_WORD
		/* 22 */ () -> { writeWord(readWord(), regs.iy.w); 						return 20 ; }, // LD_XWORD_IY
		/* 23 */ () -> { regs.iy.addW(1); 										return 10 ; }, // INC_IY
		/* 24 */ 		 notImplemented														 , //
		/* 25 */ () -> { regs.iy.setH(dec(regs.iy.h)); 							return 9  ; }, // DEC_IYH
		/* 26 */ () -> { regs.iy.setH(readByte()); 								return 9  ; }, // LD_IYH_BYTE
		/* 27 */ 		 notImplemented														 , //
		/* 28 */ 		 notImplemented														 , //
		/* 29 */ 		 notImplemented														 , //
		/* 2A */ () -> { regs.iy.setW(readWord(readWord())); 					return 20 ; }, // LD_IY_XWORD
		/* 2B */ () -> { regs.iy.addW(-1); 										return 10 ; }, // DEC_IY
		/* 2C */ 		 notImplemented														 , //
		/* 2D */ () -> { regs.iy.setL(dec(regs.iy.l));				 			return 9  ; }, // DEC_IYL
		/* 2E */ () -> { regs.iy.setL(readByte()); 								return 9  ; }, // LD_IYL_BYTE
		/* 2F */ 		 notImplemented														 , //
		/* 30 */ 		 notImplemented														 , //
		/* 31 */ 		 notImplemented														 , //
		/* 32 */ 		 notImplemented														 , //
		/* 33 */ 		 notImplemented														 , //
		/* 34 */ () -> { int j = xiy(); writeByte(j, inc(readMem(j))); 			return 23 ; }, // INC_XIY
		/* 35 */ () -> { int j = xiy(); writeByte(j, dec(readMem(j))); 			return 23 ; }, // DEC_XIY
		/* 36 */ () -> { writeByte(xiy(), readByte()); 							return 19 ; }, // LD_XIY_BYTE
		/* 37 */ 		 notImplemented														 , //
		/* 38 */ 		 notImplemented														 , //
		/* 39 */ () -> { regs.iy.setW(addWord(regs.iy.w, regs.sp)); 			return 15 ; }, // ADD_IY_SP
		/* 3A */ 		 notImplemented														 , //
		/* 3B */ 		 notImplemented														 , //
		/* 3C */ 		 notImplemented														 , //
		/* 3D */ 		 notImplemented														 , //
		/* 3E */ 		 notImplemented														 , //
		/* 3F */ 		 notImplemented														 , //
		/* 40 */ 		 notImplemented														 , //
		/* 41 */ 		 notImplemented														 , //
		/* 42 */ 		 notImplemented														 , //
		/* 43 */ 		 notImplemented														 , //
		/* 44 */ 		 notImplemented														 , //
		/* 45 */ 		 notImplemented														 , //
		/* 46 */ () -> { regs.bc.setH(readXIY()); 								return 19 ; }, // LD_B_XIY
		/* 47 */ 		 notImplemented														 , //
		/* 48 */ 		 notImplemented														 , //
		/* 49 */ 		 notImplemented														 , //
		/* 4A */ 		 notImplemented														 , //
		/* 4B */ 		 notImplemented														 , //
		/* 4C */ 		 notImplemented														 , //
		/* 4D */ 		 notImplemented														 , //
		/* 4E */ () -> { regs.bc.setL(readXIY()); 								return 19 ; }, // LD_C_XIY
		/* 4F */ 		 notImplemented														 , //
		/* 50 */ 		 notImplemented														 , //
		/* 51 */ 		 notImplemented														 , //
		/* 52 */ 		 notImplemented														 , //
		/* 53 */ 		 notImplemented														 , //
		/* 54 */ () -> { regs.de.setH(regs.iy.h); 								return 9  ; }, // LD_D_IYH
		/* 55 */ 		 notImplemented														 , //
		/* 56 */ () -> { regs.de.setH(readXIY()); 								return 19 ; }, // LD_D_XIY
		/* 57 */ 		 notImplemented														 , //
		/* 58 */ 		 notImplemented														 , //
		/* 59 */ 		 notImplemented														 , //
		/* 5A */ 		 notImplemented														 , //
		/* 5B */ 		 notImplemented														 , //
		/* 5C */ 		 notImplemented														 , //
		/* 5D */ () -> { regs.de.setL(regs.iy.l); 								return 9  ; }, // LD_E_IYL
		/* 5E */ () -> { regs.de.setL(readXIY()); 								return 19 ; }, // LD_E_XIY
		/* 5F */ 		 notImplemented														 , //
		/* 60 */ 		 notImplemented														 , //
		/* 61 */ 		 notImplemented														 , //
		/* 62 */ 		 notImplemented														 , //
		/* 63 */ 		 notImplemented														 , //
		/* 64 */ 		 notImplemented														 , //
		/* 65 */ 		 notImplemented														 , //
		/* 66 */ () -> { regs.hl.setH(readXIY()); 								return 9  ; }, // LD_H_XIY
		/* 67 */ () -> { regs.iy.setH(regs.a); 									return 9  ; }, // LD_IYH_A
		/* 68 */ 		 notImplemented														 , //
		/* 69 */ 		 notImplemented														 , //
		/* 6A */ 		 notImplemented														 , //
		/* 6B */ 		 notImplemented														 , //
		/* 6C */ 		 notImplemented														 , //
		/* 6D */ 		 notImplemented														 , //
		/* 6E */ () -> { regs.hl.setL(readXIY()); 								return 9  ; }, // LD_L_XIY
		/* 6F */ () -> { regs.iy.setL(regs.a);    								return 9  ; }, // LD_IYL_A
		/* 70 */ () -> { writeXIY(regs.bc.h);     								return 19 ; }, // LD_XIY_B
		/* 71 */ () -> { writeXIY(regs.bc.l);     								return 19 ; }, // LD_XIY_C
		/* 72 */ () -> { writeXIY(regs.de.h);     								return 19 ; }, // LD_XIY_D
		/* 73 */ () -> { writeXIY(regs.de.l);     								return 19 ; }, // LD_XIY_E
		/* 74 */ () -> { writeXIY(regs.hl.h);     								return 19 ; }, // LD_XIY_H
		/* 75 */ () -> { writeXIY(regs.hl.l);     								return 19 ; }, // LD_XIY_L
		/* 76 */ 		 notImplemented														 , //
		/* 77 */ () -> { writeXIY(regs.a); 										return 19 ; }, // LD_XIY_A
		/* 78 */ 		 notImplemented														 , //
		/* 79 */ 		 notImplemented														 , //
		/* 7A */ 		 notImplemented														 , //
		/* 7B */ 		 notImplemented														 , //
		/* 7C */ () -> { regs.a = regs.iy.h; 									return 9  ; }, // LD_A_IYH
		/* 7D */ () -> { regs.a = regs.iy.l; 									return 9  ; }, // LD_A_IYL
		/* 7E */ () -> { regs.a = readXIY(); 									return 19 ; }, // LD_A_XIY
		/* 7F */ 		 notImplemented														 , //
		/* 80 */ 		 notImplemented														 , //
		/* 81 */ 		 notImplemented														 , //
		/* 82 */ 		 notImplemented														 , //
		/* 83 */ 		 notImplemented														 , //
		/* 84 */ () -> { add(regs.iy.h); 										return 9  ; }, // ADD_A_IYH
		/* 85 */ 		 notImplemented														 , //
		/* 86 */ () -> { add(readXIY()); 										return 19 ; }, // ADD_A_XIY
		/* 87 */ 		 notImplemented														 , //
		/* 88 */ 		 notImplemented														 , //
		/* 89 */ 		 notImplemented														 , //
		/* 8A */ 		 notImplemented														 , //
		/* 8B */ 		 notImplemented														 , //
		/* 8C */ 		 notImplemented														 , //
		/* 8D */ 		 notImplemented														 , //
		/* 8E */ 		 notImplemented														 , //
		/* 8F */ 		 notImplemented														 , //
		/* 90 */ 		 notImplemented														 , //
		/* 91 */ 		 notImplemented														 , //
		/* 92 */ 		 notImplemented														 , //
		/* 93 */ 		 notImplemented														 , //
		/* 94 */ 		 notImplemented														 , //
		/* 95 */ 		 notImplemented														 , //
		/* 96 */ () -> { sub(readXIY()); 										return 19 ; }, // SUB_XIY
		/* 97 */ 		 notImplemented														 , //
		/* 98 */ 		 notImplemented														 , //
		/* 99 */ 		 notImplemented														 , //
		/* 9A */ 		 notImplemented														 , //
		/* 9B */ 		 notImplemented														 , //
		/* 9C */ 		 notImplemented														 , //
		/* 9D */ 		 notImplemented														 , //
		/* 9E */ 		 notImplemented														 , //
		/* 9F */ 		 notImplemented														 , //
		/* A0 */ 		 notImplemented														 , //
		/* A1 */ 		 notImplemented														 , //
		/* A2 */ 		 notImplemented														 , //
		/* A3 */ 		 notImplemented														 , //
		/* A4 */ 		 notImplemented														 , //
		/* A5 */ 		 notImplemented														 , //
		/* A6 */ () -> { and(readXIY()); 										return 19 ; }, // AND_XIY
		/* A7 */ 		 notImplemented														 , //
		/* A8 */ 		 notImplemented														 , //
		/* A9 */ 		 notImplemented														 , //
		/* AA */ 		 notImplemented														 , //
		/* AB */ 		 notImplemented														 , //
		/* AC */ 		 notImplemented														 , //
		/* AD */ 		 notImplemented														 , //
		/* AE */ () -> { xor(readXIY()); 										return 19 ; }, // XOR_XIY
		/* AF */ 		 notImplemented														 , //
		/* B0 */ 		 notImplemented														 , //
		/* B1 */ 		 notImplemented														 , //
		/* B2 */ 		 notImplemented														 , //
		/* B3 */ 		 notImplemented														 , //
		/* B4 */ 		 notImplemented														 , //
		/* B5 */ 		 notImplemented														 , //
		/* B6 */ () -> { or(readXIY()); 										return 19 ; }, // OR_XIY
		/* B7 */ 		 notImplemented														 , //
		/* B8 */ 		 notImplemented														 , //
		/* B9 */ 		 notImplemented														 , //
		/* BA */ 		 notImplemented														 , //
		/* BB */ 		 notImplemented														 , //
		/* BC */ 		 notImplemented														 , //
		/* BD */ 		 notImplemented														 , //
		/* BE */ () -> { compare(readXIY()); 									return 19 ; }, // CP_XIY
		/* BF */ 		 notImplemented														 , //
		/* C0 */ 		 notImplemented														 , //
		/* C1 */ 		 notImplemented														 , //
		/* C2 */ 		 notImplemented														 , //
		/* C3 */ 		 notImplemented														 , //
		/* C4 */ 		 notImplemented														 , //
		/* C5 */ 		 notImplemented														 , //
		/* C6 */ 		 notImplemented														 , //
		/* C7 */ 		 notImplemented														 , //
		/* C8 */ 		 notImplemented														 , //
		/* C9 */ 		 notImplemented														 , //
		/* CA */ 		 notImplemented														 , //
		/* CB */ () -> { fdcb(); 												return 0  ; }, // FD_CB
		/* CC */ 		 notImplemented														 , //
		/* CD */ 		 notImplemented														 , //
		/* CE */ 		 notImplemented														 , //
		/* CF */ 		 notImplemented														 , //
		/* D0 */ 		 notImplemented														 , //
		/* D1 */ 		 notImplemented														 , //
		/* D2 */ 		 notImplemented														 , //
		/* D3 */ 		 notImplemented														 , //
		/* D4 */ 		 notImplemented														 , //
		/* D5 */ 		 notImplemented														 , //
		/* D6 */ 		 notImplemented														 , //
		/* D7 */ 		 notImplemented														 , //
		/* D8 */ 		 notImplemented														 , //
		/* D9 */ 		 notImplemented														 , //
		/* DA */ 		 notImplemented														 , //
		/* DB */ 		 notImplemented														 , //
		/* DC */ 		 notImplemented														 , //
		/* DD */ 		 notImplemented														 , //
		/* DE */ 		 notImplemented														 , //
		/* DF */ 		 notImplemented														 , //
		/* E0 */ 		 notImplemented														 , //
		/* E1 */ () -> { regs.iy.setW(pop()); 									return 14 ; }, // POP_IY
		/* E2 */ 		 notImplemented														 , //
		/* E3 */ () -> { exXspIY(); 											return 23 ; }, // EX_XSP_IY
		/* E4 */ 		 notImplemented														 , //
		/* E5 */ () -> { push(regs.iy.w); 										return 15 ; }, // PUSH_IY
		/* E6 */ 		 notImplemented														 , //
		/* E7 */ 		 notImplemented														 , //
		/* E8 */ 		 notImplemented														 , //
		/* E9 */ () -> { regs.pc = regs.iy.w; 									return 8  ; }, // JP_IY
		/* EA */ 		 notImplemented														 , //
		/* EB */ 		 notImplemented														 , //
		/* EC */ 		 notImplemented														 , //
		/* ED */ 		 notImplemented														 , //
		/* EE */ 		 notImplemented														 , //
		/* EF */ 		 notImplemented														 , //
		/* F0 */ 		 notImplemented														 , //
		/* F1 */ 		 notImplemented														 , //
		/* F2 */ 		 notImplemented														 , //
		/* F3 */ 		 notImplemented														 , //
		/* F4 */ 		 notImplemented														 , //
		/* F5 */ 		 notImplemented														 , //
		/* F6 */ 		 notImplemented														 , //
		/* F7 */ 		 notImplemented														 , //
		/* F8 */ 		 notImplemented														 , //
		/* F9 */ () -> { regs.sp = regs.iy.w; 									return 10 ; }, // LD_SP_IY
		/* FA */ 		 notImplemented														 , //
		/* FB */ 		 notImplemented														 , //
		/* FC */ 		 notImplemented														 , //
		/* FD */ 		 notImplemented														 , //
		/* FE */ 		 notImplemented														 , //
		/* FF */ 		 notImplemented														 , //
	};

	private final Op[] fdcb = {
		/* 00 */ 		 notImplemented														   , //
		/* 01 */ 		 notImplemented														   , //
		/* 02 */ 		 notImplemented														   , //
		/* 03 */ 		 notImplemented														   , //
		/* 04 */ 		 notImplemented														   , //
		/* 05 */ 		 notImplemented														   , //
		/* 06 */ 		 notImplemented														   , //
		/* 07 */ 		 notImplemented														   , //
		/* 08 */ 		 notImplemented														   , //
		/* 09 */ 		 notImplemented														   , //
		/* 0A */ 		 notImplemented														   , //
		/* 0B */ 		 notImplemented														   , //
		/* 0C */ 		 notImplemented														   , //
		/* 0D */ 		 notImplemented														   , //
		/* 0E */ 		 notImplemented														   , //
		/* 0F */ 		 notImplemented														   , //
		/* 10 */ 		 notImplemented														   , //
		/* 11 */ 		 notImplemented														   , //
		/* 12 */ 		 notImplemented														   , //
		/* 13 */ 		 notImplemented														   , //
		/* 14 */ 		 notImplemented														   , //
		/* 15 */ 		 notImplemented														   , //
		/* 16 */ () -> { int j=xiy(); writeByte(j, rl(readMem(j)));     		return 23	; }, // RL_XIY
		/* 17 */ 		 notImplemented														   , //
		/* 18 */ 		 notImplemented														   , //
		/* 19 */ 		 notImplemented														   , //
		/* 1A */ 		 notImplemented														   , //
		/* 1B */ 		 notImplemented														   , //
		/* 1C */ 		 notImplemented														   , //
		/* 1D */ 		 notImplemented														   , //
		/* 1E */ () -> { int j=xiy(); writeByte(j, rr(readMem(j)));     		return 23	; }, // RR_XIY
		/* 1F */ 		 notImplemented														   , //
		/* 20 */ 		 notImplemented														   , //
		/* 21 */ 		 notImplemented														   , //
		/* 22 */ 		 notImplemented														   , //
		/* 23 */ 		 notImplemented														   , //
		/* 24 */ 		 notImplemented														   , //
		/* 25 */ 		 notImplemented														   , //
		/* 26 */ () -> { int j=xiy(); writeByte(j, sla(readMem(j)));    		return 23	; }, // SLA_XIY
		/* 27 */ 		 notImplemented														   , //
		/* 28 */ 		 notImplemented														   , //
		/* 29 */ 		 notImplemented														   , //
		/* 2A */ 		 notImplemented														   , //
		/* 2B */ 		 notImplemented														   , //
		/* 2C */ 		 notImplemented														   , //
		/* 2D */ 		 notImplemented														   , //
		/* 2E */ 		 notImplemented														   , //
		/* 2F */ 		 notImplemented														   , //
		/* 30 */ 		 notImplemented														   , //
		/* 31 */ 		 notImplemented														   , //
		/* 32 */ 		 notImplemented														   , //
		/* 33 */ 		 notImplemented														   , //
		/* 34 */ 		 notImplemented														   , //
		/* 35 */ 		 notImplemented														   , //
		/* 36 */ 		 notImplemented														   , //
		/* 37 */ 		 notImplemented														   , //
		/* 38 */ 		 notImplemented														   , //
		/* 39 */ 		 notImplemented														   , //
		/* 3A */ 		 notImplemented														   , //
		/* 3B */ 		 notImplemented														   , //
		/* 3C */ 		 notImplemented														   , //
		/* 3D */ 		 notImplemented														   , //
		/* 3E */ 		 notImplemented														   , //
		/* 3F */ 		 notImplemented														   , //
		/* 40 */ () -> { testBit(0, readXIY());                         		return 20	; }, // BIT_0_XIY
		/* 41 */ () -> { testBit(0, readXIY());                         		return 20	; }, // BIT_0_XIY
		/* 42 */ () -> { testBit(0, readXIY());                         		return 20	; }, // BIT_0_XIY
		/* 43 */ () -> { testBit(0, readXIY());                         		return 20	; }, // BIT_0_XIY
		/* 44 */ () -> { testBit(0, readXIY());                         		return 20	; }, // BIT_0_XIY
		/* 45 */ () -> { testBit(0, readXIY());                         		return 20	; }, // BIT_0_XIY
		/* 46 */ () -> { testBit(0, readXIY());                         		return 20	; }, // BIT_0_XIY
		/* 47 */ () -> { testBit(0, readXIY());                         		return 20	; }, // BIT_0_XIY
		/* 48 */ () -> { testBit(1, readXIY());                         		return 20	; }, // BIT_1_XIY
		/* 49 */ () -> { testBit(1, readXIY());                         		return 20	; }, // BIT_1_XIY
		/* 4A */ () -> { testBit(1, readXIY());                         		return 20	; }, // BIT_1_XIY
		/* 4B */ () -> { testBit(1, readXIY());                         		return 20	; }, // BIT_1_XIY
		/* 4C */ () -> { testBit(1, readXIY());                         		return 20	; }, // BIT_1_XIY
		/* 4D */ () -> { testBit(1, readXIY());                         		return 20	; }, // BIT_1_XIY
		/* 4E */ () -> { testBit(1, readXIY());                         		return 20	; }, // BIT_1_XIY
		/* 4F */ () -> { testBit(1, readXIY());                         		return 20	; }, // BIT_1_XIY
		/* 50 */ () -> { testBit(2, readXIY());                         		return 20	; }, // BIT_2_XIY
		/* 51 */ () -> { testBit(2, readXIY());                         		return 20	; }, // BIT_2_XIY
		/* 52 */ () -> { testBit(2, readXIY());                         		return 20	; }, // BIT_2_XIY
		/* 53 */ () -> { testBit(2, readXIY());                         		return 20	; }, // BIT_2_XIY
		/* 54 */ () -> { testBit(2, readXIY());                         		return 20	; }, // BIT_2_XIY
		/* 55 */ () -> { testBit(2, readXIY());                         		return 20	; }, // BIT_2_XIY
		/* 56 */ () -> { testBit(2, readXIY());                         		return 20	; }, // BIT_2_XIY
		/* 57 */ () -> { testBit(2, readXIY());                         		return 20	; }, // BIT_2_XIY
		/* 58 */ () -> { testBit(3, readXIY());                         		return 20	; }, // BIT_3_XIY
		/* 59 */ () -> { testBit(3, readXIY());                         		return 20	; }, // BIT_3_XIY
		/* 5A */ () -> { testBit(3, readXIY());                         		return 20	; }, // BIT_3_XIY
		/* 5B */ () -> { testBit(3, readXIY());                         		return 20	; }, // BIT_3_XIY
		/* 5C */ () -> { testBit(3, readXIY());                         		return 20	; }, // BIT_3_XIY
		/* 5D */ () -> { testBit(3, readXIY());                         		return 20	; }, // BIT_3_XIY
		/* 5E */ () -> { testBit(3, readXIY());                         		return 20	; }, // BIT_3_XIY
		/* 5F */ () -> { testBit(3, readXIY());                         		return 20	; }, // BIT_3_XIY
		/* 60 */ () -> { testBit(4, readXIY());                         		return 20	; }, // BIT_4_XIY
		/* 61 */ () -> { testBit(4, readXIY());                         		return 20	; }, // BIT_4_XIY
		/* 62 */ () -> { testBit(4, readXIY());                         		return 20	; }, // BIT_4_XIY
		/* 63 */ () -> { testBit(4, readXIY());                         		return 20	; }, // BIT_4_XIY
		/* 64 */ () -> { testBit(4, readXIY());                         		return 20	; }, // BIT_4_XIY
		/* 65 */ () -> { testBit(4, readXIY());                         		return 20	; }, // BIT_4_XIY
		/* 66 */ () -> { testBit(4, readXIY());                         		return 20	; }, // BIT_4_XIY
		/* 67 */ () -> { testBit(4, readXIY());                         		return 20	; }, // BIT_4_XIY
		/* 68 */ () -> { testBit(5, readXIY());                         		return 20	; }, // BIT_5_XIY
		/* 69 */ () -> { testBit(5, readXIY());                         		return 20	; }, // BIT_5_XIY
		/* 6A */ () -> { testBit(5, readXIY());                         		return 20	; }, // BIT_5_XIY
		/* 6B */ () -> { testBit(5, readXIY());                         		return 20	; }, // BIT_5_XIY
		/* 6C */ () -> { testBit(5, readXIY());                         		return 20	; }, // BIT_5_XIY
		/* 6D */ () -> { testBit(5, readXIY());                         		return 20	; }, // BIT_5_XIY
		/* 6E */ () -> { testBit(5, readXIY());                         		return 20	; }, // BIT_5_XIY
		/* 6F */ () -> { testBit(5, readXIY());                         		return 20	; }, // BIT_5_XIY
		/* 70 */ () -> { testBit(6, readXIY());                         		return 20	; }, // BIT_6_XIY
		/* 71 */ () -> { testBit(6, readXIY());                         		return 20	; }, // BIT_6_XIY
		/* 72 */ () -> { testBit(6, readXIY());                         		return 20	; }, // BIT_6_XIY
		/* 73 */ () -> { testBit(6, readXIY());                         		return 20	; }, // BIT_6_XIY
		/* 74 */ () -> { testBit(6, readXIY());                         		return 20	; }, // BIT_6_XIY
		/* 75 */ () -> { testBit(6, readXIY());                         		return 20	; }, // BIT_6_XIY
		/* 76 */ () -> { testBit(6, readXIY());                         		return 20	; }, // BIT_6_XIY
		/* 77 */ () -> { testBit(6, readXIY());                         		return 20	; }, // BIT_6_XIY
		/* 78 */ () -> { testBit(7, readXIY());                         		return 20	; }, // BIT_7_XIY
		/* 79 */ () -> { testBit(7, readXIY());                         		return 20	; }, // BIT_7_XIY
		/* 7A */ () -> { testBit(7, readXIY());                         		return 20	; }, // BIT_7_XIY
		/* 7B */ () -> { testBit(7, readXIY());                         		return 20	; }, // BIT_7_XIY
		/* 7C */ () -> { testBit(7, readXIY());                         		return 20	; }, // BIT_7_XIY
		/* 7D */ () -> { testBit(7, readXIY());                         		return 20	; }, // BIT_7_XIY
		/* 7E */ () -> { testBit(7, readXIY());                         		return 20	; }, // BIT_7_XIY
		/* 7F */ () -> { testBit(7, readXIY());                         		return 20	; }, // BIT_7_XIY
		/* 80 */ 		 notImplemented														   , //
		/* 81 */ 		 notImplemented														   , //
		/* 82 */ 		 notImplemented														   , //
		/* 83 */ 		 notImplemented														   , //
		/* 84 */ 		 notImplemented														   , //
		/* 85 */ 		 notImplemented														   , //
		/* 86 */ () -> { int j=xiy();writeByte(j, resetBit(0, readMem(j)));		return 23	; }, // RES_0_XIY
		/* 87 */ 		 notImplemented														   , //
		/* 88 */ 		 notImplemented														   , //
		/* 89 */ 		 notImplemented														   , //
		/* 8A */ 		 notImplemented														   , //
		/* 8B */ 		 notImplemented														   , //
		/* 8C */ 		 notImplemented														   , //
		/* 8D */ 		 notImplemented														   , //
		/* 8E */ () -> { int j=xiy();writeByte(j, resetBit(1, readMem(j)));		return 23	; }, // RES_1_XIY
		/* 8F */ 		 notImplemented														   , //
		/* 90 */ 		 notImplemented														   , //
		/* 91 */ 		 notImplemented														   , //
		/* 92 */ 		 notImplemented														   , //
		/* 93 */ 		 notImplemented														   , //
		/* 94 */ 		 notImplemented														   , //
		/* 95 */ 		 notImplemented														   , //
		/* 96 */ () -> { int j=xiy();writeByte(j, resetBit(2, readMem(j)));		return 23	; }, // RES_2_XIY
		/* 97 */ 		 notImplemented														   , //
		/* 98 */ 		 notImplemented														   , //
		/* 99 */ 		 notImplemented														   , //
		/* 9A */ 		 notImplemented														   , //
		/* 9B */ 		 notImplemented														   , //
		/* 9C */ 		 notImplemented														   , //
		/* 9D */ 		 notImplemented														   , //
		/* 9E */ () -> { int j=xiy();writeByte(j, resetBit(3, readMem(j)));		return 23	; }, // RES_3_XIY
		/* 9F */ 		 notImplemented														   , //
		/* A0 */ 		 notImplemented														   , //
		/* A1 */ 		 notImplemented														   , //
		/* A2 */ 		 notImplemented														   , //
		/* A3 */ 		 notImplemented														   , //
		/* A4 */ 		 notImplemented														   , //
		/* A5 */ 		 notImplemented														   , //
		/* A6 */ () -> { int j=xiy();writeByte(j, resetBit(4, readMem(j))); 	return 23	; }, // RES_4_XIY
		/* A7 */ 		 notImplemented														   , //
		/* A8 */ 		 notImplemented														   , //
		/* A9 */ 		 notImplemented														   , //
		/* AA */ 		 notImplemented														   , //
		/* AB */ 		 notImplemented														   , //
		/* AC */ 		 notImplemented														   , //
		/* AD */ 		 notImplemented														   , //
		/* AE */ () -> { int j=xiy();writeByte(j, resetBit(5, readMem(j)));		return 23	; }, // RES_5_XIY
		/* AF */ 		 notImplemented														   , //
		/* B0 */ 		 notImplemented														   , //
		/* B1 */ 		 notImplemented														   , //
		/* B2 */ 		 notImplemented														   , //
		/* B3 */ 		 notImplemented														   , //
		/* B4 */ 		 notImplemented														   , //
		/* B5 */ 		 notImplemented														   , //
		/* B6 */ () -> { int j=xiy();writeByte(j,resetBit(6,readMem(j)));		return 23	; }, // RES_6_XIY
		/* B7 */ 		 notImplemented														   , //
		/* B8 */ 		 notImplemented														   , //
		/* B9 */ 		 notImplemented														   , //
		/* BA */ 		 notImplemented														   , //
		/* BB */ 		 notImplemented														   , //
		/* BC */ 		 notImplemented														   , //
		/* BD */ 		 notImplemented														   , //
		/* BE */ () -> { int j=xiy();writeByte(j, resetBit(7, readMem(j)));		return 23	; }, // RES_7_XIY
		/* BF */ 		 notImplemented														   , //
		/* C0 */ 		 notImplemented														   , //
		/* C1 */ 		 notImplemented														   , //
		/* C2 */ 		 notImplemented														   , //
		/* C3 */ 		 notImplemented														   , //
		/* C4 */ 		 notImplemented														   , //
		/* C5 */ 		 notImplemented														   , //
		/* C6 */ () -> { int j=xiy(); writeByte(j, setBit(0, readMem(j)));		return 23	; }, // SET_0_XIY
		/* C7 */ 		 notImplemented														   , //
		/* C8 */ 		 notImplemented														   , //
		/* C9 */ 		 notImplemented														   , //
		/* CA */ 		 notImplemented														   , //
		/* CB */ 		 notImplemented														   , //
		/* CC */ 		 notImplemented														   , //
		/* CD */ 		 notImplemented														   , //
		/* CE */ () -> { int j=xiy(); writeByte(j, setBit(1, readMem(j)));		return 23	; }, // SET_1_XIY
		/* CF */ 		 notImplemented														   , //
		/* D0 */ 		 notImplemented														   , //
		/* D1 */ 		 notImplemented														   , //
		/* D2 */ 		 notImplemented														   , //
		/* D3 */ 		 notImplemented														   , //
		/* D4 */ 		 notImplemented														   , //
		/* D5 */ 		 notImplemented														   , //
		/* D6 */ () -> { int j=xiy(); writeByte(j, setBit(2, readMem(j)));		return 23	; }, // SET_2_XIY
		/* D7 */ 		 notImplemented														   , //
		/* D8 */ 		 notImplemented														   , //
		/* D9 */ 		 notImplemented														   , //
		/* DA */ 		 notImplemented														   , //
		/* DB */ 		 notImplemented														   , //
		/* DC */ 		 notImplemented														   , //
		/* DD */ 		 notImplemented														   , //
		/* DE */ () -> { int j=xiy(); writeByte(j, setBit(3, readMem(j)));		return 23	; }, // SET_3_XIY
		/* DF */ 		 notImplemented														   , //
		/* E0 */ 		 notImplemented														   , //
		/* E1 */ 		 notImplemented														   , //
		/* E2 */ 		 notImplemented														   , //
		/* E3 */ 		 notImplemented														   , //
		/* E4 */ 		 notImplemented														   , //
		/* E5 */ 		 notImplemented														   , //
		/* E6 */ () -> { int j=xiy(); writeByte(j,setBit(4,readMem(j)));		return 23	; }, // SET_4_XIY
		/* E7 */ 		 notImplemented														   , //
		/* E8 */ 		 notImplemented														   , //
		/* E9 */ 		 notImplemented														   , //
		/* EA */ 		 notImplemented														   , //
		/* EB */ 		 notImplemented														   , //
		/* EC */ 		 notImplemented														   , //
		/* ED */ 		 notImplemented														   , //
		/* EE */ () -> { int j=xiy(); writeByte(j, setBit(5, readMem(j)));		return 3 	; }, // SET_5_XIY
		/* EF */ 		 notImplemented														   , //
		/* F0 */ 		 notImplemented														   , //
		/* F1 */ 		 notImplemented														   , //
		/* F2 */ 		 notImplemented														   , //
		/* F3 */ 		 notImplemented														   , //
		/* F4 */ 		 notImplemented														   , //
		/* F5 */ 		 notImplemented														   , //
		/* F6 */ () -> { int j=xiy(); writeByte(j, setBit(6, readMem(j)));		return 23	; }, // SET_6_XIY
		/* F7 */ 		 notImplemented														   , //
		/* F8 */ 		 notImplemented														   , //
		/* F9 */ 		 notImplemented														   , //
		/* FA */ 		 notImplemented														   , //
		/* FB */ 		 notImplemented														   , //
		/* FC */ 		 notImplemented														   , //
		/* FD */ 		 notImplemented														   , //
		/* FE */ () -> { int j=xiy(); writeByte(j, setBit(7, readMem(j)));		return 23	; }, // SET_7_XIY
		/* FF */ 		 notImplemented														   , //
	};

	private void call(boolean call) {
		if (call) {
			int q = readWord();
			push(regs.pc);
			regs.pc = q;
			iCount -= 7;
		} else {
			regs.pc = (regs.pc + 2) & 0xFFFF;
		}
	}

	private void jump(boolean jump) {
		if (jump) {
			regs.pc = readArgument(regs.pc) + ((readArgument((regs.pc + 1) & 65535)) << 8);
		} else {
			regs.pc = (regs.pc + 2) & 0xFFFF;
		}
	}

	private void jumpRelative(boolean jump) {
		if (jump) {
			regs.pc = (regs.pc + (byte) readArgument(regs.pc) + 1) & 0xFFFF;
			iCount -= 5;
		} else {
			incPC();
		}
	}

	private void ret(boolean ret) {
		if (ret) {
			regs.pc = pop();
			iCount -= 6;
		}
	}

	private char portIn(int port) {
		return machine.portRead(port);
	}

	private void portOut(int port, int value) {
		machine.portWrite(port, (char) value);
	}

	private char readMem(int address) {
		return (char) machine.memoryRead(address);
	}

	private void writeByte(int address, int value) {
		machine.memoryWrite(address, (char) value);
	}

	private char readByte(int address) {
		return (char) machine.memoryRead(address);
	}

	private char readArgument(int address) {
		return (char) machine.memoryRead(address);
	}

	private char readStack(int address) {
		return (char) machine.memoryRead(address);
	}

	private void writeStack(int address, int value) {
		machine.memoryWrite(address, (char) value);
	}

	private int pop() {
		int i = readStack(regs.sp)
				+ (readStack((regs.sp + 1) & 0xFFFF) << 8);
		regs.sp = (regs.sp + 2) & 0xFFFF;
		return i;
	}

	private void push(int value) {
		regs.sp = (regs.sp - 2) & 0xFFFF;
		writeStack(regs.sp, value & 0xFF);
		writeStack((regs.sp + 1) & 0xFFFF, value >> 8);
	}

	private void call() {
		int q = readWord();
		push(regs.pc);
		regs.pc = q;
		iCount -= 7;
	}

	private void jump() {
		regs.pc = readArgument(regs.pc)
				+ ((readArgument((regs.pc + 1) & 65535)) << 8);

	}

	private void rumpRelative() {
		regs.pc = (regs.pc + (byte) readArgument(regs.pc) + 1) & 0xFFFF;
		iCount -= 5;

	}

	private void ret() {
		regs.pc = pop();
		iCount -= 6;
	}

	private void reset(int address) {
		push(regs.pc); regs.pc = address;
	}

	private int setBit(int bit, int value) {
		return value | (1 << bit);
	}

	private int resetBit(int bit, int value) {
		return value & (~1 << bit);
	}

	private void testBit(int bit, int value) {
		regs.f = (regs.f & C_FLAG)
				| H_FLAG
				| (((value & (1 << bit)) != 0)
				? ((bit == 7) ? S_FLAG : 0)
				: Z_FLAG);
	}

	private void and(int value) {
		regs.a &= value;
		regs.f = zspTable[regs.a] | H_FLAG;
	}

	private void or(int value) {
		regs.a |= value;
		regs.f = zspTable[regs.a];
	}

	private void xor(int value) {
		regs.a ^= value;
		regs.f = zspTable[regs.a];
	}

	private int in() {
		int i = portIn(regs.bc.l);
		regs.f = (regs.f & C_FLAG) | zspTable[i];
		return i;
	}

	private void rlca() {
		regs.a = ((regs.a << 1) | ((regs.a & 0x80) >> 7)) & 0xFF;
		regs.f = (regs.f & 0xEC) | (regs.a & C_FLAG);
	}

	private void rrca() {
		regs.f = (regs.f & 0xEC) | (regs.a & 0x01);
		regs.a = ((regs.a >> 1) | (regs.a << 7)) & 0xFF;
	}

	private void rla() {
		int i = regs.f & C_FLAG;
		regs.f = (regs.f & 0xEC) | ((regs.a & 0x80) >> 7);
		regs.a = ((regs.a << 1) | i) & 0xFF;
	}

	private void rra() {
		int i = regs.f & C_FLAG;
		regs.f = (regs.f & 0xEC) | (regs.a & 0x01);
		regs.a = ((regs.a >> 1) | (i << 7)) & 0xFF;
	}

	private int rlc(int value) {
		int v = value >> 7;
		value = ((value << 1) | v) & 0xFF;
		regs.f = zspTable[value] | v;
		return value;
	}

	private int rrc(int value) {
		int v = value & 1;
		value = ((value >> 1) | (v << 7)) & 0xFF;
		regs.f = zspTable[value] | v;
		return value;
	}

	private int rl(int value) {
		int v = value >> 7;
		value = ((value << 1) | (regs.f & 1)) & 0xFF;
		regs.f = zspTable[value] | v;
		return value;
	}

	private int rr(int value) {
		int v = value & 1;
		value = ((value >> 1) | (regs.f << 7)) & 0xFF;
		regs.f = zspTable[value] | v;
		return value;
	}

	private int sll(int value) {
		int v = value >> 7;
		value = ((value << 1) | 1) & 0xFF;
		regs.f = zspTable[value] | v;
		return value;
	}

	private int sla(int value) {
		int v = value >> 7;
		value = (value << 1) & 0xFF;
		regs.f = zspTable[value] | v;
		return value;
	}

	private int slr(int value) {
		int v = value & 1;
		value = (value >> 1) & 0xFF;
		regs.f = zspTable[value] | v;
		return value;
	}

	private int sra(int value) {
		int v = value & 1;
		value = ((value >> 1) | (value & 0x80)) & 0xFF;
		regs.f = zspTable[value] | v;
		return value;
	}

	private int inc(int value) {
		value = (value + 1) & 0xFF;
		regs.f = (regs.f & C_FLAG) | zsTable[value] | ((value == 0x80) ? P_FLAG : 0)
				| ((value & 0xF) != 0 ? 0 : H_FLAG);
		return value;
	}

	private int dec(int value) {
		regs.f = (regs.f & C_FLAG) | N_FLAG | ((value == 0x80) ? P_FLAG : 0)
				| ((value & 0xF) != 0 ? 0 : H_FLAG);
		value = (value - 1) & 0xFF;
		regs.f |= zsTable[value];
		return value;
	}

	private void add(int value) {
		int v = regs.a + value;
		regs.f = zsTable[v & 0xFF] | ((v & 0x100) >> 8) | ((regs.a ^ v ^ value) & H_FLAG)
				| (((value ^ regs.a ^ 0x80) & (value ^ v) & 0x80) >> 5);
		regs.a = v & 0xFF;
	}

	private void addCarry(int value) {
		int v = regs.a + value + (regs.f & 1);
		regs.f = zsTable[v & 0xFF] | ((v & 0x100) >> 8) | ((regs.a ^ v ^ value) & H_FLAG)
				| (((value ^ regs.a ^ 0x80) & (value ^ v) & 0x80) >> 5);
		regs.a = v & 0xFF;
	}

	private void sub(int value) {
		int v = regs.a - value;
		regs.f = zsTable[v & 0xFF] | ((v & 0x100) >> 8) | N_FLAG
				| ((regs.a ^ v ^ value) & H_FLAG)
				| (((value ^ regs.a) & (value ^ v) & 0x80) >> 5);
		regs.a = v & 0xFF;
	}

	private void subCarry(int value) {
		int v = regs.a - value - (regs.f & 1);
		regs.f = zsTable[v & 0xFF] | ((v & 0x100) >> 8) | N_FLAG
				| ((regs.a ^ v ^ value) & H_FLAG)
				| (((value ^ regs.a) & (value ^ v) & 0x80) >> 5);
		regs.a = v & 0xFF;
	}

	private void compare(int value) {
		int v = regs.a - value;
		regs.f = zsTable[v & 0xFF] | ((v & 0x100) >> 8) | N_FLAG
				| ((regs.a ^ v ^ value) & H_FLAG)
				| (((value ^ regs.a) & (value ^ v) & 0x80) >> 5);
	}

	private int addWord(int value1, int value2) {
		int v = value1 + value2;
		regs.f = (regs.f & (S_FLAG | Z_FLAG | P_FLAG))
				| (((value1 ^ v ^ value2) & 0x1000) >> 8) | ((v >> 16) & 1);
		return v & 0xFFFF;
	}

	private void addWordCarry(int value) {
		int v = regs.hl.w + value + (regs.f & 1);
		regs.f = (((regs.hl.w ^ v ^ value) & 0x1000) >> 8) | ((v >> 16) & 1)
				| ((v & 0x8000) >> 8) | (((v & 0xFFFF) != 0) ? 0 : Z_FLAG)
				| (((value ^ regs.hl.w ^ 0x8000) & (value ^ v) & 0x8000) >> 13);
		regs.hl.setW(v & 0xFFFF);
	}

	private void subWordCarry(int value) {
		int v = regs.hl.w - value - (regs.f & 1);
		regs.f = (((regs.hl.w ^ v ^ value) & 0x1000) >> 8) | ((v >> 16) & 1)
				| ((v & 0x8000) >> 8) | (((v & 0xFFFF) != 0) ? 0 : Z_FLAG)
				| (((value ^ regs.hl.w) & (value ^ v) & 0x8000) >> 13) | N_FLAG;
		regs.hl.setW(v & 0xFFFF);
	}

	private boolean carry() {
		return (regs.f & C_FLAG) != 0;
	}

	private boolean notCarry() {
		return (regs.f & C_FLAG) == 0;
	}

	private boolean zero() {
		return (regs.f & Z_FLAG) != 0;
	}

	private boolean notZero() {
		return (regs.f & Z_FLAG) == 0;
	}

	private boolean sign() {
		return (regs.f & S_FLAG) != 0;
	}

	private boolean notSign() {
		return (regs.f & S_FLAG) == 0;
	}

	private boolean pe() {
		return (regs.f & P_FLAG) != 0;
	}

	private boolean po() {
		return (regs.f & P_FLAG) == 0;
	}

	private void incPC() {
		regs.pc = (regs.pc + 1) & 0xFFFF;
	}

	private char readByte() {
		char opcode = readByte(regs.pc);
		incPC();
		return opcode;
	}

	private char readWord(int address) {
		return (char) ((readMem(address + 1) << 8) | readMem(address));
	}

	private void writeWord(int address, int value) {
		writeByte(address, value & 0xFF);
		writeByte(address + 1, value >> 8);
	}

	private char readWord() {
		return (char) (readByte() | (readByte() << 8));
	}

	private int xix() {
		return (regs.ix.w + (byte) readByte()) & 0xFFFF;
	}

	private int xiy() {
		return (regs.iy.w + (byte) readByte()) & 0xFFFF;
	}

	private int readXHL() {
		return readMem(regs.hl.w);
	}

	private int readXIX() {
		return readMem(xix());
	}

	private int readXIY() {
		return readMem(xiy());
	}

	private void writeXIX(int value) {
		writeByte(xix(), value);
	}

	private void writeXIY(int value) {
		writeByte(xiy(), value);
	}

	private void ei() {
		if (regs.iff1 == 0) {
			regs.iff1 = regs.iff2 = 1;
			regs.r += 1;
			int opcode = readByte(regs.pc);
			incPC();
			iCount -= op_main[opcode].call();
			interrupt();
		} else {
			regs.iff2 = 1;
		}
	}

	private void ddcb() {
		int opcode = readArgument((regs.pc + 1) & 0xFFFF);
		iCount -= ddcb[opcode].call();
		incPC();
	}

	private void fdcb() {
		int opcode = readArgument((regs.pc + 1) & 0xFFFF);
		iCount -= fdcb[opcode].call();
		incPC();
	}

	private void ed() {
		regs.r += 1;
		int opcode = readByte(regs.pc);
		incPC();
		iCount -= ed[opcode].call();
	}

	private void exXspHL() {
		int i = readWord(regs.sp);
		writeWord(regs.sp, regs.hl.w);
		regs.hl.setW(i);
	}

	private void exXspIY() {
		int i = readWord(regs.sp);
		writeWord(regs.sp, regs.iy.w);
		regs.iy.setW(i);
	}

	private void dd() {
		regs.r += 1;
		int opcode = readByte(regs.pc);
		incPC();
		iCount -= dd[opcode].call();
	}

	private void fd() {
		regs.r += 1;
		int opcode = readByte(regs.pc);
		incPC();
		iCount -= fd[opcode].call();
	}

	private void exx() {
		int i = regs.bc.w;
		regs.bc.setW(regs.bc2);
		regs.bc2 = i;
		i = regs.de.w;
		regs.de.setW(regs.de2);
		regs.de2 = i;
		i = regs.hl.w;
		regs.hl.setW(regs.hl2);
		regs.hl2 = i;
	}

	private void cb() {
		regs.r += 1;
		int opcode = readByte(regs.pc);
		incPC();
		iCount -= cb[opcode].call();
	}

	private void jp() {
		int i = regs.pc - 1;
		jump();
		int j = regs.pc;
		if (j == i) {
			if (iCount > 0) iCount = 10;
		} else if ((j == i - 3) && (readByte(j) == 0x31)) {
			if (iCount > 10) iCount = 20;
		}
	}

	private void halt() {
		regs.pc = (regs.pc - 1) & 0xFFFF;
		regs.halt = 1;
		if (iCount > 4) iCount = 4;
	}

	private void daa() {
		int delta = 0;
		boolean carry = false;
		if (regs.a > 0x99 || (regs.f & C_FLAG) != 0) {
			delta |= 0x60;
			carry = true;
		}
		if ((regs.a & 0x0f) > 9 || (regs.f & H_FLAG) != 0) {
			delta |= 0x06;
		}
		int a = regs.a;
		if ((regs.f & N_FLAG) != 0) {
			regs.a -= delta;
		} else {
		  	regs.a += delta;
	  	}
		regs.a &= 0xFF;
		setFlag(H_FLAG, ((a ^ regs.a) & H_FLAG) != 0);
		setFlag(C_FLAG, carry);
		setFlag(S_FLAG, (regs.a & S_FLAG) != 0);
		setFlag(Z_FLAG, regs.a == 0);
		setFlag(P_FLAG, PARITY[regs.a]);
	}

	private void setFlag(int flag, boolean set) {
		if(set) {
			regs.f |= flag;
		} else {
			regs.f &= ~flag;
		}
	}

	private void jumpRelative() {
		int i = regs.pc - 1;
		rumpRelative();
		int j = regs.pc;
		if (j == i) {
			if (iCount > 0) iCount = 7;
		} else if ((j == i - 1) && (readByte(j) == 0xfb)) {
			if (iCount > 4) iCount = 11;
		}
	}

	private void popAF() {
		regs.af = pop();
		regs.a = regs.af >> 8;
		regs.f = regs.af & 0xFF;
	}

	private void ldAI() {
		regs.a = regs.i;
		regs.f = (regs.f & C_FLAG) | zsTable[regs.i] | (regs.iff2 << 2);
	}

	private void ldAR() {
		regs.a = (regs.r & 127) | (regs.r2 & 128);
		regs.f = (regs.f & C_FLAG) | zsTable[regs.a] | (regs.iff2 << 2);
	}

	private void rrd() {
		int i = readMem(regs.hl.w);
		writeByte(regs.hl.w, ((i >> 4) | (regs.a << 4)) & 0xFF);
		regs.a = ((regs.a & 0xF0) | (i & 0x0F)) & 0xFF;
		regs.f = (regs.f & C_FLAG) | zspTable[regs.a];
	}

	private void rld() {
		int i = readMem(regs.hl.w);
		writeByte(regs.hl.w, ((i << 4) | (regs.a & 0x0F)) & 0xFF);
		regs.a = ((regs.a & 0xF0) | (i >> 4)) & 0xFF;
		regs.f = (regs.f & C_FLAG) | zspTable[regs.a];
	}

	private void ldi() {
		writeByte(regs.de.w, readMem(regs.hl.w));
		regs.de.addW(1);
		regs.hl.addW(1);
		regs.bc.addW(-1);
		regs.f = (regs.f & 0xE9) | (regs.bc.w != 0 ? P_FLAG : 0);
	}

	private void cpi() {
		int i = readMem(regs.hl.w);
		int j = (regs.a - i) & 0xFF;
		regs.hl.addW(1);
		regs.bc.addW(-1);
		regs.f = (regs.f & C_FLAG)
				| zsTable[j]
				| ((regs.a ^ i ^ j) & H_FLAG)
				| (regs.bc.w != 0 ? P_FLAG : 0)
				| N_FLAG;
	}

	private void ini() {
		System.err.println("EDA2 Not implemented.");
	}

	private void outi() {
		portOut(regs.bc.l, readMem(regs.hl.w));
		regs.hl.addW(1);
		regs.bc.addH(-1);
		regs.f = (regs.bc.h != 0) ? N_FLAG : (Z_FLAG | N_FLAG);
	}

	private void ldd() {
		writeByte(regs.de.w, readMem(regs.hl.w));
		regs.de.addW(-1);
		regs.hl.addW(-1);
		regs.bc.addW(-1);
		regs.f = (regs.f & 0xE9) | (regs.bc.w != 0 ? P_FLAG : 0);
	}

	private void cpd() {
		System.err.println("EDA9 Not implemented.");
	}

	private void ind() {
		System.err.println("EDAA Not implemented.");
	}

	private void outd() {
		System.err.println("EDAB Not implemented.");
	}

	private void ldir() {
		regs.r -= 2;
		do {
			regs.r += 2;
			writeByte(regs.de.w, readMem(regs.hl.w));
			regs.de.addW(1);
			regs.hl.addW(1);
			regs.bc.addW(-1);
			iCount -= 21;
		} while (regs.bc.w != 0 && iCount > 0);
		regs.f = (regs.f & 0xE9) | (regs.bc.w != 0 ? P_FLAG : 0);
		if (regs.bc.w != 0) {
			regs.pc = (regs.pc - 2) & 0xFFFF;
		} else {
			iCount += 5;
		}
	}

	private void cpir() {
		int i, j;
		regs.r -= 2;
		do {
			regs.r += 2;
			i = readMem(regs.hl.w);
			j = (regs.a - i) & 0xFF;
			regs.hl.addW(1);
			regs.bc.addW(-1);
			iCount -= 21;
		} while (regs.bc.w != 0 && j != 0 && iCount > 0);
		regs.f = (regs.f & C_FLAG)
				| zsTable[j]
				| ((regs.a ^ i ^ j) & H_FLAG)
				| (regs.bc.w != 0 ? P_FLAG : 0)
				| N_FLAG;
		if (regs.bc.w != 0 && j != 0) {
			regs.pc = (regs.pc - 2) & 0xFFFF;
		} else {
			iCount += 5;
		}
	}

	private void inir() {
		System.err.println("EDB2 Not implemented.");
	}

	private void otir() {
		regs.r -= 2;
		do {
			regs.r += 2;
			portOut(regs.bc.l, readMem(regs.hl.w));
			regs.hl.addW(1);
			regs.bc.addH(-1);
			iCount -= 21;
		} while (regs.bc.h != 0 && iCount > 0);
		regs.f = (regs.bc.h != 0) ? N_FLAG : (Z_FLAG | N_FLAG);
		if (regs.bc.h != 0) {
			regs.pc = (regs.pc - 2) & 0xFFFF;
		} else {
			iCount += 5;
		}
	}

	private void lddr() {
		regs.r -= 2;
		do {
			regs.r += 2;
			writeByte(regs.de.w, readMem(regs.hl.w));
			regs.de.addW(-1);
			regs.hl.addW(-1);
			regs.bc.addW(-1);
			iCount -= 21;
		} while (regs.bc.w != 0 && iCount > 0);
		regs.f = (regs.f & 0xE9) | (regs.bc.w != 0 ? P_FLAG : 0);
		if (regs.bc.w != 0) {
			regs.pc = (regs.pc - 2) & 0xFFFF;
		} else {
			iCount += 5;
		}
	}

	private void cpdr() {
		int i, j;
		regs.r -= 2;
		do {
			regs.r += 2;
			i = readMem(regs.hl.w);
			j = (regs.a - i) & 0xFF;
			regs.hl.addW(-1);
			regs.bc.addW(-1);
			iCount -= 21;
		} while (regs.bc.w != 0 && j != 0 && iCount > 0);
		regs.f = (regs.f & C_FLAG)
				| zsTable[j]
				| ((regs.a ^ i ^ j) & H_FLAG)
				| (regs.bc.w != 0 ? P_FLAG : 0)
				| N_FLAG;
		if (regs.bc.w != 0 && j != 0) {
			regs.pc = (regs.pc - 2) & 0xFFFF;
		} else {
			iCount += 5;
		}
	}

	private void indr() {
		System.err.println("EDBA Not implemented.");
	}

	private void otdr() {
		System.err.println("EDBB Not implemented.");
	}

	private void exDEHL() {
		int i = regs.de.w;
		regs.de.setW(regs.hl.w);
		regs.hl.setW(i);
	}

	private void exAFAF() {
		int i = (regs.a << 8) | regs.f;
		regs.a = (regs.af2 >> 8);
		regs.f = (regs.af2 & 0xFF);
		regs.af2 = i;
	}

	private void exXspIX() {
		int i = readWord(regs.sp);
		writeWord(regs.sp, regs.ix.w);
		regs.ix.setW(i);
	}

	public void reset() {
		regs.af = regs.pc   = regs.sp   = regs.a    = regs.f
				= regs.af2  = regs.bc2  = regs.de2  = regs.hl2
				= regs.iff1 = regs.iff2 = regs.halt = regs.im
				= regs.i    = regs.r    = regs.r2   = regs.r = 0;
		regs.bc.setW(0);
		regs.de.setW(0);
		regs.hl.setW(0);
		regs.ix.setW(0);
		regs.iy.setW(0);
		regs.sp = 0xF000;
		clearPendingInterrupts();
	}

	private void initTables() {
		for (int i = 0; i < 256; i++) {
			int zs = 0;
			int p = 0;
			if (i == 0) zs |= Z_FLAG;
			if ((i & 0x80) != 0) zs |= S_FLAG;
			if ((i & 1) != 0)   p++;
			if ((i & 2) != 0)   p++;
			if ((i & 4) != 0)   p++;
			if ((i & 8) != 0)   p++;
			if ((i & 16) != 0)  p++;
			if ((i & 32) != 0)  p++;
			if ((i & 64) != 0)  p++;
			if ((i & 128) != 0) p++;
			pTable  [i]       = ((p & 1) != 0) ? 0 : P_FLAG;
			zsTable [i]       = zs;
			zspTable[i]       = zs | pTable[i];
			zsTable [i + 256] = zsTable[i]  | C_FLAG;
			zspTable[i + 256] = zspTable[i] | C_FLAG;
			pTable  [i + 256] = pTable[i]   | C_FLAG;
		}
	}

	private void interrupt() {
		if (regs.pending_irq == IGNORE_INT && regs.pending_nmi == 0) {
			return;
		}
		if (regs.pending_nmi != 0 || regs.iff1 != 0)  {
			regs.iff1 = 0;
			if (regs.halt != 0) {
				incPC();
				regs.halt = 0;
			}
			if (regs.pending_nmi != 0){
				regs.pending_nmi = 0;
				push(regs.pc);
				regs.pc = 0x0066;
			} else {
				int j = regs.pending_irq;
				regs.pending_irq = IGNORE_INT;
				if (regs.im == 2) {
					push(regs.pc);
					regs.pc = readWord((j & 255) | (regs.i << 8));
				} else if (regs.im == 1) {
					iCount -= op_main[0xFF].call();
				} else {
					switch (j & 0xFF0000) {
						case 0xCD0000:
							push(regs.pc);
						case 0xC30000:
							regs.pc = j & 0xFFFF;
							break;
						default:
							j &= 0xFF;
							iCount -= op_main[j].call();
							break;
					}
				}
			}
		}
	}

	public void setRegs(Z80Regs regs) {
		this.regs.af = regs.af;
		this.regs.pc = regs.pc;
		this.regs.sp = regs.sp;
		this.regs.a = regs.a;
		this.regs.f = regs.f;
		this.regs.bc.setW(regs.bc.w);
		this.regs.de.setW(regs.de.w);
		this.regs.hl.setW(regs.hl.w);
		this.regs.ix.setW(regs.ix.w);
		this.regs.iy.setW(regs.iy.w);
		this.regs.af2 = regs.af2;
		this.regs.bc2 = regs.bc2;
		this.regs.de2 = regs.de2;
		this.regs.hl2 = regs.hl2;
		this.regs.iff1 = regs.iff1;
		this.regs.iff2 = regs.iff2;
		this.regs.halt = regs.halt;
		this.regs.im = regs.im;
		this.regs.i = regs.i;
		this.regs.r = regs.r;
		this.regs.r2 = regs.r2;
		this.regs.pending_irq = regs.pending_irq;
		this.regs.pending_nmi = regs.pending_nmi;
	}

	public Z80Regs getRegs() {
		return regs.copy();
	}

	public void causeInterrupt(int type) {
		regs.pending_irq = type;
	}

	private void clearPendingInterrupts() {
		regs.pending_irq = IGNORE_INT;
		regs.pending_nmi = 0;
	}

	private final static boolean PARITY[] = {
			true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
			false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
			false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
			true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
			false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
			true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
			true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
			false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
			false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
			true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
			true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
			false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
			true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
			false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
			false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
			true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true
	};

	private interface Op {
		int call();
	}

}
