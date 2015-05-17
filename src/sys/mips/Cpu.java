package sys.mips;

import java.util.*;
import java.util.stream.Collectors;

import static sys.mips.MipsConstants.*;
import static sys.mips.Decoder.*;
import static sys.mips.IsnUtil.*;

public class Cpu {
	
	/** general purpose registers, and hi/lo */
	private final int[] reg = new int[34];
	/** coprocessor 0 registers (32*8) */
	private final int[] cpReg = new int[256];
	/**
	 * coprocessor 1 registers (longs/doubles in consecutive registers, least
	 * significant word first!)
	 */
	private final int[] fpReg = new int[32];
	private final int[] fpControlReg = new int[32];
	private final Memory memory = new Memory();
	private final CpuLogger log = new CpuLogger(this);
	
	/** address of current instruction */
	private int pc;
	/** address of current instruction + 4 (unless changed by branch) */
	private int nextPc;
	private int cycle;
	private int rmwAddress;
	private FpRound roundingMode = FpRound.NONE;
	
	public Cpu () {
		// linux_3.2.65\arch\mips\include\asm\cpu-features.h
		// linux_3.2.65\arch\mips\include\asm\cpu.h
		// linux_3.2.65\arch\mips\include\asm\mipsregs.h
		// default values on reboot
		// 3<<28: access to coprocessor 0 and 1
		// 1<<22: bootstrap exception vectors
		// 1<<2: reset error level
		// 1<<1: exception level
		int st = (3 << 28) | (1 << 22) | (1 << 2) | (1 << 1);
		cpReg[CPR_STATUS] = st;
		// R2000A
		cpReg[CPR_PRID] = 0x0110;
		// support S, D, W, L
		int fcr = (1 << 16) | (1 << 17) | (1 << 20) | (1 << 21) | (1 << 8);
		fpControlReg[FPCR_FIR] = fcr;
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
				final int isn = memory.loadWord(pc);
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
			throw e;
		}
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
				fpReg[rt] = memory.loadWord(reg[rs] + simm);
				return;
			case OP_LDC1:
				// least significant word first...
				fpReg[rt + 1] = memory.loadWord(reg[rs] + simm);
				fpReg[rt] = memory.loadWord(reg[rs] + simm + 4);
				return;
			case OP_SWC1:
				memory.storeWord(reg[rs] + simm, fpReg[rt]);
				return;
			case OP_SDC1:
				// least significant word first...
				memory.storeWord(reg[rs] + simm, fpReg[rt + 1]);
				memory.storeWord(reg[rs] + simm + 4, fpReg[rt]);
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
				memory.storeWord(reg[rs] + simm, reg[rt]);
				return;
			case OP_SH:
				memory.storeHalfWord(reg[rs] + simm, (short) reg[rt]);
				return;
			case OP_SB:
				memory.storeByte(reg[rs] + simm, (byte) reg[rt]);
				return;
			case OP_LUI:
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
				// unaligned address
				final int a = reg[rs] + simm;
				// 0,1,2,3 -> 0,8,16,24
				final int s = (a & 3) << 3;
				final int w = memory.loadWord(a & ~3);
				reg[rt] = (w << s) | (reg[rt] & (0xffffffff >>> (32 - s)));
				return;
			}
			case OP_LWR: {
				final int a = reg[rs] + simm;
				// 0,1,2,3 -> 8,16,24,32
				final int s = ((a & 3) + 1) << 3;
				final int w = memory.loadWord(a & ~3);
				reg[rt] = (w >>> (32 - s)) | (reg[rt] & (0xffffffff << s));
				return;
			}
			case OP_SWL: {
				// unaligned addr
				final int a = reg[rs] + simm;
				// aligned address
				final int aa = a & ~3;
				final int s = (a & 3) << 3;
				final int w = memory.loadWord(aa);
				memory.storeWord(aa, (reg[rt] >>> s) | (w & (0xffffffff << (32 - s))));
				return;
			}
			case OP_SWR: {
				// unaligned addr
				final int a = reg[rs] + simm;
				// aligned addr
				final int aa = a & ~3;
				final int s = ((a & 3) + 1) << 3;
				final int w = memory.loadWord(aa);
				memory.storeWord(aa, (reg[rt] << (32 - s)) | (w & (0xffffffff >>> s)));
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
				handleException(SYSCALL_EX, syscall(isn));
				return;
			case FN_BREAK:
				handleException(BREAKPOINT_EX, syscall(isn));
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
					handleException(TRAP_EX, 0);
				}
				return;
			default:
				throw new IllegalArgumentException("invalid fn " + opString(fn));
		}
	}
	
	public void handleException(String type, int value) {
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
		final int cpr = rd * 8 + sel;
		
		switch (cpr) {
			case CPR_STATUS:
			case CPR_PRID:
				break;
			
			default:
				throw new RuntimeException("move from unknown cp reg " + rd + ", " + sel);
		}
		
		final int val = cpReg[cpr];
		reg[rt] = val;
		// System.out.println("mfc0 " + rd + "." + sel + " -> 0x" +
		// Integer.toHexString(val));
		return;
	}
	
	private final void execCpMtc0 (final int isn) {
		final int rt = rt(isn);
		final int rd = rd(isn);
		final int sel = sel(isn);
		final int cpr = rd * 8 + sel;
		final int oldVal = cpReg[cpr];
		int newVal = reg[rt];
		
		switch (cpr) {
			case CPR_CONTEXT:
				if (newVal != 0) {
					throw new RuntimeException("unknown ctx reg value " + Integer.toHexString(newVal));
				}
				break;
			case CPR_STATUS: {
				// allow only cp0/1, bev, im7-0, um, erl, exl, ie
				int mask = 0b0011_0000_0100_0000_1111_1111_0001_0111;
				newVal = newVal & mask;
				int cp = (newVal >> 28) & 0x3;
				int bev = (newVal >> 22) & 0x1;
				int im = (newVal >> 8) & 0xff;
				int um = (newVal >> 4) & 0x1;
				int erl = (newVal >> 2) & 0x1;
				int exl = (newVal >> 1) & 0x1;
				int ie = (newVal >> 0) & 0x1;
				log.debug("set status cp=%x bev=%x im=%x um=%x erl=%x exl=%x ie=%x", cp, bev, im, um, erl, exl, ie);
				if (ie == 1) {
					throw new RuntimeException("interrupts enabled");
				}
				break;
			}
			default:
				throw new RuntimeException("move to unknown cp reg " + rd + ", " + sel);
		}
		
		if (oldVal != newVal) {
			log.info("mtc0 " + rd + "." + sel + " was 0x" + Integer.toHexString(oldVal) + " now 0x" + Integer.toHexString(newVal));
			cpReg[cpr] = newVal;
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
