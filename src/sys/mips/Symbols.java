package sys.mips;

import java.util.*;

public final class Symbols {
	
	private final long[] addresses;
	private final String[] names;
	
	public Symbols (SortedMap<Long, String> syms) {
		this.addresses = new long[syms.size()];
		this.names = new String[syms.size()];
		
		int n = 0;
		for (Map.Entry<Long, String> e : syms.entrySet()) {
			addresses[n] = e.getKey();
			names[n] = e.getValue();
			n++;
		}
	}
	
	public final String getName (final int addr) {
		return getName(addr, true);
	}
	
	public final String getName (final int addr, final boolean includeAddr) {
		if (addresses.length > 0) {
			final long longaddr = addr & 0xffffffffL;
			int i = Arrays.binarySearch(addresses, longaddr);
			if (i < 0) {
				// one below insertion point
				i = -(i + 1) - 1;
			}
			// ignores first and last...
			if (i > 0 && i < addresses.length) {
				final long a = addresses[i];
				final String name = names[i];
				final long offset = longaddr - a;
				if (offset < 4096) {
					if (includeAddr) {
						return "0x" + Integer.toHexString(addr) + "<" + name + "+" + offset + ">";
					} else {
						if (offset != 0) {
							return name + "+" + offset;
						} else {
							return name;
						}
					}
				}
			}
		}
		return "0x" + Integer.toHexString(addr);
	}
	
	@Override
	public String toString () {
		int min = addresses.length > 0 ? (int) addresses[0] : 0;
		int max = addresses.length > 0 ? (int) addresses[addresses.length - 1] : 0;
		return String.format("Symbols[addrs=0x%x-0x%x]", min, max);
	}
}
