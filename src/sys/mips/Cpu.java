package sys.mips;

import java.util.*;
import java.util.stream.Collectors;

import static sys.mips.MipsConstants.*;
import static sys.mips.Decoder.*;
import static sys.mips.IsnUtil.*;

public class Cpu {
	
	/** general purpose registers, and hi/lo */
	private final int[] reg = new int[34];
	/** coprocessor 0 registers */
	private final int[] cpReg = new int[64];
	/**
	 * coprocessor 1 registers (longs/doubles in consecutive registers, least
	 * significant word first!)
	 */
	private final int[] fpReg = new int[32];
	private final int[] fpControlReg = new int[32];
	private final CpuLogger log = new CpuLogger(this);
	private final Memory memory;
	
	/** address of current instruction */
	private int pc;
	/** address of current instruction + 4 (unless changed by branch) */
	private int nextPc;
	private int cycle;
	private int rmwPhysicalAddress;
	private FpRound roundingMode = FpRound.NONE;
	private boolean kernelMode = true;
	
	public Cpu (boolean littleEndian) {
		memory = new Memory(littleEndian);
		// linux_3.2.65\arch\mips\include\asm\cpu-features.h
		// linux_3.2.65\arch\mips\include\asm\cpu.h
		// linux_3.2.65\arch\mips\include\asm\mipsregs.h
		
		// default values on reboot
		cpReg[CPR_STATUS] = CPR_STATUS_EXL | CPR_STATUS_ERL | CPR_STATUS_BEV | CPR_STATUS_CU0 | CPR_STATUS_CU1;
		
		// 0x10000 = MIPS, 0x8000 = 4KC 
		cpReg[CPR_PRID] = 0x18000;
		
		// 15: big endian, 7: TLB
		cpReg[CPR_CONFIG0] = (1 << 31) | ((littleEndian?0:1) << 15) | (1 << 7) | (1 << 1);
		
		// 25: tlb entries - 1
		// should enable 3: watch registers and 1: ejtag, but we don't want them
		cpReg[CPR_CONFIG1] = (15 << 25);
		
		// support S, D, W, L (unlike the 4kc...)
		fpControlReg[FPCR_FIR] = (1 << 16) | (1 << 17) | (1 << 20) | (1 << 21) | (1 << 8);
	}
	
	public int[] getCpRegisters () {
		return cpReg;
	}
	
	public int getCycle () {
		return cycle;
	}
	
	public Memory getMemory () {
		return memory;
	}
	
	public int getPc () {
		return pc;
	}
	
	public void setPc (int pc) {
		this.pc = pc;
		this.nextPc = pc + 4;
	}
	
	public int getNextPc () {
		return nextPc;
	}
	
	public int[] getRegisters () {
		return reg;
	}
	
	public int getRegister (int n) {
		return reg[n];
	}
	
	public void setRegister (int n, int value) {
		reg[n] = value;
	}
	
	public int getFpRegister (int n) {
		return fpReg[n];
	}
	
	public double getFpRegister (int n, FpFormat fmt) {
		return fmt.load(fpReg, n);
	}
	
	public CpuLogger getLog () {
		return log;
	}
	
	/** never returns, throws CpuException... */
	public final void run () {
		log.info("run");
		memory.print(System.out);
		final TreeMap<String, int[]> isnCount = new TreeMap<>();
		for (String name : IsnSet.INSTANCE.nameMap.keySet()) {
			isnCount.put(name, new int[1]);
		}
		
		try {
			while (true) {
				// log.add(cpRegString(this));
				// log.add(gpRegString(this));
				log.debug(IsnUtil.isnString(this));
				
				if (reg[0] != 0) {
					log.info("reg 0 not 0");
					reg[0] = 0;
				}
				final int isn = loadWord(pc);
				pc = nextPc;
				nextPc += 4;
				if (op(isn) != 0) {
					execOp(isn);
				} else {
					execFn(isn);
				}
				
				final Isn isnObj = IsnSet.INSTANCE.getIsn(isn);
				if (isnObj != null) {
					isnCount.get(isnObj.name)[0]++;
				}
				cycle++;
			}
		} catch (Exception e) {
			log.info("stop due to " + e);
			final List<String> l = isnCount.entrySet()
					.stream()
					.filter(x -> x.getValue()[0] > 0)
					.sorted((x,y) -> y.getValue()[0] - x.getValue()[0])
					.map(x -> x.getKey() + "=" + x.getValue()[0])
					.collect(Collectors.toList());
			log.debug("isn count " + l);
			System.out.println();
			log.print(System.out);
			throw new RuntimeException("stop in " + log.getCalls(), e);
		}
	}
	
