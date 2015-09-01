package sys.mips;

import java.util.*;

import sys.malta.Malta;
import sys.malta.MaltaUtil;

import static sys.mips.Constants.*;
import static sys.mips.Decoder.*;
import static sys.mips.IsnUtil.*;

public final class Cpu {
	
	private static final int INTERVAL_NS = 4000000;
	private static final ThreadLocal<Cpu> instance = new ThreadLocal<>();
	
	/** allow other classes to access cpu */
	public static Cpu getInstance() {
		return instance.get();
	}
	
	/** general purpose registers, and hi/lo */
	private final int[] reg = new int[34];
	/** coprocessor 0 registers (register+selection*32) */
	private final int[] cpReg = new int[64];
	/** coprocessor 1 registers (longs/doubles in consecutive registers) */
	private final int[] fpReg = new int[32];
	private final int[] fpControlReg = new int[32];
	private final CpuLogger log = new CpuLogger(this);
	private final Map<String, int[]> isnCount = new HashMap<>();
	private final Memory memory;
	private final boolean littleEndian;
	private final List<Exn> interrupts = new ArrayList<>();
	private final CallLogger calls = new CallLogger(this);
	
	private volatile long cycle;
	
	/** address of current instruction */
	private int pc;
	/** address of current instruction + 4 (unless changed by branch) */
	private int nextPc;
	private int loadLinkedVAddr;
	private int loadLinkedPAddr;
	private boolean loadLinkedBit;
	private FpRound roundingMode = FpRound.NONE;
	private boolean kernelMode;
	private int hi;
	private int lo;
	private boolean countIsns; 
	private boolean disasm;
	private boolean interruptsEnabled;
	private boolean execException;
	
	public Cpu (boolean littleEndian) {
		this.littleEndian = littleEndian;
		memory = new Memory(0x2000000, littleEndian);
		memory.setMalta(new Malta());
		memory.setKernelMode(true);
		memory.init();
		for (String name : IsnSet.INSTANCE.nameMap.keySet()) {
			isnCount.put(name, new int[1]);
		}
		
		// default values on reboot
		cpReg[CPR_STATUS] = CPR_STATUS_EXL | CPR_STATUS_ERL | CPR_STATUS_BEV | CPR_STATUS_CU0 | CPR_STATUS_CU1;
		statusUpdated();
		
		// 0x10000 = MIPS, 0x8000 = 4KC 
		cpReg[CPR_PRID] = 0x18000;
		
		// 15: big endian, 7: TLB
		cpReg[CPR_CONFIG] = (1 << 31) | ((littleEndian?0:1) << 15) | (1 << 7) | (1 << 1);
		
		// 25: tlb entries - 1
		// should enable 3: watch registers and 1: ejtag, but we don't want them
		cpReg[CPR_CONFIG1] = (15 << 25);
		
		cpReg[CPR_COMPARE] = -1;
		
		// support S, D, W, L (unlike the 4kc...)
		fpControlReg[FPCR_FIR] = (1 << 16) | (1 << 17) | (1 << 20) | (1 << 21) | (1 << 8);
		
		setPc(EXV_RESET);
	}
	
	public final int[] getCpRegisters () {
		return cpReg;
	}
	
	public final long getCycle () {
		return cycle;
	}
	
	public final Memory getMemory () {
		return memory;
	}
	
	public final int getPc () {
		return pc;
	}
	
	public final void setPc (int pc) {
		this.pc = pc;
		this.nextPc = pc + 4;
	}
	
	public final int getNextPc () {
		return nextPc;
	}
	
	public final int[] getRegisters () {
		return reg;
	}
	
	public final int getRegister (int n) {
		return reg[n];
	}
	
	public final void setRegister (int n, int value) {
		reg[n] = value;
	}
	
	public final int getFpRegister (int n) {
		return fpReg[n];
	}
	
	public final double getFpRegister (int n, FpFormat fmt) {
		return fmt.load(fpReg, n);
	}
	
	public final CpuLogger getLog () {
		return log;
	}
	
	public final long getHilo () {
		return (hi & 0xffff_ffffL) << 32 | (lo & 0xffff_ffffL);
	}
	
	public final int getHi () {
		return hi;
	}
	
