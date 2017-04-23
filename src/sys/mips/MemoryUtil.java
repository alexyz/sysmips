package sys.mips;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class MemoryUtil {
	
	/**
	 * useg - 0x00.. to 0x80.. (2GB) - translated through tlb
	 */
	public static final int USEG = 0;
	/**
	 * kseg0 - 0x80.. to 0x9f.. (512MB) - direct mapping to physical memory
	 * address 0
	 */
	public static final int KSEG0 = 0x8000_0000;
	/**
	 * kseg1 - 0xa0.. to 0xbf.. (512MB) - direct mapping to physical memory
	 * address 0, uncached, intercepted by malta board services
	 */
	public static final int KSEG1 = 0xa000_0000;
	/** kseg2 - 0xc0.. to 0xdf... (512MB) - mapped through tlb */
	public static final int KSEG2 = 0xc000_0000;
	/** kseg3 - 0xe0.. to 0xff... (512MB) - mapped through tlb */
	public static final int KSEG3 = 0xe000_0000;
	/** 512mb mask */
	public static final int KSEG_MASK = 0x1fff_ffff;
	
	/** round up to nearest word */
	public static final int nextWord (final int addr) {
		return (addr + 3) & ~3;
	}
	
	/** return pointer to first word after data */
	public static final int storeWords (final Memory mem, final int addr, final List<Integer> data) {
		System.out.println("memory store integers " + data.size());
		for (int n = 0; n < data.size(); n++) {
			final int a = addr + (n * 4);
			mem.storeWord(a, data.get(n));
		}
		return addr + (data.size() * 4);
	}
	
	/** return pointer to first word after data */
	public static final int storeWords (final Memory mem, final int addr, final int[] data) {
		System.out.println("memory store ints " + data.length);
		for (int n = 0; n < data.length; n++) {
			final int a = addr + (n * 4);
			mem.storeWord(a, data[n]);
		}
		return addr + (data.length * 4);
	}
	
	/** fill in addresses and trailing null, return pointer to first byte after data */
	public static final int storeStrings (final Memory mem, final int addr, final List<Integer> addresses, final List<String> values) {
		System.out.println("memory store strings " + values.size());
		int p = addr;
		for (String value : values) {
			addresses.add(p);
			p = storeString(mem, p, value);
		}
		// add trailing null
		addresses.add(0);
		return p;
	}
	
	public static final String loadString (final Memory mem, final int addr) {
		return loadString(mem, addr, Integer.MAX_VALUE);
	}
	
	public static final String loadString (final Memory mem, final int addr, int max) {
		StringBuilder sb = new StringBuilder();
		for (int n = 0; n < max; n++) {
			byte b = mem.loadByte(addr+n);
			if (b != 0) {
				if (b >= 32 && b <= 127) {
					sb.append((char)b);
				} else {
					sb.append("{" + Integer.toHexString(b & 0xff) + "}");
				}
			} else {
				break;
			}
		}
		return sb.toString();
	}
	
	/** store null terminated string, return pointer to first byte after data */
	public static final int storeString (final Memory mem, final int addr, String value) {
		System.out.println("store string " + Integer.toHexString(addr) + ": " + value);
		return storeBytes(mem, addr, (value + "\0").getBytes(StandardCharsets.US_ASCII));
	}
	
	/** return pointer to first byte after data */
	public static final int storeBytes (final Memory mem, final int addr, final byte[] data) {
		//System.out.println("memory store bytes " + data.length);
		if (data.length > 0) {
			if ((addr & 3) == 0 && (data.length & 3) == 0) {
				storeBytesAsWords(mem, addr, data, 0, data.length);
				
			} else {
				System.out.println("store " + data.length + " bytes as bytes");
				// slow...
				for (int n = 0; n < data.length; n++) {
					mem.storeByte(addr + n, data[n]);
				}
			}
			return addr + data.length;
		} else {
			throw new IllegalArgumentException("zero length");
		}
	}
	
	private static void storeBytesAsWords (final Memory mem, final int addr, byte[] data, int offset, int len) {
		System.out.println("memory store bytes as words " + len);
		if ((addr & 3) == 0 && (len & 3) == 0) {
			for (int n = offset; n < (offset + len); n += 4) {
				final int b1 = data[n] & 0xff;
				final int b2 = data[n + 1] & 0xff;
				final int b3 = data[n + 2] & 0xff;
				final int b4 = data[n + 3] & 0xff;
				int w;
				if (mem.isLittleEndian()) {
					w = (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
				} else {
					w = (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
				}
				mem.storeWord(addr + n, w);
			}
		} else {
			throw new IllegalArgumentException("unaligned");
		}
	}
	
	/** convert index out of bounds exception to virtual address (assuming kseg0) */
	public static int toAddr (Exception e) {
		if (e instanceof ArrayIndexOutOfBoundsException) {
			try {
				return KSEG0 + (Integer.parseInt(e.getMessage()) << 2);
			} catch (Exception e2) {
				//
			}
		}
		return 0;
	}
	
	
	private MemoryUtil () {
		//
	}
}
