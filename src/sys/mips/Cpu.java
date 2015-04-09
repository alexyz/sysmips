package sys.mips;

import static sys.mips.Mips.*;

import java.util.*;

public final class Cpu {

	/** general purpose registers, and hi/lo */
	private final int[] reg = new int[34];
	/** coprocessor 0 registers */
	private final int[][] cpReg = new int[32][8];
	/** coprocessor 1 registers */
	private final int[] fpReg = new int[34];
	private final int[] fpControlReg = new int[32];
	private final Memory memory = new Memory();
	private final CpuLogger logger = new CpuLogger(this);

	/** address of current instruction */
	private int pc;
	/** address of current instruction + 4 (unless changed by branch) */
	private int nextPc;
	private int cycle;
	private int rmwAddress;
	private Round round = Round.NONE;
	private boolean fccr;

	public Cpu () {
		cpReg[STATUS_CPR][STATUS_SEL] = (3 << Status.CU) | (1 << Status.BEV) | (1 << Status.ERL);
		// #define PRID_COMP_LEGACY 0x000000
		// #define PRID_IMP_R2000 0x0100
		// #define PRID_REV_R2000A 0x0010
		cpReg[PRID_CPR][PRID_SEL] = 0x0110;
		// ((cpu_get_fpu_id() & 0xff00) != FPIR_IMP_NONE);
		// #define MIPS_FPIR_S (_ULCAST_(1) << 16)
		// #define MIPS_FPIR_D (_ULCAST_(1) << 17)
		// #define MIPS_FPIR_W (_ULCAST_(1) << 20)
		fpControlReg[FIR_FCR] = 0x130100;
	}

	private final void execCpFn (final int isn) {
		final int fn = isn & 0x3f;
		throw new RuntimeException("invalid coprocessor fn " + fn);
	}

	private void execCpRs (final int isn) {
		final int rs = (isn >>> 21) & 0x1f;
		final int rt = (isn >>> 16) & 0x1f;
		final int rd = (isn >>> 11) & 0x1f;
		final int sel = isn & 0x7;

		switch (rs) {
			case CP_RS_MFC0: {
				boolean ok = false;
				switch (rd) {
					case STATUS_CPR:
						switch (sel) {
							case STATUS_SEL:
								ok = true;
								break;
						}
						break;
					case PRID_CPR:
						switch (sel) {
							case PRID_SEL:
								ok = true;
								break;
						}
						break;
				}
				if (!ok) {
					throw new RuntimeException("move from unknown cp reg " + rd + ", " + sel);
				}
				final int val = cpReg[rd][sel];
				reg[rt] = val;
				// System.out.println("mfc0 " + rd + "." + sel + " -> 0x" +
				// Integer.toHexString(val));
				return;
			}
			case CP_RS_MTC0: {
				final int val = cpReg[rd][sel];
				final int newVal = reg[rt];
				boolean ok = false;
				switch (rd) {
					case STATUS_CPR:
						switch (sel) {
							case STATUS_SEL:
								ok = true;
								break;
						}
						break;
				}
				if (val != newVal) {
					System.out.println("mtc0 " + rd + "." + sel + " 0x" + Integer.toHexString(val) + " -> 0x" + Integer.toHexString(newVal));
					if (!ok) {
						throw new RuntimeException("move to unknown cp reg " + rd + ", " + sel);
					}
					cpReg[rd][sel] = newVal;
				}
				return;
			}

			default:
				throw new RuntimeException("invalid coprocessor rs " + rs);
		}
	}