	public final int getLo () {
		return lo;
	}
	
	public final boolean isCountIsns () {
		return countIsns;
	}
	
	public final void setCountIsns (boolean countIsns) {
		this.countIsns = countIsns;
	}
	
	public final Map<String, int[]> getIsnCount () {
		return isnCount;
	}
	
	public final int[] getFpControlReg () {
		return fpControlReg;
	}
	
	public final boolean isLittleEndian () {
		return littleEndian;
	}
	
	public final CallLogger getCalls () {
		return calls;
	}
	
	public final boolean isInterruptsEnabled () {
		return interruptsEnabled;
	}
	
	public final boolean isKernelMode () {
		return kernelMode;
	}
	
	public final boolean isExecException () {
		return execException;
	}
	
	/** never returns, throws runtime exception... */
	public final void run () {
		final long startTime = System.nanoTime();
		
		try {
			instance.set(this);
			calls.call(pc);
			log.info("run " + cycle);
			long t = startTime;
			int da = 0;
			
			while (true) {
				for (int n = 0; n < 10000; n++) {
					if (reg[0] != 0) {
						reg[0] = 0;
					}
					
					final int isn = memory.loadWord(pc);
					
					if (disasm) {
						// log.debug(cpRegString(this));
						// log.debug(gpRegString(this));
						log.debug(IsnUtil.isnString(this, isn));
					}
					
					if (pc == 0x803a94a0 && reg[REG_A0] > da) {
						log.info("delay " + Integer.toHexString(reg[REG_A0]));
						da = reg[REG_A0];
					}
					
					pc = nextPc;
					nextPc += 4;
					
					execOp(isn);
					
					if (countIsns) {
						isnCount.get(IsnSet.INSTANCE.getIsn(isn).name)[0]++;
					}
					
					final int cmp = cpReg[CPR_COMPARE];
					final int count = (int) (cycle >> 1);
					if (cmp == count) {
						if (interruptsEnabled) {
							throw new RuntimeException("compare hit");
						}
					}
					
					cycle++;
				}
				
				
				if (cycle > 1000000000) {
					throw new RuntimeException("timeout");
				}
				
				// XXX this is total hack, should really be externally driven
				long ct = System.nanoTime();
				if (ct >= t + INTERVAL_NS) {
					t += INTERVAL_NS;
					if (interruptsEnabled) {
						log.info("fire programmable interrupt timer");
						execException(EX_INTERRUPT, MaltaUtil.INT_SB_INTR, MaltaUtil.IRQ_TIMER);
					}
				} else {
					//checkExn();
				}
			}
			
		} catch (Exception e) {
			throw new RuntimeException("cycle " + cycle + " kernel mode " + kernelMode + " interrupts enabled " + interruptsEnabled + " stack " + calls.callString(), e);
			
		} finally {
			final long d = System.nanoTime() - startTime;
			log.info("ns per isn: " + (d / cycle));
			instance.remove();
		}
	}

	public final void interrupt (final Exn e) {
		synchronized (interrupts) {
			System.out.println("add exn " + e);
			interrupts.add(e);
		}
	}
	
//	private void checkExn () {
//		Exn e = null;
//		synchronized (interrupts) {
//			if (interrupts.size() > 0) {
//				e = interrupts.remove(0);
//				System.out.println("got interrupt " + e);
//			}
//		}
//		if (e != null) {
//			if (e.type < 0) {
//				throw new RuntimeException("stop");
//			} else if (interruptsEnabled) {
//				execException(e.type, e.in);
//			} else {
//				log.info("ignore exn " + e);
//			}
//		}
//	}
	