	public int loadWord (final int addr) {
		return memory.loadWord(translate(addr));
	}
	
	public void storeWord (final int addr, final int value) {
		memory.storeWord(translate(addr), value);
	}
	
	public short loadHalfWord (final int addr) {
		return memory.loadHalfWord(translate(addr));
	}
	
	public void storeHalfWord (final int addr, final short value) {
		memory.storeHalfWord(translate(addr), value);
	}
	
	public byte loadByte (final int addr) {
		return memory.loadByte(translate(addr));
	}
	
	public void storeByte (final int addr, final byte value) {
		memory.storeByte(translate(addr), value);
	}
	
	/**
	 * translate virtual address to physical
	 */
	private final int translate (int addr) {
//		private static final int KSSEG = 0xc000_0000;
//		private static final int KSEG0 = 0x8000_0000;
		// can only do signed compare...
		if (addr >= 0) {
			// from 0 to KSEG0 (2GB mark)
			return lookup(addr);
		} else if (kernelMode) {
			if (addr < 0xc000_000) {
				// from KSEG0 (2GB mark) to KSSEG (3GB mark)
				return addr;
			} else {
				throw new CpuException("unimplemented kernel translate " + Integer.toHexString(addr));
			}
		} else {
			throw new CpuException("invalid user translate " + Integer.toHexString(addr));
		}
	}
	
	protected int lookup (int addr) {
		throw new CpuException("unimplemented tlb " + Integer.toHexString(addr));
	}
	
	private final void execOp (final int isn) {
		final int[] reg = this.reg;
		final int op = op(isn);
		final int rs = rs(isn);
		final int rt = rt(isn);
		final int simm = simm(isn);
		
		switch (op) {
			case OP_REGIMM:
				execRegimm(isn);
				return;
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
			case OP_COP1X:
				execFpuFnX(isn);
				return;
			case OP_SPECIAL2:
				execFn2(isn);
				return;
			case OP_LWC1:
				fpReg[rt] = loadWord(reg[rs] + simm);
				return;
			case OP_LDC1:
				// least significant word first...
				fpReg[rt + 1] = loadWord(reg[rs] + simm);
				fpReg[rt] = loadWord(reg[rs] + simm + 4);
				return;
			case OP_SWC1:
				storeWord(reg[rs] + simm, fpReg[rt]);
				return;
			case OP_SDC1:
				// least significant word first...
				storeWord(reg[rs] + simm, fpReg[rt + 1]);
				storeWord(reg[rs] + simm + 4, fpReg[rt]);
				return;
			case OP_J:
				nextPc = jump(isn, pc);
				return;
			case OP_JAL:
				reg[31] = nextPc;
				nextPc = jump(isn, pc);
				log.call(nextPc);
				return;
			case OP_BLEZ:
				if (reg[rs] <= 0) {
					nextPc = branch(isn, pc);
				}
				return;
			case OP_BEQ:
				if (reg[rs] == reg[rt]) {
					nextPc = branch(isn, pc);
				}
				return;
			case OP_BNE:
				if (reg[rs] != reg[rt]) {
					nextPc = branch(isn, pc);
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
					nextPc = branch(isn, pc);
				}
				return;
			case OP_SLTI:
				reg[rt] = reg[rs] < simm ? 1 : 0;
				return;
			case OP_SLTIU: {
				// zero extend
				long rsValue = reg[rs] & 0xffffffffL;
				// zero extend the sign extended imm so it represents ends of
				// unsigned range
				long immValue = simm & 0xffffffffL;
				reg[rt] = (rsValue < immValue) ? 1 : 0;
				return;
			}
			case OP_ORI:
				reg[rt] = reg[rs] | (simm & 0xffff);
				return;
			case OP_SW:
				storeWord(reg[rs] + simm, reg[rt]);
				return;
			case OP_SH:
				storeHalfWord(reg[rs] + simm, (short) reg[rt]);
				return;
			case OP_SB:
				storeByte(reg[rs] + simm, (byte) reg[rt]);
				return;
			case OP_LUI:
				reg[rt] = simm << 16;
				return;
			case OP_LL: {
				// begin rmw
				final int pa = translate(reg[rs] + simm);
				rmwPhysicalAddress = pa;
				reg[rt] = memory.loadWord(pa);
				return;
			}
			case OP_SC: {
				final int pa = translate(reg[rs] + simm);
				// should also fail if another cpu does a store to the same
				// block of memory in same page
				// or the same processor if Config5LLB=1
				if (pa == rmwPhysicalAddress) {
					memory.storeWord(pa, reg[rt]);
					reg[rt] = 1;
					rmwPhysicalAddress = 0;
				} else {
					System.out.println("sc fail: a=" + pa + " rmwAddress=" + rmwPhysicalAddress);
					reg[rt] = 0;
				}
				return;
			}
			case OP_LW:
				reg[rt] = loadWord(reg[rs] + simm);
				return;
			case OP_LB:
				reg[rt] = loadByte(reg[rs] + simm);
				return;
			case OP_LBU:
				reg[rt] = loadByte(reg[rs] + simm) & 0xff;
				return;
			case OP_LHU:
				reg[rt] = loadHalfWord(reg[rs] + simm) & 0xffff;
				return;
			case OP_LH:
				reg[rt] = loadHalfWord(reg[rs] + simm);
				return;
			case OP_LWL: {
				// unaligned address
				final int a = reg[rs] + simm;
				// 0,1,2,3 -> 0,8,16,24
				final int s = (a & 3) << 3;
				final int w = loadWord(a & ~3);
				reg[rt] = (w << s) | (reg[rt] & (0xffffffff >>> (32 - s)));
				return;
			}
			case OP_LWR: {
				final int a = reg[rs] + simm;
				// 0,1,2,3 -> 8,16,24,32
				final int s = ((a & 3) + 1) << 3;
				final int w = loadWord(a & ~3);
				reg[rt] = (w >>> (32 - s)) | (reg[rt] & (0xffffffff << s));
				return;
			}
			case OP_SWL: {
				// unaligned addr
				final int a = reg[rs] + simm;
				// aligned address
				final int aa = a & ~3;
				final int s = (a & 3) << 3;
				final int w = loadWord(aa);
				storeWord(aa, (reg[rt] >>> s) | (w & (0xffffffff << (32 - s))));
				return;
			}
			case OP_SWR: {
				// unaligned addr
				final int a = reg[rs] + simm;
				// aligned addr
				final int aa = a & ~3;
				final int s = ((a & 3) + 1) << 3;
				final int w = loadWord(aa);
				storeWord(aa, (reg[rt] << (32 - s)) | (w & (0xffffffff >>> s)));
				return;
			}
			
			default:
				throw new RuntimeException("invalid op " + opString(op));
		}
	}
	
