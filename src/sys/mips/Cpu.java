package sys.mips;

import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import sys.malta.MaltaUtil;
import sys.util.Log;
import sys.util.Logger;
import sys.util.Symbols;

import static sys.mips.CpuConstants.*;
import static sys.mips.CpuFunctions.*;
import static sys.mips.InstructionUtil.*;

public final class Cpu {
	
	private static final ThreadLocal<Cpu> instance = new ThreadLocal<>();
	private static final Logger log = new Logger("Cpu");
	
	/** allow other classes to access cpu */
	public static Cpu getInstance() {
		return instance.get();
	}
	
	/** general purpose registers */
	private final int[] register = new int[32];
	/** coprocessor 0 registers (register+selection*32) */
	private final int[] cpRegister = new int[64];
	private final Map<String, int[]> isnCount = new HashMap<>();
	private final Memory memory;
	private final boolean littleEndian;
	private final Deque<CpuExceptionParams> exceptions = new ArrayDeque<>();
	private final CallLogger calls = new CallLogger(this);
	/** 0 for little endian, 3 for big endian */
	private final int wordAddrXor;
	private final Fpu fpu = new Fpu(this);
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final PropertyChangeSupport support = new PropertyChangeSupport(this);
	private final List<Log> logs = new ArrayList<>();
	private final Symbols symbols = new Symbols();
	
	private volatile long cycle = 1;
	private volatile boolean logScheduled;
	private volatile long waitTimeNs;
	private volatile int waitCount;
	
	/**
	 * within execOp: address of current instruction. if pc2 != pc + 4 then the
	 * current instruction is in a branch delay slot.
	 */
	private int pc;
	/** within execOp: address of next instruction (the branch delay slot) */
	private int pc2;
	/** within execOp: address of next next instruction (change this to branch) */ 
	private int pc3;
	private int loadLinkedVaddr;
	private int loadLinkedPaddr;
	private boolean loadLinkedBit;
	private boolean kernelMode;
	private int hi;
	private int lo;
	private boolean countIsns; 
	//private boolean disasm;
	private int disasmCount;
	// denormalised from CPR_STATUS_IE
	private boolean interruptsEnabled;
	// equivalent to CPR_STATUS_EXL?
	private boolean execException;
	
	public Cpu (int memsize, boolean littleEndian) {
		this.memory = new Memory(memsize, littleEndian);
		this.littleEndian = littleEndian;
		this.wordAddrXor = littleEndian ? 0 : 3;
		
		memory.setKernelMode(true);
		memory.init(this);
		
		for (String name : InstructionSet.getInstance().getNameMap().keySet()) {
			isnCount.put(name, new int[1]);
		}
		
		// default values on reboot
		setCpValue(CPR_STATUS_EXL, true);
		setCpValue(CPR_STATUS_ERL, true);
		setCpValue(CPR_STATUS_BEV, true);
		setCpValue(CPR_STATUS_CU0, true);
		setCpValue(CPR_STATUS_CU1, true); // should be false for 4kc...
		statusUpdated();
		
		// 0x10000 = MIPS, 0x8000 = 4KC 
		setCpValue(CPR_PRID_PROCID, 0x80);
		setCpValue(CPR_PRID_COMPANYID, 1);
		
		// 15: big endian, 7: TLB
		cpRegister[CPR_CONFIG] = (1 << 31) | ((littleEndian?0:1) << 15) | (1 << 7) | (1 << 1);
		
		// 25: tlb entries - 1
		// should enable 3: watch registers and 1: ejtag, but we don't want them
		cpRegister[CPR_CONFIG1] = (15 << 25);
		
		cpRegister[CPR_COMPARE] = -1;
		
		// entry point should be set by elf loader
		//setPc(EXV_RESET);
	}
	
	public final int[] getCpRegisters () {
		return cpRegister;
	}
	
	public int getCpValue(CpRegConstant c) {
		return c.get(cpRegister);
	}
	
	public boolean getCpValueBoolean(CpRegConstant c) {
		return c.get(cpRegister) != 0;
	}
	