	private final void execOp (final int isn) {
		final int op = op(isn);
		final int rs = rs(isn);
		final int rt = rt(isn);
		final int simm = simm(isn);
		
		switch (op) {
			case OP_SPECIAL:
				execFunction(isn);
				return;
			case OP_REGIMM:
				execRegImm(isn);
				return;
			case OP_COP0:
				if (rs < 16) {
					execCpRs(isn);
				} else {
					execCpFunction(isn);
				}
				return;
			case OP_COP1:
				execFpuRs(isn);
				return;
			case OP_COP1X:
				execFpuFnX(isn);
				return;
			case OP_SPECIAL2:
				execFunction2(isn);
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
				execJump(isn);
				return;
			case OP_JAL:
				execLink();
				execJump(isn);
				calls.call(nextPc);
				return;
			case OP_BLEZ:
				if (reg[rs] <= 0) {
					execBranch(isn);
				}
				return;
			case OP_BEQ:
				if (reg[rs] == reg[rt]) {
					execBranch(isn);
				}
				return;
			case OP_BNE:
				if (reg[rs] != reg[rt]) {
					execBranch(isn);
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
					execBranch(isn);
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
				final int va = reg[rs] + simm;
				final int pa =  memory.translate(va);
				loadLinkedVAddr = va;
				loadLinkedPAddr = pa;
				loadLinkedBit = true;
				reg[rt] = memory.loadWord(va);
				return;
			}
			case OP_SC: {
				final int va = reg[rs] + simm;
				final int pa = memory.translate(va);
				if (va == loadLinkedVAddr && pa == loadLinkedPAddr && loadLinkedBit) {
					memory.storeWord(va, reg[rt]);
					reg[rt] = 1;
				} else {
					log.info("sc fail: va=" + Integer.toHexString(va) + " pa=" + Integer.toHexString(pa) + " ll=" + loadLinkedBit);
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
				reg[rt] = memory.loadHalfWord(reg[rs] + simm) & 0xffff;
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
			case OP_PREF:
				return;
			default:
				throw new RuntimeException("invalid op " + opString(op));
		}
	}
	
	/** update nextpc with jump */
	private final void execJump (final int isn) {
		nextPc = jump(isn, pc);
	}

	/** update nextpc with branch */
	private final void execBranch (final int isn) {
		nextPc = branch(isn, pc);
	}

	/** skip branch delay */
	private final void execBranchSkip () {
		nextPc += 4;
	}

	private final void execLink () {
		reg[31] = nextPc;
	}
	
	private final void execRegImm (final int isn) {
		final int rs = rs(isn);
		final int rt = rt(isn);
		
		switch (rt) {
			case RT_BGEZAL:
				execLink();
				//$FALL-THROUGH$
			case RT_BGEZ:
				if (reg[rs] >= 0) {
					execBranch(isn);
				}
				return;
			case RT_BLTZAL:
				execLink();
				//$FALL-THROUGH$
			case RT_BLTZ:
				if (reg[rs] < 0) {
					execBranch(isn);
				}
				return;
			default:
				throw new RuntimeException("invalid regimm " + opString(rt));
		}
	}

	private final void execFunction (final int isn) {
		final int rd = rd(isn);
		final int rt = rt(isn);
		final int rs = rs(isn);
		final int fn = fn(isn);
		
		switch (fn) {
			case FN_SLL:
				if (rd != 0) {
					reg[rd] = reg[rt] << sa(isn);
				}
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
					calls.ret();
				}
				return;
			case FN_JALR:
				reg[rd] = nextPc;
				nextPc = reg[rs];
				calls.call(nextPc);
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
				execException(EX_SYSCALL, 0, 0);
				return;
			case FN_BREAK:
				execException(EX_BREAKPOINT, 0, 0);
				return;
			case FN_SYNC:
				log.debug("sync");
				return;
			case FN_MFHI:
				reg[rd] = hi;
				return;
			case FN_MTHI:
				hi = reg[rs];
				return;
			case FN_MFLO:
				reg[rd] = lo;
				return;
			case FN_MTLO:
				lo = reg[rd];
				return;
			case FN_MULT: {
				// sign extend
				final long rsValue = reg[rs];
				final long rtValue = reg[rt];
				final long result = rsValue * rtValue;
				lo = (int) result;
				hi = (int) (result >>> 32);
				return;
			}
			case FN_MULTU: {
				// zero extend
				final long rsValue = reg[rs] & 0xffff_ffffL;
				final long rtValue = reg[rt] & 0xffff_ffffL;
				final long result = rsValue * rtValue;
				lo = (int) result;
				hi = (int) (result >>> 32);
				return;
			}
			case FN_DIV: {
				// divide as signed
				// result is unpredictable for zero, no exceptions thrown
				int rsValue = reg[rs];
				int rtValue = reg[rt];
				if (rt != 0) {
					lo = rsValue / rtValue;
					hi = rsValue % rtValue;
				}
				return;
			}
			case FN_DIVU: {
				// unpredictable result and no exception for zero
				// zero extend
				final long rsValue = reg[rs] & 0xffff_ffffL;
				final long rtValue = reg[rt] & 0xffff_ffffL;
				if (rtValue != 0) {
					lo = (int) (rsValue / rtValue);
					hi = (int) (rsValue % rtValue);
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
				long rsValue = reg[rs] & 0xffff_ffffL;
				long rtValue = reg[rt] & 0xffff_ffffL;
				reg[rd] = rsValue < rtValue ? 1 : 0;
				return;
			}
			case FN_TNE:
				if (reg[rs] != reg[rt]) {
					execException(EX_TRAP, 0, 0);
				}
				return;
			default:
				throw new IllegalArgumentException("invalid fn " + opString(fn));
		}
	}
	
	/**
	 * execute exception.
	 * if excode is interrupt, interrupt code must be provided.
	 * if interrupt code is south bridge interrupt, irq must be provided.
	 */
	// traps.c trap_init
	// genex.S
	// malta-int.c plat_irq_dispatch (deals with hardware interrupts)
	private final void execException (final int excode, final int interrupt, final int irq) {
		log.info("exec exception " + excode + " " + exceptionName(excode) + " interrupt " + interrupt + " " + MaltaUtil.interruptName(interrupt) + " irq " + MaltaUtil.irqName(irq));
		final boolean isInterrupt = excode == EX_INTERRUPT;
		final boolean isSbIntr = isInterrupt && interrupt == MaltaUtil.INT_SB_INTR;
		
		if (execException) {
			throw new RuntimeException("exception in exception handler");
		}
		
		if (excode < 0 || excode >= 32) {
			throw new RuntimeException("invalid excode " + excode);
		}

		if (isInterrupt && (interrupt < 0 || interrupt >= 8)) {
			throw new RuntimeException("invalid interrupt " + interrupt);
		}
		
		if (isSbIntr && (irq < 0 || irq >= 16)) {
			throw new RuntimeException("invalid irq " + irq);
		}
		
		final int status = cpReg[CPR_STATUS];
		final int cause = cpReg[CPR_CAUSE];
		final boolean bev = (status & CPR_STATUS_BEV) != 0;
		final boolean exl = (status & CPR_STATUS_EXL) != 0;
		final boolean iv = (cause & CPR_CAUSE_IV) != 0;
		
		if (bev) {
			// we don't have a boot rom...
			throw new RuntimeException("bootstrap exception");
		}
		
		if (exl) {
			// eh...
			throw new RuntimeException("exception in exception...");
		}

		// this is same for status and cause register
		final int interruptmask = isInterrupt ? 1 << (CPR_STATUS_IM_SHL + interrupt) : 0;
		
		if (isInterrupt && (status & interruptmask) == 0) {
			throw new RuntimeException("masked interrupt " + interrupt);
		}
		
		// pc is next instruction, nextpc is one after (maybe jumped)
		final int bdmask = nextPc == pc + 4 ? 0 : CPR_CAUSE_BD;
		final int excodemask = excode << CPR_CAUSE_EXCODE_SHL;
		
		cpReg[CPR_STATUS] = status | CPR_STATUS_EXL;
		cpReg[CPR_CAUSE] = (cause & ~(CPR_CAUSE_EXCODE | CPR_CAUSE_IP | CPR_CAUSE_BD)) | excodemask | interruptmask | bdmask;
		cpReg[CPR_EPC] = nextPc == pc + 4 ? pc : pc - 4;
		
		log.info("epc=" + memory.getSymbols().getName(cpReg[CPR_EPC], true) + " bd=" + (bdmask != 0));
		
		statusUpdated();
		
		if (isSbIntr) {
			// uh....
			memory.getMalta().getGt().setIrq(irq);
		}
		
		if (isInterrupt && iv) {
			log.info("jump to interrupt vector");
			setPc(EXV_INTERRUPT);
			
		} else {
			log.info("jump to exception vector");
			setPc(EXV_EXCEPTION);
		}
		
		execException = true;
		calls.push("ex" + excode + "int" + interrupt + "irq" + irq);
		calls.call(pc);
	}
	
	private final void execFunction2 (final int isn) {
		final int rs = rs(isn);
		final int rt = rt(isn);
		final int rd = rd(isn);
		final int fn = fn(isn);
		
		switch (fn) {
			case FN2_MADD: {
				long rsValue = reg[rs];
				long rtValue = reg[rt];
				long result = rsValue * rtValue + getHilo();
				lo = (int) result;
				hi = (int) (result >>> 32);
				return;
			}
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
	private final void execCpRs (final int isn) {
		final int rs = rs(isn);
		// should check if cp0 enabled/kernel mode?
		
		switch (rs) {
			case CP_RS_MFC0:
				execCpMoveFrom(isn);
				return;
			case CP_RS_MTC0:
				execCpMoveTo(isn);
				return;
			default:
				throw new RuntimeException("invalid coprocessor rs " + opString(rs));
		}
	}
	
	private final void execCpMoveFrom (final int isn) {
		final int rt = rt(isn);
		final int rd = rd(isn);
		final int sel = sel(isn);
		final int cpr = cpr(rd, sel);
		
		switch (cpr) {
			case CPR_STATUS:
			case CPR_PRID:
			case CPR_CONFIG:
			case CPR_CONFIG1:
			case CPR_CAUSE:
			case CPR_ENTRYHI:
			case CPR_WIRED:
			case CPR_EPC:
				break;
			case CPR_COUNT:
				// update this only when read
				cpReg[cpr] = (int) (cycle >>> 1);
				break;
			default:
				throw new RuntimeException("move from unknown cp reg " + cpRegName(rd, sel));
		}
		
		reg[rt] = cpReg[cpr];
		return;
	}
	
	/** move to system coprocessor register */
	private final void execCpMoveTo (final int isn) {
		final int rd = rd(isn);
		final int sel = sel(isn);
		final int cpr = cpr(rd, sel);
		final int oldValue = cpReg[cpr];
		final int newValue = reg[rt(isn)];
		if (oldValue != newValue) {
			log.debug("mtc0 " + cpRegName(rd, sel) + " 0x" + Integer.toHexString(oldValue) + " <- 0x" + Integer.toHexString(newValue));
		}
		
		switch (cpr) {
			case CPR_INDEX:
				cpReg[cpr] = newValue & 0xf;
				return;
			case CPR_ENTRYLO0:
			case CPR_ENTRYLO1:
				cpReg[cpr] = newValue & 0x7fff_ffff;
				return;
			case CPR_ENTRYHI:
				cpReg[cpr] = newValue & 0xffff_f0ff;
				return;
			case CPR_PAGEMASK:
				cpReg[cpr] = newValue & 0x00ff_f000;
				return;
			case CPR_CONTEXT:
			case CPR_WIRED:
				if (oldValue != newValue) {
					throw new RuntimeException("unknown value " + Integer.toHexString(newValue));
				}
				return;
			case CPR_STATUS:
				setCpStatus(newValue);
				return;
			case CPR_CAUSE:
				setCpCause(newValue);
				return;
			case CPR_CONFIG:
				if (oldValue != newValue) {
					throw new RuntimeException("unknown config value " + Integer.toHexString(newValue));
				}
				return;
			case CPR_COMPARE:
			case CPR_EPC:
				cpReg[cpr] = newValue;
				return;
			default:
				throw new RuntimeException("move to unknown cp reg " + cpRegName(rd, sel));
		}
	}
	
	private final void setCpCause (final int newValue) {
		final int value = cpReg[CPR_CAUSE];
		int mask = CPR_CAUSE_IV;
		if ((newValue & ~mask) != 0) {
			throw new RuntimeException("unknown cause value " + Integer.toHexString(newValue));
		}
		cpReg[CPR_CAUSE] = (value & ~mask) | (newValue & mask);
	}
	
	private final void setCpStatus (final int newValue) {
		final int mask = CPR_STATUS_CU1 | CPR_STATUS_CU0 | CPR_STATUS_BEV | CPR_STATUS_IM | CPR_STATUS_UM | CPR_STATUS_ERL | CPR_STATUS_EXL | CPR_STATUS_IE;
		if ((newValue & ~mask) != 0) {
			throw new RuntimeException("unknown status value " + Integer.toHexString(newValue));
		}
		
		// don't need to preserve anything
		cpReg[CPR_STATUS] = newValue & mask;

		statusUpdated();
	}
	
	private final void statusUpdated () {
		final int status = cpReg[CPR_STATUS];
		final boolean ie = (status & CPR_STATUS_IE) != 0;
		final boolean exl = (status & CPR_STATUS_EXL) != 0;
		final boolean erl = (status & CPR_STATUS_ERL) != 0;
		final boolean um = (status & CPR_STATUS_UM) != 0;
		//log.debug("ie=" + ie + " exl=" + exl + " erl=" + erl + " um=" + um);
		
		// kernel mode if UM = 0, or EXL = 1, or ERL = 1
		kernelMode = !um || exl || erl;
		if (!kernelMode) {
			throw new RuntimeException("user mode");
		}
		
		// interrupts enabled if IE = 1 and EXL = 0 and ERL = 0
		// asmmacro.h local_irq_enable
		boolean interruptsEnabled = ie && !exl && !erl;
		if (this.interruptsEnabled != interruptsEnabled) {
			log.info("interrupts " + (interruptsEnabled ? "enabled" : "disabled") + " in " + calls.callString());
		}
		this.interruptsEnabled = interruptsEnabled;
	}
	
	private final void execCpFunction (final int isn) {
		final int fn = fn(isn);
		
		switch (fn) {
			case CP_FN_TLBWI: {
				// write indexed tlb entry...
				int i = cpReg[CPR_INDEX];
				int lo0 = cpReg[CPR_ENTRYLO0];
				int lo1 = cpReg[CPR_ENTRYLO1];
				int pm = cpReg[CPR_PAGEMASK];
				int hi = cpReg[CPR_ENTRYHI];
				Entry e = memory.getEntry(i);
				e.pageMask = pm >>> 12;
				e.virtualPageNumber = hi >>> 12;
				e.addressSpaceId = (hi >>> 12) & 0xff;
				e.global = (lo0 & lo1 & 1) != 0;
				e.data0.physicalFrameNumber = (lo0 >>> 6) & 0x7ffff;
				e.data0.dirty = (lo0 & 4) != 0;
				e.data0.valid = (lo0 & 2) != 0;
				e.data1.physicalFrameNumber = (lo1 >>> 6) & 0x7ffff;
				e.data1.dirty = (lo1 & 4) != 0;
				e.data1.valid = (lo1 & 2) != 0;
				log.info("updated tlb[" + i + "]=" + e);
				return;
			}
			case CP_FN_ERET: {
				final int epc = cpReg[CPR_EPC];
				final int status = cpReg[CPR_STATUS];
				log.info("exception return " + memory.getSymbols().getName(epc, true));
				if ((status & CPR_STATUS_ERL) != 0) {
					throw new RuntimeException("eret with erl");
				}
				// no delay slot
				setPc(epc);
				cpReg[CPR_STATUS] = status & ~CPR_STATUS_EXL;
				loadLinkedBit = false;
				execException = false;
				statusUpdated();
				calls.pop();
				return;
			}
			default:
				throw new RuntimeException("invalid coprocessor fn " + opString(fn));
		}
	}
	
	private final void execFpuRs (final int isn) {
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
				if (fptf(isn) == fccrFcc(fpControlReg, fpcc(isn))) {
					execBranch(isn);
				} else {
					execBranchSkip();
				}
				return;
				
			case FP_RS_CFC1:
				execFpuCopyFrom(isn);
				return;
				
			case FP_RS_CTC1:
				execFpuCopyTo(isn);
				return;
				
			default:
				throw new RuntimeException("invalid fpu rs " + opString(rs));
		}
		
	}
	
	private final void execFpuCopyFrom (final int isn) {
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
	
	private final void execFpuCopyTo (final int isn) {
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
	
	private final void execFpuFn (final int isn, final FpFormat fmt) {
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
	
	private final void execFpuFnWord (final int isn) {
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
	
	private final void execFpuFnX (final int isn) {
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
	
	private final void setFpCondition (final int cc, final boolean cond) {
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
