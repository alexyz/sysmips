package sys.mips;

import java.util.HashMap;
import java.util.Map;

public class IOMemory {
	
	private final Map<Integer, int[]> ports = new HashMap<>();
	
	public void putByte (int addr, byte data) {
		put(addr, data, 1);
	}
	
	public void putWord (int addr, int data) {
		put(addr, data, 4);
	}
	
	public int[] put (int addr, int data, int size) {
		final Integer addrObj = Integer.valueOf(addr);
		int [] v = ports.get(addrObj);
		if (v == null) {
			ports.put(addrObj, v = new int[2]);
		}
		v[0] = data;
		v[1] = size;
		return v;
	}
	
	public byte getByte (int addr) {
		return (byte) get(addr, 1);
	}
	
	public int getWord (int addr) {
		return get(addr, 4);
	}
	
	public int get (int addr, int size) {
		int[] v = ports.get(Integer.valueOf(addr));
		if (v == null) {
			throw new RuntimeException("get unmapped io port " + Integer.toHexString(addr));
		}
		if (v[1] == size) {
			return v[0];
		} else {
			throw new RuntimeException("get io port " + Integer.toHexString(addr) + " expected size " + v[1] + " actual size " + size);
		}
	}
	
}
