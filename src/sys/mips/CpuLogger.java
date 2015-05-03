package sys.mips;

import java.io.PrintStream;
import java.util.*;

public class CpuLogger {
	
	private final LinkedList<String> log = new LinkedList<>();
	private final List<String> calls = new ArrayList<>();
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
	
	public void debug(String msg, Object... args) {
		log(String.format(msg, args), false);
	}
	
	public void info(String msg, Object... args) {
		log(String.format(msg, args), true);
	}
	
	private void log (String msg, boolean print) {
		while (log.size() > 50) {
			log.removeFirst();
		}
		final String s = "[" + cpu.getCycle() + "] " + msg;
		log.add(s);
		if (print) {
			System.out.println(s);
		}
	}
	
	public void call (int addr) {
		calls.add(cpu.getMemory().getSymbols().getName(addr, false));
		StringBuilder sb = new StringBuilder();
		for (String call : calls) {
			sb.append("/").append(call);
		}
		info("call " + sb.toString());
	}
	
	public void ret () {
		if (calls.size() > 0) {
			calls.remove(calls.size() - 1);
		}
	}
	
}
