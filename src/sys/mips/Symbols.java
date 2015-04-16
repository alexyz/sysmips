package sys.mips;

import java.util.*;

public final class Symbols {
	
	private static class Symbol {
		public final String name;
		public final int size;
		
		public Symbol (String name, int size) {
			this.name = name;
			this.size = size;
		}
		
		@Override
		public String toString () {
			return name;
		}
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
		// zero extend address
		final long longAddr = addr & 0xffffffffL;
		final String addrStr = "0x" + Integer.toHexString(addr);
		
		Map.Entry<Long, Symbol> entry = map.floorEntry(new Long(longAddr));
		while (entry != null) {
			final long key = entry.getKey();
			final Symbol value = entry.getValue();
			final int offset = (int) (longAddr - key);
			if (offset < value.size) {
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
				// should warn if doing too many of these
				entry = map.lowerEntry(key);
			}
		}
		
		return addrStr;
	}
	
	public void put (final int addr, String name) {
		put(addr, name, Integer.MAX_VALUE);
	}
	
	public void put (final int addr, String name, int size) {
		if (size > 0) {
			// zero extend address
			final Long key = new Long(addr & 0xffffffffL);
			final Symbol prev = map.get(key);
			if (prev != null) {
				map.put(key, new Symbol(prev.name + "," + name, Math.max(prev.size, size)));
			} else {
				map.put(key, new Symbol(name, size));
			}
		} else {
			throw new IllegalArgumentException("invalid name " + name + " size " + size);
		}
	}
	
	@Override
	public String toString () {
		Map.Entry<Long, Symbol> e1 = map.firstEntry();
		Map.Entry<Long, Symbol> e2 = map.lastEntry();
		return String.format("Symbols[%d: %s - %s]", map.size(), e1, e2);
	}
}