	private final void execRegimm (final int isn) {
		final int rs = rs(isn);
		final int rt = rt(isn);
		
		switch (rt) {
			case RT_BGEZAL:
				reg[31] = nextPc;
				// fall through
			case RT_BGEZ:
				if (reg[rs] >= 0) {
					nextPc = branch(isn, pc);
				}
				return;
			case RT_BLTZAL:
				reg[31] = nextPc;
				// fall through
			case RT_BLTZ:
				if (reg[rs] < 0) {
					nextPc = branch(isn, pc);
				}
				return;
			default:
				throw new RuntimeException("invalid regimm " + opString(rt));
		}
	}
	
	private final void execFn (final int isn) {
		final int rd = rd(isn);
		final int rt = rt(isn);
		final int rs = rs(isn);
		final int fn = fn(isn);
		
		switch (fn) {
			case FN_SLL:
				reg[rd] = reg[rt] << sa(isn);
				return;
			case FN_SRL:
				reg[rd] = reg[rt] >>> sa(isn);
				return;
			case FN_SRA:
				reg[rd] = reg[rt] >> sa(isn);
				return;
			case FN_SRLV:
				reg[rd] = reg[rt] >>> (reg[rs] & 0x1f);
				return;
			case FN_SRAV:
				reg[rd] = reg[rt] >> (reg[rs] & 0x1f);
				return;
			case FN_SLLV:
				reg[rd] = reg[rt] << (reg[rs] & 0x1f);
				return;
			case FN_JR:
				nextPc = reg[rs];
				if (rs == 31) {
					log.ret();
				}
				return;
			case FN_JALR:
				reg[rd] = nextPc;
				nextPc = reg[rs];
				log.call(nextPc);
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
			case FN_SYSCALL:
				execExn(SYSCALL_EX, syscall(isn));
				return;
			case FN_BREAK:
				execExn(BREAKPOINT_EX, syscall(isn));
				return;
			case FN_MFHI:
				reg[rd] = reg[REG_HI];
				return;
			case FN_MTHI:
				reg[REG_HI] = reg[rs];
				return;
			case FN_MFLO:
				reg[rd] = reg[REG_LO];
				return;
			case FN_MTLO:
				reg[REG_LO] = reg[rd];
				return;
			case FN_MULT: {
				// sign extend
				final long rsValue = reg[rs];
				final long rtValue = reg[rt];
				final long result = rsValue * rtValue;
				reg[REG_LO] = (int) result;
				reg[REG_HI] = (int) (result >>> 32);
				return;
			}
			case FN_MULTU: {
				// zero extend
				final long rsValue = reg[rs] & 0xffffffffL;
				final long rtValue = reg[rt] & 0xffffffffL;
				final long result = rsValue * rtValue;
				reg[REG_LO] = (int) result;
				reg[REG_HI] = (int) (result >>> 32);
				return;
			}
			case FN_DIV: {
				// divide as signed
				// result is unpredictable for zero, no exceptions thrown
				int rsValue = reg[rs];
				int rtValue = reg[rt];
				if (rt != 0) {
					reg[REG_LO] = rsValue / rtValue;
					reg[REG_HI] = rsValue % rtValue;
				}
				return;
			}
			case FN_DIVU: {
				// unpredictable result and no exception for zero
				// zero extend
				final long rsValue = reg[rs] & 0xffffffffL;
				final long rtValue = reg[rt] & 0xffffffffL;
				if (rtValue != 0) {
					reg[REG_LO] = (int) (rsValue / rtValue);
					reg[REG_HI] = (int) (rsValue % rtValue);
				}
				return;
			}
			case FN_ADDU:
				reg[rd] = reg[rs] + reg[rt];
				return;
			case FN_SUBU:
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
			case FN_SLTU: {
				// zero extend
				long rsValue = reg[rs] & 0xffffffffL;
				long rtValue = reg[rt] & 0xffffffffL;
				reg[rd] = rsValue < rtValue ? 1 : 0;
				return;
			}
			case FN_TNE:
				if (reg[rs] != reg[rt]) {
					execExn(TRAP_EX, 0);
				}
				return;
			default:
				throw new IllegalArgumentException("invalid fn " + opString(fn));
		}
	}
	
