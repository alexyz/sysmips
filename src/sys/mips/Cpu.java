package sys.mips;

import java.util.*;

import sys.malta.Malta;
import sys.malta.MaltaUtil;
import sys.util.Symbols;

import static sys.mips.Constants.*;
import static sys.mips.Decoder.*;
import static sys.mips.IsnUtil.*;

public final class Cpu {
	
	private static final ThreadLocal<Cpu> instance = new ThreadLocal<>();
	private static final int INTERVAL_NS = 4000000;
	
	/** allow other classes to access cpu */
	public static Cpu getInstance() {
		return instance.get();
	}
	
	/** general purpose registers */
	private final int[] register = new int[32];
	/** coprocessor 0 registers (register+selection*32) */
	private final int[] cpReg = new int[64];
	/** coprocessor 1 registers (longs/doubles in consecutive registers) */
	private final int[] fpReg = new int[32];
	private final int[] fpControlReg = new int[32];
	private final CpuLogger log = new CpuLogger(this);
	private final Map<String, int[]> isnCount = new HashMap<>();
	private final Memory memory;
	private final boolean littleEndian;
//	private final List<CpuException> interrupts = new ArrayList<>();
	private final CallLogger calls = new CallLogger(this);
	/** 0 for le, 3 for be */
	private final int wordAddrXor;
	
	private volatile long cycle;
	
	/** address of current instruction */
	private int pc;
	/** address of current instruction + 4 (unless changed by branch) */
	private int nextPc;
	private int loadLinkedVaddr;
	private int loadLinkedPaddr;
	private boolean loadLinkedBit;
	private FpRound roundingMode = FpRound.NONE;
	private boolean kernelMode;
	private int hi;
	private int lo;
	private boolean countIsns; 
	private boolean disasm;
	private boolean interruptsEnabled;
	private boolean execException;
	private boolean singleStep;
	