	private final void execFn (final int isn) {
		final int rd = (isn >>> 11) & 0x1f;
		final int rt = (isn >>> 16) & 0x1f;
		final int rs = (isn >>> 21) & 0x1f;
		final int fn = isn & 0x3f;

		switch (fn) {
			case FN_SYSCALL:
				throw new RuntimeException();
			case FN_SLL:
				reg[rd] = reg[rt] << sa(isn);
				return;
			case FN_SRL: // shift word right logical
				reg[rd] = reg[rt] >>> sa(isn);
				return;
			case FN_SRA: // shift word right arithmetic (preserve sign)
				reg[rd] = reg[rt] >> sa(isn);
				return;
			case FN_SRLV: // shift word right logical variable (shift amount in
				// reg[rs])
				reg[rd] = reg[rt] >>> (reg[rs] & 0x1f);
				return;
			case FN_SRAV: // shift word right arithmetic variable (shift amount
				// in reg[rs], propagate sign).
				reg[rd] = reg[rt] >> (reg[rs] & 0x1f);
				return;
			case FN_SLLV: // shift word left logical variable (low order 5 bits
				// of RS)
				reg[rd] = reg[rt] << (reg[rs] & 0x1f);
				return;
			case FN_JR:
				nextPc = reg[rs];
				if (rs == 31) {
					logger.ret();
				}
				return;
			case FN_JALR:
				reg[rd] = nextPc;
				nextPc = reg[rs];
				logger.call(nextPc);
				return;
			case FN_MOVZ:
				if (reg[rt] == 0) {
					reg[rd] = reg[rs];
				}
				return;
			case FN_MOVN:
				if (reg[rt] != 0) {
					reg[rd] = reg[rs];
				}
				return;
			case FN_MFHI:
				reg[rd] = reg[HI_GPR];
				return;
			case FN_MTHI:
				reg[HI_GPR] = reg[rs];
				return;
			case FN_MFLO:
				reg[rd] = reg[LO_GPR];
				return;
			case FN_MTLO:
				reg[LO_GPR] = reg[rd];
				return;
			case FN_MULT: { // multiply 32 bit signed integers. 64 bit result.
				final long res = ((long) reg[rs]) * ((long) reg[rt]);
				reg[LO_GPR] = ((int) (res & 0xffffffffL));
				reg[HI_GPR] = ((int) ((res >>> 32) & 0xffffffffL));
				return;
			}
			case FN_MULTU: { // mul unsigned integers
				final long res = (reg[rs] & 0xffffffffL) * (reg[rt] & 0xffffffffL);
				reg[LO_GPR] = (int) (res & 0xffffffffL);
				reg[HI_GPR] = (int) ((res >>> 32) & 0xffffffffL);
				return;
			}
			case FN_DIV: // divide 32 bit signed integers
				// result is unpredictable for zero, no exceptions thrown
				if (rt != 0) {
					reg[LO_GPR] = reg[rs] / reg[rt];
					reg[HI_GPR] = reg[rs] % reg[rt];
				}
				return;
			case FN_DIVU: { // divide unsigned word
				// unpredictable result and no exception for zero
				if (reg[rt] != 0) {
					// use long so a and b are unsigned
					final long a = (reg[rs] & 0xffffffffL), b = (reg[rt] & 0xffffffffL);
					reg[LO_GPR] = (int) (a / b); // shouldnt be signed
					reg[HI_GPR] = (int) (a % b);
				}
				return;
			}
			case FN_ADDU: // add unsigned word
				reg[rd] = reg[rs] + reg[rt];
				return;
			case FN_SUBU: // subtract unsigned word
				reg[rd] = reg[rs] - reg[rt];
				return;
			case FN_AND:
				reg[rd] = reg[rs] & reg[rt];
				return;
			case FN_OR:
				reg[rd] = reg[rs] | reg[rt];
				return;
			case FN_XOR:
				reg[rd] = reg[rs] ^ reg[rt];
				return;
			case FN_NOR:
				reg[rd] = ~(reg[rs] | reg[rt]);
				return;
			case FN_SLT:
				reg[rd] = (reg[rs] < reg[rt]) ? 1 : 0;
				return;
			case FN_SLTU:
				reg[rd] = ((reg[rs] & 0xffffffffL) < (reg[rt] & 0xffffffffL)) ? 1 : 0;
				return;
			case FN_TNE:
				if (reg[rs] != reg[rt]) {
					throw new RuntimeException("trap");
				}
				return;
		}

		throw new RuntimeException("invalid fn " + fn);
	}

	private final void execFn2 (int isn) {
		final int rs = (isn >>> 21) & 0x1f;
		final int rt = (isn >>> 16) & 0x1f;
		final int rd = (isn >>> 11) & 0x1f;
		final int fn = isn & 0x3f;

		switch (fn) {
			case FN2_MUL:
				reg[rd] = reg[rs] * reg[rt];
				return;
			default:
				throw new RuntimeException("invalid fn2 " + fn);
		}
	}

