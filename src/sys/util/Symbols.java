package sys.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import sys.mips.Cpu;

public final class Symbols {
	
	public static Symbols getInstance() {
		return Cpu.getInstance().getMemory().getSymbols();
	}
	
	// needs to be long so it can naturally sort
	private final TreeMap<Long, Symbol> map = new TreeMap<>();
	private final TreeMap<String, Integer> revmap = new TreeMap<>();
	
	public Symbols () {
		//
	}
	
	/** get name, no address or offset */
	public final String getName (final int addr) {
		return getName(addr, false, false);
	}
	
	/** get name with offset */
	public final String getNameOffset (final int addr) {
		return getName(addr, false, true);
	}
	
	/** get name with address and offset */
	public final String getNameAddrOffset (final int addr) {
		return getName(addr, true, true);
	}
	
	private final String getName (final int addr, final boolean includeAddr, final boolean includeOffset) {
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
					if (includeOffset && offset != 0) {
						return addrStr + "<" + value.name + "+0x" + Integer.toHexString(offset) + ">";
					} else {
						return addrStr + "<" + value.name + ">";
					}
				} else {
					if (includeOffset && offset != 0) {
						return value.name + "+0x" + Integer.toHexString(offset);
					} else {
						return value.name;
					}
				}
			} else {
				// XXX should warn if doing too many of these
				entry = map.lowerEntry(key);
			}
		}
		
		return addrStr;
	}
	
	public int getAddr (String name) {
		return revmap.get(name);
	}
	
	public void put (final int addr, String name) {
		put(addr, name, Integer.MAX_VALUE);
	}
	
	public void put (final int addr, String name, int size) {
		if (addr != 0 && name != null && name.length() > 0 && size > 0) {
			// zero extend address
			final Long key = new Long(addr & 0xffff_ffffL);
			final Symbol prev = map.get(key);
			if (prev != null && !prev.name.equals(name)) {
				map.put(key, new Symbol(prev.name + "," + name, Math.max(prev.size, size)));
			} else {
				map.put(key, new Symbol(name, size));
			}
			revmap.put(name, addr);
		}
	}
	
	public void init(Class<?> c, String prefix, String rep, int offset, int size) {
		for (Field f : c.getFields()) {
			String name = f.getName();
			if (name.startsWith(prefix)) {
				final int m = f.getModifiers();
				if (Modifier.isPublic(m) && Modifier.isStatic(m) && Modifier.isFinal(m) && f.getType().isAssignableFrom(int.class)) {
					try {
						if (rep != null) {
							name = rep + name.substring(prefix.length());
						}
						put(offset + f.getInt(null), name, size);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}
	
	@Override
	public String toString () {
		Map.Entry<Long, Symbol> e1 = map.firstEntry();
		Map.Entry<Long, Symbol> e2 = map.lastEntry();
		return String.format("Symbols[%d: %s - %s]", map.size(), e1, e2);
	}
}