	protected void execExn(String type, int value) {
		throw new CpuException(type + " " + value);
	}
	
	private void execFn2 (final int isn) {
		final int rs = rs(isn);
		final int rt = rt(isn);
		final int rd = rd(isn);
		final int fn = fn(isn);
		
		switch (fn) {
			case FN2_MUL: {
				// sign extend, return bottom 32 bits of result
				long rsValue = reg[rs];
				long rtValue = reg[rt];
				reg[rd] = (int) (rsValue * rtValue);
				return;
			}
			case FN2_CLZ: {
				int value = reg[rs];
				int n = 0;
				while (n < 32 && (value & (1 << (31 - n))) == 0) {
					n++;
				}
				reg[rd] = n;
				return;
			}
			default:
				throw new RuntimeException("invalid fn2 " + opString(fn));
		}
	}
	
	/** execute system coprocessor instruction */
	private void execCpRs (final int isn) {
		final int rs = rs(isn);
		
		switch (rs) {
			case CP_RS_MFC0:
				execCpMfc0(isn);
				return;
			case CP_RS_MTC0:
				execCpMtc0(isn);
				return;
			default:
				throw new RuntimeException("invalid coprocessor rs " + opString(rs));
		}
	}
	
	private final void execCpMfc0 (final int isn) {
		final int rt = rt(isn);
		final int rd = rd(isn);
		final int sel = sel(isn);
		final int cpr = cpr(rd, sel);
		
		switch (cpr) {
			case CPR_STATUS:
			case CPR_PRID:
			case CPR_CONFIG0:
			case CPR_CONFIG1:
			case CPR_CAUSE:
				break;
			default:
				throw new RuntimeException("move from unknown cp reg " + cpRegName(rd, sel));
		}
		
		reg[rt] = cpReg[cpr];
		return;
	}
	
	/** move to system coprocessor register */
	private final void execCpMtc0 (final int isn) {
		final int rd = rd(isn);
		final int sel = sel(isn);
		final int cpr = cpr(rd, sel);
		final int value = cpReg[cpr];
		final int newValue = reg[rt(isn)];

		if (value != newValue) {
			log.info("mtc0 " + cpRegName(rd, sel) + " 0x" + Integer.toHexString(value) + " <- 0x" + Integer.toHexString(newValue));
			
			switch (cpr) {
				case CPR_CONTEXT:
					if (newValue != 0) {
						throw new RuntimeException("unknown ctx reg value " + Integer.toHexString(newValue));
					}
					return;
				case CPR_STATUS:
					setStatus(newValue);
					return;
				case CPR_CAUSE:
					setCause(newValue);
					break;
				default:
					throw new RuntimeException("move to unknown cp reg " + cpRegName(rd, sel));
			}
		}
	}

