package sys.mips;

import java.util.*;

import sys.util.Logger;

/**
 * log function calls
 */
public class CallLogger {
	
	private static final String INLINE = "#";

	private static final Logger log = new Logger("Calls");
	
	private final List<List<String>> threads = new ArrayList<>();
	private final List<String> threadNames = new ArrayList<>();
	private final Cpu cpu;
	private boolean printAfterNext;
	private boolean printCalls;
	
	public CallLogger (Cpu cpu) {
		this.cpu = cpu;
		push("main");
	}
	
	public boolean isPrintCalls () {
		return printCalls;
	}

	public void setPrintCalls (boolean printCalls) {
		this.printCalls = printCalls;
	}

	public boolean isPrintAfterNext () {
		if (printAfterNext) {
			printAfterNext = false;
			return true;
		} else {
			return false;
		}
	}
	
	public void call (int addr) {
		call(addr, true);
	}
	
	public void call (int addr, boolean linked) {
		final List<String> calls = threads.get(0);
		final String nameos = cpu.getMemory().getSymbols().getNameOffset(addr);
		calls.add(linked ? nameos : INLINE + nameos);
		
//		final String name = cpu.getMemory().getSymbols().getName(addr);
//		if (printCall(name)) {
//			printCall = true;
//		}
		
		if (printCalls) {
			log.println("call " + callString());
		}
	}
	
	private boolean printCall (String name) {
		switch (name) {
			case "proc_tty_init":
			case "proc_register":
			case "proc_mkdir":
			case "proc_mkdir_mode":
			case "__proc_create":
			case "__xlate_proc_name":
			case "strchr":
			case "memcmp":
			case "memcpy":
			case "memset":
				return true;
			default:
				return false;
		}
	}
	
	public void printCall() {
		final String name = cpu.getMemory().getSymbols().getName(cpu.getPc());
		int a0 = cpu.getRegister(CpuConstants.REG_A0);
		int a1 = cpu.getRegister(CpuConstants.REG_A1);
		int a2 = cpu.getRegister(CpuConstants.REG_A2);
		int a3 = cpu.getRegister(CpuConstants.REG_A3);
		List<String> args = new ArrayList<>();
		
		switch (name) {
			case "proc_register":
				//  int proc_register(struct proc_dir_entry * dir, struct proc_dir_entry * dp)
				args.add(Integer.toHexString(a0));
				args.add(Integer.toHexString(a1));
				break;
			case "proc_mkdir":
				//*proc_mkdir(const char *name, struct proc_dir_entry *parent)
				args.add(a0 < 0 ? MemoryUtil.loadString(cpu.getMemory(), a0) : null);
				args.add(Integer.toHexString(a1));
				break;
			case "proc_mkdir_mode":
				// const char *name, mode_t mode, struct proc_dir_entry *parent)
				args.add(a0 < 0 ? MemoryUtil.loadString(cpu.getMemory(), a0) : null);
				args.add(Integer.toHexString(a1));
				args.add(Integer.toHexString(a2));
				break;
			case "__proc_create":
				// (struct proc_dir_entry **parent, const char *name, mode_t mode, nlink_t nlink)
				args.add(Integer.toHexString(a0));
				args.add(a1 < 0 ? MemoryUtil.loadString(cpu.getMemory(), a1) : null);
				args.add(Integer.toHexString(a2));
				args.add(Integer.toHexString(a3));
				break;
			case "__xlate_proc_name":
				// (const char *name, struct proc_dir_entry **ret, const char **residual)
				args.add(a0 < 0 ? MemoryUtil.loadString(cpu.getMemory(), a0) : null);
				args.add(Integer.toHexString(a1));
				args.add(Integer.toHexString(a2));
				//if (args.get(0).equals("tty/ldisc")) { cpu.setSingleStep(true); cpu.setDisasm(true); }
				break;
			case "strchr":
				// (const char *s, int c)
				args.add(a0 < 0 ? MemoryUtil.loadString(cpu.getMemory(), a0) : null);
				args.add("" + (char) a1);
				break;
			case "memcmp":
				// (const void *cs, const void *ct, size_t count)
				args.add(Integer.toHexString(a0) + "[" + (a0 < 0 ? MemoryUtil.loadString(cpu.getMemory(), a0, 16) : null) + "]");
				args.add(Integer.toHexString(a1) + "[" + (a1 < 0 ? MemoryUtil.loadString(cpu.getMemory(), a1, 16) : null) + "]");
				args.add("" + a2);
				break;
			case "memcpy":
				// void *memcpy(void *dest, const void *src, size_t count)
				args.add(Integer.toHexString(a0));
				args.add(Integer.toHexString(a1) + "[" + (a1 < 0 ? MemoryUtil.loadString(cpu.getMemory(), a1, 16) : null) + "]");
				args.add("" + a2);
				//if (a0 == 0x8180bfd5) { cpu.setSingleStep(true); cpu.setDisasm(true); }
				break; 
			case "memset":
				// void *memset(void *s, int c, size_t count)
				args.add(Integer.toHexString(a0));
				args.add(Integer.toHexString(a1 & 0xff));
				args.add("" + a2);
				break;
			case "proc_tty_init":
				break;
			default:
				throw new RuntimeException("unexpected call " + name);
		}
		
		log.println("call " + name + args);
	}
	
	public void ret () {
		final List<String> calls = threads.get(0);
		while (calls.size() > 0 && calls.get(calls.size() - 1).startsWith(INLINE)) {
			calls.remove(calls.size() - 1);
		}
		if (calls.size() > 0) {
			calls.remove(calls.size() - 1);
		}
		
//		final String name = cpu.getMemory().getSymbols().getName(cpu.getPc());
//		if (printCall(name)) {
//			int v0 = cpu.getRegister(Constants.REG_V0);
//			int v1 = cpu.getRegister(Constants.REG_V1);
//			final String nameos = cpu.getMemory().getSymbols().getNameOffset(cpu.getPc());
//			cpu.getLog().info("ret  " + nameos + " => " + Integer.toHexString(v0));
//		}
	}
	
	public void push (String name) {
		threadNames.add(0, name);
		threads.add(0, new ArrayList<>());
	}
	
	public void pop () {
		// could be multiple returns
		if (threads.size() > 1) {
			threadNames.remove(0);
			threads.remove(0);
		} else {
			threads.get(0).clear();
		}
	}
	
	public String callString () {
		return "thread: " + threadNames.get(0) + ", calls: " + threads.get(0);
	}
	
	/** cached previous calls */
	private String[] pa;
	
	public String[] calls () {
		List<String> l = threads.get(0);
		String[] a = l.toArray(new String[l.size()]);
		if (pa != null && Arrays.equals(pa, a)) {
			return pa;
		} else {
			pa = a;
			return a;
		}
	}
	
}