	public Cpu (int memsize, boolean littleEndian) {
		this.memory = new Memory(memsize, littleEndian);
		this.littleEndian = littleEndian;
		this.wordAddrXor = littleEndian ? 0 : 3;
		
		memory.setMalta(new Malta());
		memory.setKernelMode(true);
		memory.init();
		for (String name : IsnSet.getInstance().getNameMap().keySet()) {
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
		return register;
	}
	
	public final int getRegister (int n) {
		return register[n];
	}
	
	public final void setRegister (int n, int value) {
		register[n] = value;
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
		return zeroExtendInt(hi) << 32 | zeroExtendInt(lo);
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
	
	public boolean isSingleStep () {
		return singleStep;
	}

	public void setSingleStep (boolean singleStep) {
		this.singleStep = singleStep;
	}
	
	public void setDisasm (boolean disasm) {
		this.disasm = disasm;
	}

	/** never returns, throws runtime exception... */
	public final void run () {
		final long startTime = System.nanoTime();
		
		try {
			instance.set(this);
			calls.call(pc);
			log.info("run " + cycle);
			long t = startTime;
//			int da = 0;
//			String[] regstr = new String[32];
			// this should only be checked during call
			int f = 0;
//			f = memory.getSymbols().getAddr("init_vdso");
//			if (f == 0) {
//				throw new RuntimeException();
//			}
			
			while (true) {
				for (int n = 0; n < 10000; n++) {
					if (register[0] != 0) {
						register[0] = 0;
					}
					
					final int isn = memory.loadWord(pc);
					
					if (pc == f) {
						//log.info("single step...");
						//disasm = true;
						calls.setPrintCalls(true);
						//singleStep = true;
					}
					
					if (disasm) {
						// log.debug(cpRegString(this));
//						final String x = CpuUtil.gpRegString(this, regstr);
//						if (x.length() > 0) {
//							log.info(x);
//						}
						log.info(IsnUtil.isnString(this, isn));
					}
					
//					if (pc == 0x803a94a0 && register[REG_A0] > da) {
//						log.info("delay " + Integer.toHexString(register[REG_A0]));
//						da = register[REG_A0];
//					}
					
					pc = nextPc;
					nextPc += 4;
					final boolean printCall = calls.isPrintAfterNext();
					
					try {
						execOp(isn);
						
					} catch (CpuException e) {
						log.info("caught " + e);
						execException(e.extype, 0, 0, e.vaddr);
					}
					
					if (printCall) {
						calls.printCall();
					}
					
					if (countIsns) {
						isnCount.get(IsnSet.getInstance().getIsn(isn).name)[0]++;
					}
					
					final int cmp = cpReg[CPR_COMPARE];
					final int count = (int) (cycle >> 1);
					if (cmp == count) {
						if (interruptsEnabled) {
							throw new RuntimeException("compare hit");
						}
					}
					
					if (singleStep) {
						while (System.in.read() != 10) {
							//
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
						execException(EX_INTERRUPT, MaltaUtil.INT_SB_INTR, MaltaUtil.IRQ_TIMER, 0);
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

//	public final void interrupt (final CpuException e) {
//		synchronized (interrupts) {
//			System.out.println("add exn " + e);
//			interrupts.add(e);
//		}
//	}
	
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
		final short simm = simm(isn);
		
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
				fpReg[rt] = memory.loadWord(register[rs] + simm);
				return;
			case OP_LDC1:
				// least significant word first...
				fpReg[rt + 1] = memory.loadWord(register[rs] + simm);
				fpReg[rt] = memory.loadWord(register[rs] + simm + 4);
				return;
			case OP_SWC1:
				memory.storeWord(register[rs] + simm, fpReg[rt]);
				return;
			case OP_SDC1:
				// least significant word first...
				memory.storeWord(register[rs] + simm, fpReg[rt + 1]);
				memory.storeWord(register[rs] + simm + 4, fpReg[rt]);
				return;
			case OP_J:
				execJump(isn);
				{
					// [85960839:ki] 0x80269124<proc_mkdir+0x4>               0809a42d j       0x802690b4<proc_mkdir_mode>
					Symbols sym = memory.getSymbols();
					String pcname = sym.getName(pc);
					String nextpcname = sym.getName(nextPc);
					if (!pcname.equals(nextpcname)) {
						//log.debug("implicit call " + pcname + " -> " + nextpcname);
						calls.callNoRet(nextPc);
					}
				}
				return;
			case OP_JAL:
				execLink();
				execJump(isn);
				calls.call(nextPc);
				return;
			case OP_BLEZ:
				if (register[rs] <= 0) {
					execBranch(isn);
				}
				return;
			case OP_BEQ:
				if (register[rs] == register[rt]) {
					execBranch(isn);
				}
				return;
			case OP_BNE:
				if (register[rs] != register[rt]) {
					execBranch(isn);
				}
				return;
			case OP_ADDIU:
				register[rt] = register[rs] + simm;
				return;
			case OP_ANDI:
				register[rt] = register[rs] & zeroExtendShort(simm);
				return;
			case OP_XORI:
				register[rt] = register[rs] ^ zeroExtendShort(simm);
				return;
			case OP_BGTZ:
				if (register[rs] > 0) {
					execBranch(isn);
				}
				return;
			case OP_SLTI:
				register[rt] = register[rs] < simm ? 1 : 0;
				return;
			case OP_SLTIU: {
				// zero extend
				long rsValue = zeroExtendInt(register[rs]);
				// sign extend then zero extend imm so it represents ends of
				// unsigned range
				long immValue = zeroExtendInt(simm);
				register[rt] = (rsValue < immValue) ? 1 : 0;
				return;
			}
			case OP_ORI:
				register[rt] = register[rs] | zeroExtendShort(simm);
				return;
			case OP_SW:
				memory.storeWord(register[rs] + simm, register[rt]);
				return;
			case OP_SH:
				memory.storeHalfWord(register[rs] + simm, (short) register[rt]);
				return;
			case OP_SB:
				memory.storeByte(register[rs] + simm, (byte) register[rt]);
				return;
			case OP_LUI:
				register[rt] = simm << 16;
				return;
			case OP_LL: {
				final int va = register[rs] + simm;
				final int pa =  memory.translate(va, false);
				loadLinkedVaddr = va;
				loadLinkedPaddr = pa;
				loadLinkedBit = true;
				register[rt] = memory.loadWord(va);
				return;
			}
			case OP_SC: {
				final int va = register[rs] + simm;
				final int pa = memory.translate(va, true);
				if (va == loadLinkedVaddr && pa == loadLinkedPaddr && loadLinkedBit) {
					memory.storeWord(va, register[rt]);
					register[rt] = 1;
				} else {
					log.info("sc fail: va=" + Integer.toHexString(va) + " pa=" + Integer.toHexString(pa) + " ll=" + loadLinkedBit);
					register[rt] = 0;
				}
				return;
			}
			case OP_LW:
				register[rt] = memory.loadWord(register[rs] + simm);
				return;
			case OP_LB:
				register[rt] = memory.loadByte(register[rs] + simm);
				return;
			case OP_LBU:
				register[rt] = memory.loadByte(register[rs] + simm) & 0xff;
				return;
			case OP_LHU:
				register[rt] = zeroExtendShort(memory.loadHalfWord(register[rs] + simm));
				return;
			case OP_LH:
				register[rt] = memory.loadHalfWord(register[rs] + simm);
				return;
			case OP_LWL: {
				// lealign 0: mem << 24 | regmask >> 8
				// lealign 1: mem << 16 | regmask >> 16
				// lealign 2: mem << 8 | regmask >> 24
				// lealign 3: mem << 0 | regmask >> 32
				final int a = register[rs] + simm;
				final int lealign = (a & 3) ^ wordAddrXor;
				final int mem = memory.loadWord(a & ~3);
				final int rsh = (lealign + 1) * 8;
				final int lsh = 32 - rsh;
				register[rt] = (mem << lsh) | (register[rt] & (int) (ZX_INT_MASK >>> rsh));
				return;
			}
			case OP_LWR: {
				// lealign 0: regmask << 32 | mem >> 0
				// lealign 1: regmask << 24 | mem >> 8
				// lealign 2: regmask << 16 | mem >> 16
				// lealign 3: regmask << 8 | mem >> 24
				final int a = register[rs] + simm;
				final int lealign = (a & 3) ^ wordAddrXor;
				final int mem = memory.loadWord(a & ~3);
				final int rsh = lealign * 8;
				final int lsh = 32 - rsh;
				register[rt] = (register[rt] & (int) (ZX_INT_MASK << lsh)) | (mem >>> rsh);
				return;
			}
			case OP_SWL: {
				// lealign 0: memmask << 8 | reg >> 24
				// lealign 1: memmask << 16 | reg >> 16
				// lealign 2: memmask << 24 | reg >> 8
				// lealign 3: memmask << 32 | reg >> 0
				final int a = register[rs] + simm;
				final int lealign = (a & 3) ^ wordAddrXor;
				final int word = memory.loadWord(a & ~3);
				final int lsh = (lealign + 1) * 8;
				final int rsh = 32 - lsh;
				memory.storeWord(a & ~3, (word & (int) (ZX_INT_MASK << lsh)) | (register[rt] >>> rsh));
				return;
			}
			case OP_SWR: {
				// lealign 0: reg << 0 | memmask >> 32
				// lealign 1: reg << 8 | memmask >> 24
				// lealign 2: reg << 16 | memmask >> 16
				// lealign 3: reg << 24 | memmask >> 8
				final int a = register[rs] + simm;
				final int lealign = (a & 3) ^ wordAddrXor;
				final int word = memory.loadWord(a & ~3);
				final int lsh = lealign * 8;
				final int rsh = 32 - lsh;
				memory.storeWord(a & ~3, (register[rt] << lsh) | (word & (int) (ZX_INT_MASK >>> rsh)));
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
		register[31] = nextPc;
	}
	
	private final void execRegImm (final int isn) {
		final int rs = rs(isn);
		final int rt = rt(isn);
		
		switch (rt) {
			case RT_BGEZAL:
				execLink();
				//$FALL-THROUGH$
			case RT_BGEZ:
				if (register[rs] >= 0) {
					execBranch(isn);
				}
				return;
			case RT_BLTZAL:
				execLink();
				//$FALL-THROUGH$
			case RT_BLTZ:
				if (register[rs] < 0) {
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
					register[rd] = register[rt] << sa(isn);
				}
				return;
			case FN_SRL:
				register[rd] = register[rt] >>> sa(isn);
				return;
			case FN_SRA:
				register[rd] = register[rt] >> sa(isn);
				return;
			case FN_SRLV:
				register[rd] = register[rt] >>> (register[rs] & 0x1f);
				return;
			case FN_SRAV:
				register[rd] = register[rt] >> (register[rs] & 0x1f);
				return;
			case FN_SLLV:
				register[rd] = register[rt] << (register[rs] & 0x1f);
				return;
			case FN_JR:
				nextPc = register[rs];
				if (rs == 31) {
					calls.ret();
				}
				return;
			case FN_JALR:
				register[rd] = nextPc;
				nextPc = register[rs];
				calls.call(nextPc);
				return;
			case FN_MOVZ:
				if (register[rt] == 0) {
					register[rd] = register[rs];
				}
				return;
			case FN_MOVN:
				if (register[rt] != 0) {
					register[rd] = register[rs];
				}
				return;
			case FN_SYSCALL:
				execException(EX_SYSCALL, 0, 0, 0);
				return;
			case FN_BREAK:
				execException(EX_BREAKPOINT, 0, 0, 0);
				return;
			case FN_SYNC:
				log.debug("sync");
				return;
			case FN_MFHI:
				register[rd] = hi;
				return;
			case FN_MTHI:
				hi = register[rs];
				return;
			case FN_MFLO:
				register[rd] = lo;
				return;
			case FN_MTLO:
				lo = register[rd];
				return;
			case FN_MULT: {
				// sign extend
				final long rsValue = register[rs];
				final long rtValue = register[rt];
				final long result = rsValue * rtValue;
				lo = (int) result;
				hi = (int) (result >>> 32);
				return;
			}
			case FN_MULTU: {
				// zero extend
				final long rsValue = zeroExtendInt(register[rs]);
				final long rtValue = zeroExtendInt(register[rt]);
				final long result = rsValue * rtValue;
				lo = (int) result;
				hi = (int) (result >>> 32);
				return;
			}
			case FN_DIV: {
				// divide as signed
				// result is unpredictable for zero, no exceptions thrown
				int rsValue = register[rs];
				int rtValue = register[rt];
				if (rt != 0) {
					lo = rsValue / rtValue;
					hi = rsValue % rtValue;
				}
				return;
			}
			case FN_DIVU: {
				// unpredictable result and no exception for zero
				// zero extend
				final long rsValue = zeroExtendInt(register[rs]);
				final long rtValue = zeroExtendInt(register[rt]);
				if (rtValue != 0) {
					lo = (int) (rsValue / rtValue);
					hi = (int) (rsValue % rtValue);
				}
				return;
			}
			case FN_ADDU:
				register[rd] = register[rs] + register[rt];
				return;
			case FN_SUBU:
				register[rd] = register[rs] - register[rt];
				return;
			case FN_AND:
				register[rd] = register[rs] & register[rt];
				return;
			case FN_OR:
				register[rd] = register[rs] | register[rt];
				return;
			case FN_XOR:
				register[rd] = register[rs] ^ register[rt];
				return;
			case FN_NOR:
				register[rd] = ~(register[rs] | register[rt]);
				return;
			case FN_SLT:
				register[rd] = (register[rs] < register[rt]) ? 1 : 0;
				return;
			case FN_SLTU: {
				// zero extend
				long rsValue = zeroExtendInt(register[rs]);
				long rtValue = zeroExtendInt(register[rt]);
				register[rd] = rsValue < rtValue ? 1 : 0;
				return;
			}
			case FN_TNE:
				if (register[rs] != register[rt]) {
					execException(EX_TRAP, 0, 0, 0);
					throw new RuntimeException("trap");
				}
				return;
			default:
				throw new IllegalArgumentException("invalid fn " + opString(fn));
		}
	}
	
	/**
	 * execute exception.
	 * if excode is interrupt, interrupt code must be provided.
	 * if excode is tlb load/store, vaddr is required.
	 * if interrupt code is south bridge interrupt, irq must be provided.
	 */
	// traps.c trap_init
	// genex.S
	// malta-int.c plat_irq_dispatch (deals with hardware interrupts)
	public final void execException (final int excode, final int interrupt, final int irq, final int vaddr) {
		log.info("exec exception " + excode + " " + exceptionName(excode) + " interrupt " + interrupt + " " + MaltaUtil.interruptName(interrupt) + " irq " + MaltaUtil.irqName(irq));
		
		final boolean isInterrupt = excode == EX_INTERRUPT;
		final boolean isTlbRefill = excode == EX_TLB_LOAD || excode == EX_TLB_STORE;
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
		
		if (isTlbRefill) {
			cpReg[CPR_BADVADDR] = vaddr;
			// context, entryhi, ...
			throw new RuntimeException("tlb refill...");
		}
		
		log.info("epc=" + memory.getSymbols().getNameAddrOffset(cpReg[CPR_EPC]) + " bd=" + (bdmask != 0));
		
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
				long rsValue = register[rs];
				long rtValue = register[rt];
				long result = rsValue * rtValue + getHilo();
				lo = (int) result;
				hi = (int) (result >>> 32);
				return;
			}
			case FN2_MUL: {
				// sign extend, return bottom 32 bits of result
				long rsValue = register[rs];
				long rtValue = register[rt];
				register[rd] = (int) (rsValue * rtValue);
				return;
			}
			case FN2_CLZ: {
				int value = register[rs];
				int n = 0;
				while (n < 32 && (value & (1 << (31 - n))) == 0) {
					n++;
				}
				register[rd] = n;
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
		
		register[rt] = cpReg[cpr];
		return;
	}
	
	/** move to system coprocessor register */
	private final void execCpMoveTo (final int isn) {
		final int rd = rd(isn);
		final int sel = sel(isn);
		final int cpr = cpr(rd, sel);
		final int oldValue = cpReg[cpr];
		final int newValue = register[rt(isn)];
		if (oldValue != newValue) {
			//log.debug("mtc0 " + cpRegName(rd, sel) + " 0x" + Integer.toHexString(oldValue) + " <- 0x" + Integer.toHexString(newValue));
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
			//log.info("interrupts " + (interruptsEnabled ? "enabled" : "disabled") + " in " + calls.callString());
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
				log.info("exception return " + memory.getSymbols().getNameAddrOffset(epc));
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
				register[rt] = fpReg[fs];
				return;
				
			case FP_RS_MTC1:
				fpReg[fs] = register[rt];
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
				register[rt] = fpControlReg[fs];
				return;
				
			default:
				throw new RuntimeException("read unimplemented fp control register " + fs);
		}
	}
	
	private final void execFpuCopyTo (final int isn) {
		final int rtValue = register[rt(isn)];
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
