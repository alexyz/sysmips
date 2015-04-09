package sys.mips;

import java.util.*;

public final class Symbols {
	
	private final TreeMap<Long,String> map = new TreeMap<>();
	
	public Symbols () {
		//
	}
	
	public final String getName (final int addr) {
		return getName(addr, true);
	}
	
	public final String getName (final int addr, final boolean includeAddr) {
		final long longAddr = addr & 0xffffffffL;
		final Map.Entry<Long, String> entry = map.floorEntry(longAddr);
		if (entry != null) {
			final long key = entry.getKey();
			final String value = entry.getValue();
			final long offset = longAddr - key;
			if (includeAddr) {
				return "0x" + Integer.toHexString(addr) + "<" + value + "+" + offset + ">";
			} else {
				if (offset != 0) {
					return value + "+" + offset;
				} else {
					return value;
				}
			}
		}
		return "0x" + Integer.toHexString(addr);
	}
	
	public SortedMap<Long, String> getMap () {
		return map;
	}
	
	@Override
	public String toString () {
		Map.Entry<Long, String> e1 = map.firstEntry();
		Map.Entry<Long, String> e2 = map.lastEntry();
		return String.format("Symbols[%d: %s - %s]", map.size(), e1, e2);
	}
}
