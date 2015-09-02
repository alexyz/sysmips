package sys.mips;

import java.io.PrintStream;
import java.util.*;

public class CpuLogger {
	
	private static final int MAX = 200;

	public static final CpuLogger getInstance() {
		return Cpu.getInstance().getLog();
	}
	
	private final LinkedList<String> log = new LinkedList<>();
	private final Cpu cpu;
	
	public CpuLogger (Cpu cpu) {
		this.cpu = cpu;
	}
	
	public void print (PrintStream ps) {
		System.out.println("log buffer is");
		for (String line : log) {
			ps.println(line);
		}
	}
	
	public void debug (String msg, Object... args) {
		log(args.length == 0 ? msg : String.format(msg, args), false);
	}
	
	public void info (String msg, Object... args) {
		log(args.length == 0 ? msg : String.format(msg, args), true);
	}
	
	private void log (String msg, boolean print) {
		while (log.size() > MAX) {
			log.removeFirst();
		}
		final String ie = cpu.isInterruptsEnabled() ? "i" : "";
		final String km = cpu.isKernelMode() ? "k" : "";
		final String ex = cpu.isExecException() ? "x" : "";
		final String s = "[" + cpu.getCycle() + ":" + km + ie + ex + "] " + msg;
		log.add(s);
		if (print) {
			System.out.println(s);
		}
	}
}