	public void setCpValue(CpRegConstant c, int v) {
		c.set(cpRegister, v);
	}
	
	public void setCpValue(CpRegConstant c, boolean b) {
		c.set(cpRegister, b ? 1 : 0);
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
	
	/** 
	 * load pc2 and pc3 (the existing delay slot is discarded).
	 */
	public final void setPc (int pc) {
		// don't actually set pc, it is derived from pc2
		this.pc2 = pc;
		this.pc3 = pc + 4;
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
	
	public final Fpu getFpu () {
		return fpu;
	}

	public final ScheduledExecutorService getExecutor () {
		return executor;
	}

	public final PropertyChangeSupport getSupport () {
		return support;
	}
	
	public Symbols getSymbols () {
		return symbols;
	}

	public final void addLog(Log log) {
		synchronized (this) {
			logs.add(log);
			if (!logScheduled) {
				executor.schedule(() -> fireLogs(), 1000, TimeUnit.MILLISECONDS);
				logScheduled = true;
			}
		}
	}

	private void fireLogs () {
		Log[] a;
		synchronized (this) {
			a = logs.toArray(new Log[logs.size()]);
			logs.clear();
			logScheduled = false;
		}
		if (a.length > 0) {
			support.firePropertyChange("logs", null, a);
		}
	}

	/** never returns, throws runtime exception... */
	public final void run () {
		final long startTime = System.nanoTime();
		
		try {
			instance.set(this);
			calls.call(pc2);
			log.println("run");
			String[] regstr = new String[32];
			// this should only be checked during call
//			int f = memory.getSymbols().getAddr("size_fifo");
//			if (f == 0) {
//				throw new RuntimeException();
//			}
//			boolean x = false;
			
			while (true) {
				if (register[0] != 0) {
					register[0] = 0;
				}
				
				//final boolean printCall = calls.isPrintAfterNext();
				
				// set up pc before handling exception
				pc = pc2;
				pc2 = pc3;
				pc3 += 4;
				
//				if (pc == f) {
//					log.println("----- size_fifo -----");
//					disasm = true;
//					calls.setPrintCalls(true);
//					x = true;
//				}
				
//				if (x) {
//					log.println(memory.getSymbols().getNameOffset(pc));
//				}
				
				// every 1024 cycles
				if ((cycle & 0xfff) == 0) {
					// TODO should probably also do this after eret...
					if (checkException()) {
						// restart loop, start executing exception handler...
						continue;
					}
				}
				
				try {
					// this might cause tlb miss...
					final int isn = memory.loadWord(pc);
					
					if (disasmCount > 0) {
						log.println(CpuUtil.gpRegString(this, regstr));
						log.println(InstructionUtil.isnString(this, isn));
						if (disasmCount > 0) {
							disasmCount--;
						}
					}
					
					// to signal a synchronous exception, either
					// 1. call execException and return (more efficient)
					// 2. throw new CpuException (easier in deep call stack)
					// this instruction will be re-executed after the exception handler returns.
					// to interrupt asynchronously: 
					// 1. create exception params and call addException 
					execOp(isn);
					
				} catch (CpuException e) {
					log.println("caught " + e);
					execException(e.ep);
				}
				
				//if (printCall) {
				//	calls.printCall();
				//}
				
				//if (countIsns) {
				//	isnCount.get(IsnSet.getInstance().getIsn(isn).name)[0]++;
				//}
				
				final int cmp = cpRegister[CPR_COMPARE];
				final int count = (int) (cycle >>> 1);
				if (cmp == count) {
					if (interruptsEnabled) {
						throw new RuntimeException("compare hit");
					}
				}
				
				//if (singleStep) {
				//	while (System.in.read() != 10) {
				//		//
				//	}
				//}
				
				cycle++;
				
				if (cycle > 200_000_000) {
					throw new RuntimeException("timeout");
				}
			}
			
		} catch (Exception e) {
			throw new RuntimeException("exception in cycle " + cycle + ", "
					+ "kernel mode: " + kernelMode + ", "
					+ "interrupts enabled: " + interruptsEnabled + ", " 
					+ "executing exeception: " + execException + ", "
					+ "program counter: " + symbols.getNameAddrOffset(pc) + ", "
					+ calls.callString(), e);
			
		} finally {
			final long endTime = System.nanoTime();
			final long duration = endTime - startTime - waitTimeNs;
			log.println("ended");
			log.println("nanoseconds per isn: " + (duration / cycle));
			instance.remove();
			executor.shutdown();
			fireLogs();
		}
	}
	
	/** exec exception if there is one */
	private boolean checkException () {
		if (interruptsEnabled) {
//			if (execException) {
//				throw new RuntimeException();
//			}
			CpuExceptionParams ep;
			synchronized (this) {
				ep = exceptions.poll();
			}
			if (ep != null) {
				execException(ep);
				return true;
			}
		}
		return false;
	}

	public final void addException (final CpuExceptionParams ep) {
		//log.println("add exn " + ep);
		synchronized (this) {
			exceptions.add(ep);
			// wake up...
			notifyAll();
		}
	}
	
	/**
	 * execute exception (i.e. set up exception state).
	 * exception completes when linux calls eret.
	 */
	// traps.c trap_init
	// genex.S
	// malta-int.c plat_irq_dispatch (deals with hardware interrupts)
	private final void execException (CpuExceptionParams ep) {
		//log.println("exec exception " + ep);
//		log.println(CpuUtil.gpRegString(this, null));
//		log.println(IsnUtil.isnString(this, memory.loadWord(pc)));
		
		execException = true;

		if (getCpValueBoolean(CPR_STATUS_BEV)) {
			// we don't have a boot rom...
			throw new RuntimeException("bootstrap exception");
		}
		
		if (getCpValueBoolean(CPR_STATUS_EXL)) {
			// eh...
			throw new RuntimeException("exception in exception...");
		}
		
		boolean isTlbException = false;
		boolean isInterruptException = false;
		boolean isSouthbridgeInterrupt = false;
		// this passes the external INT code to the cpu in a strange way
		// malta-int.c irq_ffs()
		int pendingMask = 0;
		
		switch (ep.excode) {
			case EX_INTERRUPT:
				isInterruptException = true;
				pendingMask = 1 << ep.interrupt.intValue();
				if ((getCpValue(CPR_STATUS_IM) & pendingMask) == 0) {
					throw new RuntimeException("masked interrupt " + ep.interrupt);
				}
				switch (ep.interrupt.intValue()) {
					case MaltaUtil.INT_SOUTHBRIDGE_INTR:
						isSouthbridgeInterrupt = true;
						break;
					default:
						throw new RuntimeException("unknown interrupt " + ep.interrupt);
				}
				break;
			case EX_TLB_LOAD:
			case EX_TLB_STORE:
				isTlbException = true;
				break;
			default:
				throw new RuntimeException("unexpected exception " + ep.excode + ": " + InstructionUtil.exceptionString(ep.excode));
		}
		
		// actually handle exception
		
		setCpValue(CPR_STATUS_EXL, true);
		setCpValue(CPR_CAUSE_EXCODE, ep.excode);
		setCpValue(CPR_CAUSE_IP, pendingMask);
		
		final boolean isDelaySlot = pc2 != pc + 4;
		setCpValue(CPR_CAUSE_BD, isDelaySlot);
		setCpValue(CPR_EPC_VALUE, isDelaySlot ? pc - 4 : pc);
		
		if (isTlbException) {
			final int vpn2 = vpn2(ep.vaddr.intValue());
			setCpValue(CPR_BADVADDR_BADVADDR, ep.vaddr.intValue());
			setCpValue(CPR_CONTEXT_BADVPN2, vpn2);
			setCpValue(CPR_ENTRYHI_VPN2, vpn2);
		}
		
		//log.println("epc=" + memory.getSymbols().getNameAddrOffset(cpRegister[CPR_EPC]) + " delaySlot=" + isDelaySlot);
		
		statusUpdated();
		
		if (isSouthbridgeInterrupt) {
			// uh....
			memory.getMalta().setIrq(ep.irq.intValue());
		}
		
		if (isTlbException && ep.tlbRefill.booleanValue()) {
			//log.println("jump to tlb refill vector");
			setPc(EXV_TLBREFILL);
			
		} else if (isInterruptException && getCpValueBoolean(CPR_CAUSE_IV)) {
			//log.println("jump to interrupt vector");
			setPc(EXV_INTERRUPT);
			
		} else {
			//log.println("jump to general exception vector");
			setPc(EXV_EXCEPTION);
		}
		
		calls.push("ex" + ep.excode + "int" + ep.interrupt + "irq" + ep.irq);
		calls.call(pc);
	}
	
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
				fpu.execFpuRs(isn);
				return;
			case OP_COP1X:
				fpu.execFpuFnX(isn);
				return;
			case OP_SPECIAL2:
				execFunction2(isn);
				return;
			case OP_LWC1:
				fpu.setFpRegister(rt, memory.loadWord(register[rs] + simm));
				return;
			case OP_LDC1: {
				// least significant word first...
				final int a = register[rs] + simm;
				fpu.setFpRegister(rt + 1, memory.loadWord(a));
				fpu.setFpRegister(rt, memory.loadWord(a + 4));
				return;
			}
			case OP_SWC1:
				memory.storeWord(register[rs] + simm, fpu.getFpRegister(rt));
				return;
			case OP_SDC1: {
				// least significant word first...
				final int a = register[rs] + simm;
				memory.storeWord(a, fpu.getFpRegister(rt + 1));
				memory.storeWord(a + 4, fpu.getFpRegister(rt));
				return;
			}
			case OP_J:
				execJump(isn);
				checkCall(false);
				return;
			case OP_JAL:
				execLink();
				execJump(isn);
				calls.call(pc3);
				return;
			case OP_BLEZ:
				if (register[rs] <= 0) {
					execBranch(isn);
					checkCall(false);
				}
				return;
			case OP_BEQ:
				if (register[rs] == register[rt]) {
					execBranch(isn);
					checkCall(false);
				}
				return;
			case OP_BNE:
				if (register[rs] != register[rt]) {
					execBranch(isn);
					checkCall(false);
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
					checkCall(false);
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
				// short promoted to int for shift
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
					log.println("store conditional word fail: va=" + Integer.toHexString(va) + " pa=" + Integer.toHexString(pa) + " ll=" + loadLinkedBit);
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
				// zero extend
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
				final int aa = a & ~3;
				// force tlb store error
				memory.translate(aa, true);
				final int lealign = (a & 3) ^ wordAddrXor;
				final int word = memory.loadWord(aa);
				final int lsh = (lealign + 1) * 8;
				final int rsh = 32 - lsh;
				memory.storeWord(aa, (word & (int) (ZX_INT_MASK << lsh)) | (register[rt] >>> rsh));
				return;
			}
			case OP_SWR: {
				// lealign 0: reg << 0 | memmask >> 32
				// lealign 1: reg << 8 | memmask >> 24
				// lealign 2: reg << 16 | memmask >> 16
				// lealign 3: reg << 24 | memmask >> 8
				final int a = register[rs] + simm;
				final int aa = a & ~3;
				// force tlb store error
				memory.translate(aa, true);
				final int lealign = (a & 3) ^ wordAddrXor;
				final int word = memory.loadWord(aa);
				final int lsh = lealign * 8;
				final int rsh = 32 - lsh;
				memory.storeWord(aa, (register[rt] << lsh) | (word & (int) (ZX_INT_MASK >>> rsh)));
				return;
			}
			case OP_PREF:
				// no-op
				return;
			default:
				throw new RuntimeException("invalid op " + opString(op));
		}
	}