	private void setCause (final int regrt) {
		final int value = cpReg[CPR_CAUSE];
		int mask = CPR_CAUSE_IV;
		if ((regrt & ~mask) != 0) {
			throw new RuntimeException("unknown cause value " + Integer.toHexString(regrt));
		}
		cpReg[CPR_CAUSE] = (value & ~mask) | (regrt & mask);
	}

	private void setStatus (final int newValue) {
		final int mask = CPR_STATUS_CU1 | CPR_STATUS_CU0 | CPR_STATUS_BEV | CPR_STATUS_IM | CPR_STATUS_UM | CPR_STATUS_ERL | CPR_STATUS_EXL | CPR_STATUS_IE;
		if ((newValue & ~mask) != 0) {
			throw new RuntimeException("unknown status value " + Integer.toHexString(newValue));
		}
		// don't need to preserve anything
		cpReg[CPR_STATUS] = newValue & mask;
		
		boolean ie = (newValue & CPR_STATUS_IE) != 0;
		boolean exl = (newValue & CPR_STATUS_EXL) != 0;
		boolean erl = (newValue & CPR_STATUS_ERL) != 0;
		boolean um = (newValue & CPR_STATUS_UM) != 0;
		
		// kernel mode if UM = 0, or EXL = 1, or ERL = 1
		kernelMode = !um || exl || erl;
		if (!kernelMode) {
			throw new RuntimeException("user mode");
		}
		
		// interrupts enabled if IE = 1 and EXL = 0 and ERL = 0 and DM = 0 (debug mode)
		boolean interruptsEnabled = ie && !exl && !erl;
		if (interruptsEnabled) {
			throw new RuntimeException("interrupts enabled");
		}
	}