	private void execFpuFn (final int isn, final Access access) {
		final int fs = fs(isn);
		final int ft = ft(isn);
		final int fd = fd(isn);
		final int fn = fn(isn);

		switch (fn) {
			case FP_FN_ADD_D: {
				final double fsValue = access.get(fpReg, fs);
				final double ftValue = access.get(fpReg, ft);
				access.set(fpReg, fd, round.round(fsValue + ftValue));
				return;
			}
			case FP_FN_SUB_D: {
				final double fsValue = access.get(fpReg, fs);
				final double ftValue = access.get(fpReg, ft);
				access.set(fpReg, fd, round.round(fsValue - ftValue));
				return;
			}
			case FP_FN_MUL_D: {
				final double fsValue = access.get(fpReg, fs);
				final double ftValue = access.get(fpReg, ft);
				access.set(fpReg, fd, round.round(fsValue * ftValue));
				return;
			}
			case FP_FN_DIV_D: {
				final double fsValue = access.get(fpReg, fs);
				final double ftValue = access.get(fpReg, ft);
				access.set(fpReg, fd, round.round(fsValue / ftValue));
				return;
			}
			case FP_FN_ABS_D: {
				final double fsValue = access.get(fpReg, fs);
				access.set(fpReg, fd, StrictMath.abs(fsValue));
				return;
			}
			case FP_FN_MOV_D: {
				final double fsValue = access.get(fpReg, fs);
				access.set(fpReg, fd, fsValue);
				return;
			}
			case FP_FN_NEG_D: {
				final double fsValue = access.get(fpReg, fs);
				access.set(fpReg, fd, -fsValue);
				return;
			}
			case FP_FN_CVT_S: {
				final double fsValue = access.get(fpReg, fs);
				setSingle(fpReg, fd, (float) round.round(fsValue));
				return;
			}
			case FP_FN_CVT_D: {
				final double fsValue = access.get(fpReg, fs);
				setDouble(fpReg, fd, round.round(fsValue));
				return;
			}
			case FP_FN_CVT_W: {
				final double fsValue = access.get(fpReg, fs);
				fpReg[fd] = (int) round.round(fsValue);
				return;
			}
			case FP_FN_C_ULT: {
				final double fsValue = access.get(fpReg, fs);
				final double ftValue = access.get(fpReg, ft);
				fccr = Double.isNaN(fsValue) || Double.isNaN(ftValue) || fsValue < ftValue;
				return;
			}
			case FP_FN_C_EQ: {
				final double fsValue = access.get(fpReg, fs);
				final double ftValue = access.get(fpReg, ft);
				fccr = fsValue == ftValue;
				return;
			}
			case FP_FN_C_LT: {
				final double fsValue = access.get(fpReg, fs);
				final double ftValue = access.get(fpReg, ft);
				fccr = fsValue < ftValue;
				return;
			}
			case FP_FN_C_LE: {
				final double fsValue = access.get(fpReg, fs);
				final double ftValue = access.get(fpReg, ft);
				fccr = fsValue <= ftValue;
				return;
			}
			default:
				throw new RuntimeException("invalid fpu fn " + fn);
		}
	}

	private void execFpuRs (final int isn) {
		final int rs = rs(isn);
		final int rt = rt(isn);
		final int fs = fs(isn);

		switch (rs) {
			case FP_RS_MFC1:
				reg[rt] = fpReg[fs];
				return;

			case FP_RS_MTC1:
				fpReg[fs] = reg[rt];
				return;

			case FP_RS_S:
				execFpuFn(isn, Access.SINGLE);
				return;

			case FP_RS_D:
				execFpuFn(isn, Access.DOUBLE);
				return;

			case FP_RS_W:
				execFpuFn(isn, Access.WORD);
				return;

			case FP_RS_BC1:
				if (fpTrue(isn) == fccr) {
					nextPc = branch(isn, pc);
				} else {
					// don't execute delay slot
					nextPc += 4;
				}
				return;

			case FP_RS_CFC1:
				cfc1: switch (fs) {
					case FCSR_FCR:
					case FIR_FCR:
						break cfc1;
					default:
						throw new RuntimeException("read unimplemented fp control register " + fs);
				}
			reg[rt] = fpControlReg[fs];
			return;

			case FP_RS_CTC1:
				// move control word to floating point. 31=fcsr
				if ((reg[rt] > 1) || (fs != 31)) {
					throw new RuntimeException("write unimplemented fp control register " + fs);
				}
				setfcsr(reg[rt]);
				return;

			default:
				throw new RuntimeException("invalid fpu rs " + rs);
		}

	}

