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
		for (String line : log) {
			ps.println(line);
		}
	}
	
	public void info (String msg) {
		while (log.size() > 50) {
			log.removeFirst();
		}
		log.add(cpu.getCycle() + ": " + msg);
	}
	
	public void call (int addr) {
		calls.add(cpu.getMemory().getSymbols().getName(addr, false));
		StringBuilder sb = new StringBuilder();
		for (String call : calls) {
			sb.append("/").append(call);
		}
		System.out.println(sb);
	}
	
	public void ret () {
		if (calls.size() > 0) {
			calls.remove(calls.size() - 1);
		}
	}
}
