package sys.util;

import sys.mips.Cpu;

public class Logger {
	
	public static int rootLevel = 1;
	
	private final String name;

	public Logger (String name) {
		this.name = name;
	}
	
	public void println (String msg) {
		println(1, msg);
	}
	
	public void println (String format, Object... args) {
		println(1, String.format(format, args));
	}
	
	public void println (int level, String msg) {
		if (level >= rootLevel) {
			Cpu cpu = Cpu.getInstance();
			Log log;
			if (cpu != null) {
				String sym = cpu.getSymbols().getNameOffset(cpu.getPc());
				boolean k = cpu.isKernelMode();
				boolean i = cpu.isInterruptsEnabled();
				boolean x = cpu.isExecException();
				log = new Log(cpu.getCycle(), k, i, x, name, msg, sym);
				cpu.addLog(log);
			} else {
				log = new Log(name, msg);
			}
			//System.out.println(log.toString());
		}
	}
}
