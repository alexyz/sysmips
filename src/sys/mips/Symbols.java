package sys.mips;

import java.util.*;

public final class Symbols {
	
	public static class Symbol {
		public final String name;
		public final int size;
		
		public Symbol (String name) {
			this(name, Integer.MAX_VALUE);
		}
		
		public Symbol (String name, int size) {
			this.name = name;
			this.size = size;
		}
	}
	
	private static long toLongAddr (int addr) {
		return addr & 0xffffffffL;
	}
	
	// needs to be long so it can naturally sort
	private final TreeMap<Long, Symbol> map = new TreeMap<>();
	
	public Symbols () {
		//
	}
	
	public final String getName (final int addr) {
		return getName(addr, true);
	}
	
	public final String getName (final int addr, final boolean includeAddr) {
		final long longAddr = toLongAddr(addr);
		final String addrStr = "0x" + Integer.toHexString(addr);
		
		Map.Entry<Long, Symbol> entry = map.floorEntry(new Long(longAddr));
		while (entry != null) {
			final long key = entry.getKey();
			final Symbol value = entry.getValue();
			final int offset = (int) (longAddr - key);
			if (offset <= value.size) {
				// found suitable symbol
				if (includeAddr) {
					if (offset != 0) {
						return addrStr + "<" + value.name + "+0x" + Integer.toHexString(offset) + ">";
					} else {
						return addrStr + "<" + value.name + ">";
					}
				} else {
					if (offset != 0) {
						return value.name + "+0x" + Integer.toHexString(offset);
					} else {
						return value.name;
					}
				}
			} else {
				entry = map.lowerEntry(key);
			}
		}
		
		return addrStr;
	}
	
	public void put (final int addr, String name) {
		put(addr, name, Integer.MAX_VALUE);
	}
	
	public void put (final int addr, String name, int size) {
		final Long key = new Long(toLongAddr(addr));
		final Symbol prev = map.get(key);
		if (prev != null) {
			map.put(key, new Symbol(prev.name + "," + name, Math.max(prev.size, size)));
		} else {
			map.put(key, new Symbol(name, size));
		}
	}
	
	@Override
	public String toString () {
		Map.Entry<Long, Symbol> e1 = map.firstEntry();
		Map.Entry<Long, Symbol> e2 = map.lastEntry();
		return String.format("Symbols[%d: %s - %s]", map.size(), e1, e2);
	}
}
