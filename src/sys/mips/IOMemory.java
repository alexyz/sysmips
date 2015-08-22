package sys.mips;

import java.util.HashMap;
import java.util.Map;

public class IOMemory {
	
	private final Map<Integer, int[]> ports = new HashMap<>();
	
	public void putByte (int addr, int data) {
		put(addr, 1, data);
	}
	
	public void putWord (int addr, int data) {
		put(addr, 4, data);
	}
	
	public int[] put (int addr, int size, int data) {
		return ports.put(Integer.valueOf(addr), new int[] { data, size });
	}
	
	public int getByte (int addr) {
		return get(addr, 1);
	}
	
	public int getWord (int addr) {
		return get(addr, 4);
	}
	
	public int get (int addr, int size) {
		int[] v = ports.get(Integer.valueOf(addr));
		if (v == null) {
			throw new RuntimeException("unmapped io port " + Integer.toHexString(addr));
		}
		if (v[1] == size) {
			return v[0];
		}
		throw new RuntimeException();
	}
	
}
