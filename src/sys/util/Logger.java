package sys.util;

import sys.mips.Cpu;
import sys.ui.LoggerJPanel;

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
			//String t = Thread.currentThread().getName();
			Log log;
			if (cpu != null) {
//				final String ie = cpu.isInterruptsEnabled() ? "i" : "";
//				final String km = cpu.isKernelMode() ? "k" : "";
//				final String ex = cpu.isExecException() ? "x" : "";
				//msg = "[" + t + ":" + cpu.getCycle() + ":" + km + ie + ex + ":" + c.getSimpleName() + "] " + msg;
				log = new Log(cpu.getCycle(), cpu.isKernelMode(), cpu.isInterruptsEnabled(), cpu.isExecException(), name, msg);
			} else {
				//msg = "[" + t + ":" + c.getSimpleName() + "] " + msg;
				log = new Log(name, msg);
			}
			System.out.println(log.toString());
			
			LoggerJPanel.instance.addLog(log);
		}
	}
}
