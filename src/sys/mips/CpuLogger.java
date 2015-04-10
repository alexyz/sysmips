package sys.mips;

import java.util.*;

public class CpuLogger {
	
	private final List<String> calls = new ArrayList<>();
	private final Cpu cpu;
	
	public CpuLogger (Cpu cpu) {
		this.cpu = cpu;
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