package sys.util;

import sys.mips.Cpu;

public class Logger {
	
	public static int rootLevel = 1;
	private final Class<?> c;

	public Logger (Class<?> c) {
		this.c = c;
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
			String t = Thread.currentThread().getName();
			if (cpu != null) {
				final String ie = cpu.isInterruptsEnabled() ? "i" : "";
				final String km = cpu.isKernelMode() ? "k" : "";
				final String ex = cpu.isExecException() ? "x" : "";
				msg = "[" + t + ":" + cpu.getCycle() + ":" + km + ie + ex + ":" + c.getSimpleName() + "] " + msg;
			} else {
				msg = "[" + t + ":" + c.getSimpleName() + "] " + msg;
			}
			System.out.println(msg);
		}
	}
}