	private final void execCpFn (final int isn) {
		final int fn = fn(isn);
		// ...
		throw new RuntimeException("invalid coprocessor fn " + opString(fn));
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
				execFpuFn(isn, FpFormat.SINGLE);
				return;
				
			case FP_RS_D:
				execFpuFn(isn, FpFormat.DOUBLE);
				return;
				
			case FP_RS_W:
				execFpuFnWord(isn);
				return;
				
			case FP_RS_BC1:
				if (fptf(isn) == getFpCondition(fpcc(isn))) {
					nextPc = branch(isn, pc);
				} else {
					// don't execute delay slot
					nextPc += 4;
				}
				return;
				
			case FP_RS_CFC1:
				execFpuCfc1(isn);
				return;
				
			case FP_RS_CTC1:
				execFpuCtc1(isn);
				return;
				
			default:
				throw new RuntimeException("invalid fpu rs " + opString(rs));
		}
		
	}
	
	private void execFpuCfc1 (final int isn) {
		final int rt = rt(isn);
		final int fs = fs(isn);
		
		switch (fs) {
			case FPCR_FCSR:
			case FPCR_FCCR:
			case FPCR_FIR:
				reg[rt] = fpControlReg[fs];
				return;
				
			default:
				throw new RuntimeException("read unimplemented fp control register " + fs);
		}
	}
	
	private void execFpuCtc1 (final int isn) {
		final int rtValue = reg[rt(isn)];
		final int fs = fs(isn);
		
		switch (fs) {
			case FPCR_FCSR:
				if ((rtValue & ~0x3) != 0) {
					throw new RuntimeException("unknown fcsr %x\n");
				}
				break;
			
			default:
				throw new RuntimeException("write unimplemented fp control register " + fs + ", " + Integer.toHexString(rtValue));
		}
		
		fpControlReg[fs] = rtValue;
		roundingMode = FpRound.getInstance(fpControlReg[FPCR_FCSR]);
	}
	
	private void execFpuFn (final int isn, final FpFormat fmt) {
		final int[] fpReg = this.fpReg;
		final int fs = fs(isn);
		final int ft = ft(isn);
		final int fd = fd(isn);
		final int fn = fn(isn);
		
		switch (fn) {
			case FP_FN_ADD:
				fmt.store(fpReg, fd, roundingMode.round(fmt.load(fpReg, fs) + fmt.load(fpReg, ft)));
				return;
			case FP_FN_SUB:
				fmt.store(fpReg, fd, roundingMode.round(fmt.load(fpReg, fs) - fmt.load(fpReg, ft)));
				return;
			case FP_FN_MUL:
				fmt.store(fpReg, fd, roundingMode.round(fmt.load(fpReg, fs) * fmt.load(fpReg, ft)));
				return;
			case FP_FN_DIV:
				fmt.store(fpReg, fd, roundingMode.round(fmt.load(fpReg, fs) / fmt.load(fpReg, ft)));
				return;
			case FP_FN_ABS:
				fmt.store(fpReg, fd, StrictMath.abs(fmt.load(fpReg, fs)));
				return;
			case FP_FN_MOV:
				fmt.store(fpReg, fd, fmt.load(fpReg, fs));
				return;
			case FP_FN_NEG:
				fmt.store(fpReg, fd, 0.0 - fmt.load(fpReg, fs));
				return;
			case FP_FN_CVT_S:
				storeSingle(fpReg, fd, (float) roundingMode.round(fmt.load(fpReg, fs)));
				return;
			case FP_FN_CVT_D:
				storeDouble(fpReg, fd, roundingMode.round(fmt.load(fpReg, fs)));
				return;
			case FP_FN_CVT_W:
				fpReg[fd] = (int) roundingMode.round(fmt.load(fpReg, fs));
				return;
			case FP_FN_C_ULT: {
				final double fsValue = fmt.load(fpReg, fs);
				final double ftValue = fmt.load(fpReg, ft);
				setFpCondition(fpcc(isn), Double.isNaN(fsValue) || Double.isNaN(ftValue) || fsValue < ftValue);
				return;
			}
			case FP_FN_C_EQ:
				setFpCondition(fpcc(isn), fmt.load(fpReg, fs) == fmt.load(fpReg, ft));
				return;
			case FP_FN_C_LT:
				setFpCondition(fpcc(isn), fmt.load(fpReg, fs) < fmt.load(fpReg, ft));
				return;
			case FP_FN_C_LE:
				setFpCondition(fpcc(isn), fmt.load(fpReg, fs) <= fmt.load(fpReg, ft));
				return;
			default:
				throw new RuntimeException("invalid fpu fn " + opString(fn));
		}
	}
	
	private void execFpuFnWord (final int isn) {
		final int fn = fn(isn);
		final int fs = fs(isn);
		final int fd = fd(isn);
		
		switch (fn) {
			case FP_FN_CVT_D:
				storeDouble(fpReg, fd, fpReg[fs]);
				return;
			case FP_FN_CVT_S:
				storeSingle(fpReg, fd, fpReg[fs]);
				return;
			default:
				throw new RuntimeException("invalid fpu fn word " + opString(fn));
		}
	}
	
	private void execFpuFnX (final int isn) {
		final int fn = fn(isn);
		final int fr = fr(isn);
		final int ft = ft(isn);
		final int fs = fs(isn);
		final int fd = fd(isn);
		
		switch (fn) {
			case FP_FNX_MADDS:
				storeSingle(fpReg, fd, loadSingle(fpReg, fs) * loadSingle(fpReg, ft) + loadSingle(fpReg, fr));
				return;
			default:
				throw new RuntimeException("invalid fpu fnx " + opString(fn));
		}
	}
	
	public boolean getFpCondition (final int cc) {
		if (cc >= 0 && cc < 8) {
			return (fpControlReg[FPCR_FCCR] & (1 << cc)) != 0;
			
		} else {
			throw new IllegalArgumentException("invalid fpu cc " + cc);
		}
	}
	
	private void setFpCondition (final int cc, final boolean cond) {
		if (cc >= 0 && cc < 8) {
			// oh my god
			final int ccMask = 1 << cc;
			final int csMask = 1 << (cc == 0 ? 23 : cc + 25);
			int fccr = fpControlReg[FPCR_FCCR];
			int fcsr = fpControlReg[FPCR_FCSR];
			if (cond) {
				// set the bits
				fccr = fccr | ccMask;
				fcsr = fcsr | csMask;
			} else {
				// clear the bits
				fccr = fccr & ~ccMask;
				fcsr = fcsr & ~csMask;
			}
			fpControlReg[FPCR_FCCR] = fccr;
			fpControlReg[FPCR_FCSR] = fcsr;
			
		} else {
			throw new IllegalArgumentException("invalid fpu cc " + cc);
		}
	}
	
}