	private final void execOp (final int isn) {
		/*
		 * Instructions should RETURN unless they need a relative branch in
		 * which case they should BREAK. A bit ugly but faster.
		 */
		final int op = isn >>> 26;
		final int rs = (isn >>> 21) & 0x1f;
		final int rt = (isn >>> 16) & 0x1f;
		final short simm = (short) isn;

		switch (op) {
			case OP_REGIMM:
				switch (rt) {
					case RT_BGEZAL:
						reg[31] = nextPc;
						System.out.println("RT_BGEZAL " + memory.getSymbols().getName(nextPc));
						// fall through
					case RT_BGEZ:
						if (reg[rs] >= 0) {
							break;
						}
						return;
					case RT_BLTZAL:
						reg[31] = nextPc;
						System.out.println("RT_BLTZAL " + memory.getSymbols().getName(nextPc));
						// fall through
					case RT_BLTZ:
						if (reg[rs] < 0) {
							break;
						}
						return;
					default:
						throw new RuntimeException("invalid regimm " + rt);
				}
				break;
			case OP_COP0:
				if (rs < 16) {
					execCpRs(isn);
				} else {
					execCpFn(isn);
				}
				return;
			case OP_COP1:
				execFpuRs(isn);
				return;
			case OP_SPEC2:
				execFn2(isn);
				return;
			case OP_LWC1:
				fpReg[rt] = memory.loadWord(reg[rs] + simm);
				return;
			case OP_SWC1:
				memory.storeWord(reg[rs] + simm, fpReg[rt]);
				return;
			case OP_J:
				nextPc = jump(isn, pc);
				return;
			case OP_JAL:
				reg[31] = nextPc;
				nextPc = jump(isn, pc);
				logger.call(nextPc);
				return;
			case OP_BLEZ:
				if (reg[rs] <= 0) {
					break;
				}
				return;
			case OP_BEQ:
				if (reg[rs] == reg[rt]) {
					break;
				}
				return;
			case OP_BNE:
				if (reg[rs] != reg[rt]) {
					break;
				}
				return;
			case OP_ADDIU:
				reg[rt] = reg[rs] + simm;
				return;
			case OP_ANDI:
				reg[rt] = reg[rs] & (simm & 0xffff);
				return;
			case OP_XORI:
				reg[rt] = reg[rs] ^ (simm & 0xffff);
				return;
			case OP_BGTZ:
				if (reg[rs] > 0) {
					break;
				}
				return;
			case OP_SLTI:
				reg[rt] = reg[rs] < simm ? 1 : 0;
				return;
			case OP_SLTIU:
				reg[rt] = ((reg[rs] & 0xffffffffL) < (simm & 0xffffffffL)) ? 1 : 0;
				return;
			case OP_ORI:
				reg[rt] = reg[rs] | (simm & 0xffff);
				return;
			case OP_SW:
				memory.storeWord(reg[rs] + simm, reg[rt]);
				return;
			case OP_SH: // store halfword
				memory.storeHalfWord(reg[rs] + simm, ((short) (reg[rt] & 0xffff)));
				return;
			case OP_SB:
				memory.storeByte(reg[rs] + simm, ((byte) (reg[rt] & 0xff)));
				return;
			case OP_LUI: // load upper immediate
				reg[rt] = simm << 16;
				return;
			case OP_LL: {
				// begin rmw
				final int a = reg[rs] + simm;
				rmwAddress = a;
				reg[rt] = memory.loadWord(a);
				return;
			}
			case OP_SC: {
				final int a = reg[rs] + simm;
				// should also fail if another cpu does a store to the same
				// block of memory in same page
				// or the same processor if Config5LLB=1
				if (a == rmwAddress) {
					memory.storeWord(a, reg[rt]);
					reg[rt] = 1;
					rmwAddress = 0;
				} else {
					System.out.println("sc fail: a=" + a + " rmwAddress=" + rmwAddress);
					reg[rt] = 0;
				}
				return;
			}
			case OP_LW:
				reg[rt] = memory.loadWord(reg[rs] + simm);
				return;
			case OP_LB:
				reg[rt] = memory.loadByte(reg[rs] + simm);
				return;
			case OP_LBU:
				reg[rt] = memory.loadByte(reg[rs] + simm) & 0xff;
				return;
			case OP_LHU:
				reg[rt] = (memory.loadHalfWord(reg[rs] + simm) & 0xffff);
				return;
			case OP_LH:
				reg[rt] = memory.loadHalfWord(reg[rs] + simm);
				return;
			case OP_LWL: {
				final int a = reg[rs] + simm;
				final int s = (a & 3) << 3;
				reg[rt] = (memory.loadWord(a & ~3) << s) | (reg[rt] & ((int) (0xffffffffL >>> (32 - s))));
				return;
			}
			case OP_LWR: { // load word right. least signicant byte at eff.addr.
				final int a = reg[rs] + simm;
				final int s = ((a & 3) + 1) << 3;
				reg[rt] = (memory.loadWord(a & ~3) >>> (32 - s)) | (reg[rt] & ((int) (0xffffffffL << s)));
				return;
			}
			case OP_SWL: {
				final int a = reg[rs] + simm; // msb
				final int b = a & ~3; // aligned address
				final int s = (a & 3) << 3;
				memory.storeWord(b, (reg[rt] >>> s) | (memory.loadWord(b) & ((int) (0xffffffffL << (32 - s)))));
				return;
			}
			case OP_SWR: {
				final int a = reg[rs] + simm; // lsb
				final int b = a & ~3;
				final int s = ((a & 3) + 1) << 3;
				memory.storeWord(b, (reg[rt] << (32 - s)) | (memory.loadWord(b) & ((int) (0xffffffffL >>> s))));
				return;
			}

			default:
				throw new RuntimeException("invalid op " + op);
		}

		nextPc = branch(isn, pc);
	}

