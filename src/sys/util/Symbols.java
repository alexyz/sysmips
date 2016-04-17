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
	private final TreeMap<String, Long> reverseMap = new TreeMap<>();
	
	public Symbols () {
		//
	}
	
	public final Collection<Symbol> getSymbols() {
		return Collections.unmodifiableCollection(map.values());
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
	
	public int getAddr (final String name) {
		return reverseMap.get(name).intValue();
	}
	
	public void put (final int addr, final String name) {
		put(addr, name, Integer.MAX_VALUE);
	}
	
	public void put (final int addr, final String name, final int size) {
		if (addr != 0 && name != null && name.length() > 0 && size > 0) {
			// zero extend address
			final Long key = new Long(addr & 0xffff_ffffL);
			final Symbol prev = map.get(key);
			if (prev != null && !prev.name.equals(name)) {
				map.put(key, new Symbol(addr, prev.name + "," + name, Math.max(prev.size, size)));
			} else {
				map.put(key, new Symbol(addr, name, size));
			}
			reverseMap.put(name, key);
		}
	}
	
	public void init(final Class<?> c, final String prefix, final String prefixReplacement, final int addr, final int size) {
		init(c, prefix, prefixReplacement, addr, size, 1);
	}
	
	public void init(final Class<?> c, final String prefix, final String prefixReplacement, final int addr, final int size, final int offsetMul) {
		boolean hasPut = false;
		for (final Field f : c.getFields()) {
			String name = f.getName();
			if (name.startsWith(prefix)) {
				final int m = f.getModifiers();
				if (Modifier.isPublic(m) && Modifier.isStatic(m) && Modifier.isFinal(m) && f.getType().isAssignableFrom(int.class)) {
					try {
						if (prefixReplacement != null) {
							name = prefixReplacement + name.substring(prefix.length());
						}
						put(addr + (f.getInt(null) * offsetMul), name, size);
						hasPut = true;
					} catch (final Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		if (!hasPut) {
			throw new RuntimeException("no symbols found");
		}
	}
	
	@Override
	public String toString () {
		final Map.Entry<Long, Symbol> e1 = map.firstEntry();
		final Map.Entry<Long, Symbol> e2 = map.lastEntry();
		return String.format("Symbols[%d: %s - %s]", map.size(), e1, e2);
	}
}
