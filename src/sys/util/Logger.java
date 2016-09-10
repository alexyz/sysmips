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
				log = new Log(cpu.getCycle(), cpu.isKernelMode(), cpu.isInterruptsEnabled(), cpu.isExecException(), name, msg, cpu.getCalls().calls());
				cpu.addLog(log);
			} else {
				log = new Log(name, msg);
			}
			//System.out.println(log.toString());
		}
	}
}
