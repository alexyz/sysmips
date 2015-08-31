package sys.mips;

import java.util.*;

/**
 * log function calls
 */
public class CallLogger {
	
	private final List<List<String>> threads = new ArrayList<>();
	private final Cpu cpu;
	
	public CallLogger (Cpu cpu) {
		this.cpu = cpu;
		push();
	}
	
	public void call (int addr) {
		final List<String> calls = threads.get(0);
		calls.add(cpu.getMemory().getSymbols().getName(addr, false));
	}
	
	public void ret () {
		final List<String> calls = threads.get(0);
		if (calls.size() > 0) {
			calls.remove(calls.size() - 1);
		}
	}
	
	public void push () {
		threads.add(0, new ArrayList<>());
	}
	
	public void pop () {
		threads.remove(0);
	}
	
	public String callString () {
		final StringBuilder sb = new StringBuilder("entry-" + threads.size());
		final List<String> calls = threads.get(0);
		
		for (String call : calls) {
			sb.append("/").append(call);
		}
		
		sb.append("/").append(cpu.getMemory().getSymbols().getName(cpu.getPc(), false));
		
		return sb.toString();
	}
	
}