	public int[][] getCpRegisters () {
		return cpReg;
	}

	public int getCycle () {
		return cycle;
	}

	public Memory getMemory () {
		return memory;
	}

	public int getNextPc () {
		return nextPc;
	}

	public int getPc () {
		return pc;
	}

	public int[] getRegisters () {
		return reg;
	}

	public final void run () throws Exception {
		// BufferedReader br = new BufferedReader(new
		// InputStreamReader(System.in));
		final LinkedList<String> log = new LinkedList<>();

		try {
			while (true) {
				while (log.size() > 100) {
					log.removeFirst();
				}
				// log.add(cpRegString(this));
				// log.add(gpRegString(this));
				log.add(cycle + ": " + isnString(this));

				if ((cycle & 0xffff) == 0) {
					System.out.println("cycle " + cycle);
				}
				if (reg[0] != 0) {
					throw new RuntimeException("reg 0 failure");
				}
				final int isn = memory.loadWord(pc);
				pc = nextPc;
				nextPc += 4;
				if (op(isn) != 0) {
					execOp(isn);
				} else {
					execFn(isn);
				}

				cycle++;
				// br.readLine();
			}

		} catch (final Exception e) {
			for (final String s : log) {
				System.out.println(s);
			}
			e.printStackTrace(System.out);
		}
	}

	/**
	 * Set the value and associated values of the condition and status register
	 */
	private void setfcsr (int fcsr) {
		fpControlReg[31] = fcsr;
		if ((fcsr & ~0x3) != 0) {
			throw new RuntimeException("setfcsr: unknown mode %x\n");
		}
		final int rm = fcsr & 0x3;
		if (rm == FCSR_RM_RN) {
			round = Round.NONE;
		} else if (rm == FCSR_RM_RZ) {
			round = Round.ZERO;
		} else if (rm == FCSR_RM_RP) {
			round = Round.POSINF;
		} else if (rm == FCSR_RM_RM) {
			round = Round.NEGINF;
		}
	}

	public void setPc (int pc) {
		this.pc = pc;
		this.nextPc = pc + 4;
	}
}