	private void checkCall (boolean linked) {
		String pcname = symbols.getName(pc);
		String nextpcname = symbols.getName(pc3);
		if (!pcname.equals(nextpcname)) {
			calls.call(pc, linked);
			//throw new RuntimeException("pcname=" + pcname + " nextpcname=" + nextpcname);
		}
	}
	
	/** update nextpc with jump */
	private final void execJump (final int isn) {
		pc3 = jump(isn, pc2);
	}

	/** update nextpc with branch */
	public final void execBranch (final int isn) {
		pc3 = branch(isn, pc2);
	}

	private final void execLink () {
		register[31] = pc3;
	}
	
	private final void execRegImm (final int isn) {
		final int rs = rs(isn);
		final int rt = rt(isn);
		boolean link = false;
		
		switch (rt) {
			case RT_BGEZAL:
				link = true;
				execLink();
				//$FALL-THROUGH$
			case RT_BGEZ:
				if (register[rs] >= 0) {
					execBranch(isn);
					checkCall(link);
				}
				return;
			case RT_BLTZAL:
				link = true;
				execLink();
				//$FALL-THROUGH$
			case RT_BLTZ:
				if (register[rs] < 0) {
					execBranch(isn);
					checkCall(link);
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
				pc3 = register[rs];
				if (rs == 31) {
					calls.ret();
				}
				return;
			case FN_JALR:
				register[rd] = pc3;
				pc3 = register[rs];
				calls.call(pc3);
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
				// won't this try to re-execute the syscall?
				// unless linux is smart enough to add 4 to the return address...
				execException(new CpuExceptionParams(EX_SYSCALL));
				return;
			case FN_BREAK:
				execException(new CpuExceptionParams(EX_BREAKPOINT));
				return;
			case FN_SYNC:
				// no-op
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
				lo = register[rs];
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
				if (rtValue != 0) {
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
					execException(new CpuExceptionParams(EX_TRAP));
					throw new RuntimeException("trap");
				}
				return;
			default:
				throw new IllegalArgumentException("invalid fn " + opString(fn));
		}
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
		final int cpr = cprIndex(rd, sel);
		
		switch (cpr) {
			case CPR_STATUS:
			case CPR_PRID:
			case CPR_CONFIG:
			case CPR_CONFIG1:
			case CPR_CAUSE:
			case CPR_ENTRYHI:
			case CPR_WIRED:
			case CPR_EPC:
			case CPR_BADVADDR:
			case CPR_CONTEXT:
				break;
			case CPR_COUNT:
				// update this only when read
				cpRegister[cpr] = (int) (cycle >>> 1);
				break;
			default:
				throw new RuntimeException("move from unknown cp reg " + cpRegName(rd, sel));
		}
		
		register[rt] = cpRegister[cpr];
		return;
	}
	
	/** move to system coprocessor register */
	private final void execCpMoveTo (final int isn) {
		final int rd = rd(isn);
		final int sel = sel(isn);
		final int cpr = cprIndex(rd, sel);
		final int oldValue = cpRegister[cpr];
		final int newValue = register[rt(isn)];
		if (oldValue != newValue) {
			//log.debug("mtc0 " + cpRegName(rd, sel) + " 0x" + Integer.toHexString(oldValue) + " <- 0x" + Integer.toHexString(newValue));
		}
		
		switch (cpr) {
			case CPR_INDEX:
				cpRegister[cpr] = newValue & 0xf;
				return;
			case CPR_ENTRYLO0:
			case CPR_ENTRYLO1:
				cpRegister[cpr] = newValue & 0x7fff_ffff;
				return;
			case CPR_ENTRYHI:
				cpRegister[cpr] = newValue & 0xffff_f0ff;
				memory.setAsid(getCpValue(CPR_ENTRYHI_ASID));
				return;
			case CPR_PAGEMASK:
				cpRegister[cpr] = newValue & 0x00ff_f000;
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
				log.println("set compare " + Integer.toHexString(newValue));
				cpRegister[cpr] = newValue;
				return;
			case CPR_EPC:
				cpRegister[cpr] = newValue;
				return;
			default:
				throw new RuntimeException("move to unknown cp reg " + cpRegName(rd, sel));
		}
	}
	
	private final void setCpCause (final int newValue) {
		final int value = cpRegister[CPR_CAUSE];
		// only allow change to mask
		int mask = CPR_CAUSE_IV.mask;
		if ((newValue & ~mask) != 0) {
			throw new RuntimeException("unknown cause value " + Integer.toHexString(newValue));
		}
		cpRegister[CPR_CAUSE] = (value & ~mask) | (newValue & mask);
	}
	
	private final void setCpStatus (final int newValue) {
		// only allow change to these
		final int mask = CPR_STATUS_CU1.mask | CPR_STATUS_CU0.mask | CPR_STATUS_BEV.mask 
				| CPR_STATUS_IM.mask | CPR_STATUS_UM.mask | CPR_STATUS_ERL.mask 
				| CPR_STATUS_EXL.mask | CPR_STATUS_IE.mask;
		if ((newValue & ~mask) != 0) {
			throw new RuntimeException("unknown status value " + Integer.toHexString(newValue));
		}
		
		// don't need to preserve anything
		cpRegister[CPR_STATUS] = newValue & mask;

		statusUpdated();
	}
	
	private final void statusUpdated () {
		final boolean ie = getCpValueBoolean(CPR_STATUS_IE);
		final boolean exl = getCpValueBoolean(CPR_STATUS_EXL);
		final boolean erl = getCpValueBoolean(CPR_STATUS_ERL);
		final boolean um = getCpValueBoolean(CPR_STATUS_UM);
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
			case CP_FN_TLBWI:
				updateEntry(getCpValue(CPR_INDEX_INDEX));
				return;
			case CP_FN_TLBWR:
				updateEntry(random());
				return;
			case CP_FN_TLBP: {
				int i = memory.probe(getCpValue(CPR_ENTRYHI_VPN2));
				if (i >= 0) {
					setCpValue(CPR_INDEX_INDEX, i);
					setCpValue(CPR_INDEX_PROBEFAIL, false);
				} else {
					setCpValue(CPR_INDEX_PROBEFAIL, true);
				}
				return;
			}
			case CP_FN_WAIT:
				// no idea...
				synchronized (this) {
					while (exceptions.size() == 0) {
						try {
							log.println("waiting...");
							long t = System.nanoTime();
							wait();
							t = System.nanoTime() - t;
							waitTimeNs += t;
							waitCount++;
							if (waitCount > 10) {
								throw new RuntimeException("wait count exceeded");
							}
							log.println("continuing after " + (t/1000000000.0) + " seconds");
						} catch (InterruptedException e) {
							log.println("interrupted in wait...");
						}
					}
				}
				return;
			case CP_FN_ERET: {
				final int epc = cpRegister[CPR_EPC];
				//log.println("exception return " + memory.getSymbols().getNameAddrOffset(epc));
				if (getCpValueBoolean(CPR_STATUS_ERL)) {
					throw new RuntimeException("eret with erl");
				}
				// no delay slot
				setPc(epc);
				setCpValue(CPR_STATUS_EXL, false);
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

	private void updateEntry (final int i) {
		log.println("update entry " + i + " in " + symbols.getNameAddrOffset(pc));
		
		final Entry e = memory.getEntry(i);
		e.pageMask = getCpValue(CPR_PAGEMASK_MASK);
		e.virtualPageNumber2 = getCpValue(CPR_ENTRYHI_VPN2);
		e.addressSpaceId = getCpValue(CPR_ENTRYHI_ASID);
		e.global = getCpValueBoolean(CPR_ENTRYLO0_GLOBAL) && getCpValueBoolean(CPR_ENTRYLO1_GLOBAL);
		
		e.data[0].physicalFrameNumber = getCpValue(CPR_ENTRYLO0_PFN);
		e.data[0].dirty = getCpValueBoolean(CPR_ENTRYLO0_DIRTY);
		e.data[0].valid = getCpValueBoolean(CPR_ENTRYLO0_VALID);
		
		e.data[1].physicalFrameNumber = getCpValue(CPR_ENTRYLO1_PFN);
		e.data[1].dirty = getCpValueBoolean(CPR_ENTRYLO1_DIRTY);
		e.data[1].valid = getCpValueBoolean(CPR_ENTRYLO1_VALID);
		
		log.println("updated tlb[" + i + "]=" + e);
		
		if (e.pageMask != 0) {
			throw new RuntimeException("non zero page mask");
		}
	}
	
}
