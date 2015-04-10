package sys.mips;

import java.util.*;

public final class Symbols {
	
	private static long toLongAddr(int addr) {
		return addr & 0xffffffffL;
	}
	
	// needs to be long so it can naturally sort
	private final TreeMap<Long,String> map = new TreeMap<>();
	
	public Symbols () {
		//
	}
	
	public final String getName (final int addr) {
		return getName(addr, true);
	}
	
	public final String getName (final int addr, final boolean includeAddr) {
		final long longAddr = toLongAddr(addr);
		final Map.Entry<Long, String> entry = map.floorEntry(new Long(longAddr));
		if (entry != null) {
			final long key = entry.getKey();
			final String value = entry.getValue();
			final int offset = (int) (longAddr - key);
			if (includeAddr) {
				if (offset != 0) {
					return "0x" + Integer.toHexString(addr) + "<" + value + "+0x" + Integer.toHexString(offset) + ">";
				} else {
					return "0x" + Integer.toHexString(addr) + "<" + value + ">";
				}
			} else {
				if (offset != 0) {
					return value + "+0x" + Integer.toHexString(offset);
				} else {
					return value;
				}
			}
		}
		return "0x" + Integer.toHexString(addr);
	}
	
	public void put(final int addr, String name) {
		final Long key = new Long(toLongAddr(addr));
		final String oldname = map.get(key);
		if (oldname != null) {
			name = oldname + "," + name;
		}
		map.put(key, name);
	}
	
	@Override
	public String toString () {
		Map.Entry<Long, String> e1 = map.firstEntry();
		Map.Entry<Long, String> e2 = map.lastEntry();
		return String.format("Symbols[%d: %s - %s]", map.size(), e1, e2);
	}
}
