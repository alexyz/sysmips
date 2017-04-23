package sys.mips;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import sys.malta.MaltaUtil;

public class CpuStats {
	public final Map<String, int[]> isnCount = new HashMap<>();
	public final int[] exceptions = new int[32];
	public final int[] irqs = new int[16];
	public final int[] interrupts = new int[16];
	public volatile long endTimeNs;
	public volatile long startTimeNs;
	public volatile long waitTimeNs;
	public volatile int waitCount;
	public volatile int scSuccess, scFail;
	
	public CpuStats () {
		for (String name : InstructionSet.getInstance().getNameMap().keySet()) {
			isnCount.put(name, new int[1]);
		}
	}
	
	public String exceptionsString () {
		return arrayString(exceptions, n -> InstructionUtil.exceptionString(n));
	}
	
	public String irqsString () {
		return arrayString(irqs, n -> MaltaUtil.irqString(n));
	}
	
	public String interruptsString () {
		return arrayString(interrupts, n -> MaltaUtil.interruptString(n));
	}
	
	private static String arrayString (int[] a, Function<Integer, String> f) {
		Map<String, Integer> m = new TreeMap<>();
		for (int n = 0; n < a.length; n++) {
			if (a[n] != 0) {
				m.put(f.apply(n), a[n]);
			}
		}
		return m.toString();
	}
	
	public long durationNs () {
		return endTimeNs - startTimeNs - waitTimeNs;
	}
	
	public double durationS () {
		return seconds(endTimeNs - startTimeNs - waitTimeNs);
	}
	
	public double waitS () {
		return seconds(waitTimeNs);
	}
	
	public double totalS () {
		return seconds(endTimeNs - startTimeNs);
	}
	
	private static double seconds (long ns) {
		return ns / 1_000_000_000.0;
	}
	
	public List<?> instructionsByPop () {
		// instructions by pop
		return isnCount.entrySet()
				.stream()
				.filter(x -> x.getValue()[0] > 0)
				.sorted((x,y) -> y.getValue()[0] - x.getValue()[0])
				.map(x -> x.getKey() + "=" + x.getValue()[0])
				.collect(Collectors.toList());
	}
	
}