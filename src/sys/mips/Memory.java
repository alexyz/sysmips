package sys.mips;

public final class Memory {
	
	/** 1mb page size (divided by 4) */
	private static final int PS = 0x40000;
	
	public static int[] toInt (byte[] bytes) {
		if ((bytes.length & 3) != 0) {
			throw new RuntimeException();
		}
		int[] words = new int[bytes.length >> 2];
		int w = 0;
		// a(0) b(1) c(2) d(3)
		for (int n = 0; n < bytes.length; n += 4) {
			final int b1 = bytes[n] & 0xff;
			final int b2 = bytes[n + 1] & 0xff;
			final int b3 = bytes[n + 2] & 0xff;
			final int b4 = bytes[n + 3] & 0xff;
			words[w++] = (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
		}
		return words;
	}
	
	private static int pageIndex (int addr) {
		return addr >>> 20;
	}
	
	private static int wordIndex (int addr) {
		return (addr & 0xfffff) >> 2;
	}
	
	private final int[][] pages = new int[0x1000][];
	private final int system = 0xa0000000;
	
	private Malta malta;
	
	public Memory () {
		// 16mb for the kernel
		for (int n = 0; n < 16; n++) {
			pages[0x800 + n] = new int[PS];
		}
		// 12mb for the system controller
		for (int n = 0; n < 12; n++) {
			// malta says 1f0, but linux adds a00 for some reason
			pages[0xbf0 + n] = new int[PS];
		}
	}
	
	public int getSystem () {
		return system;
	}
	
	public final void setMalta (Malta malta) {
		this.malta = malta;
	}
	
	public final void storeBytes (final int addr, final byte[] data) {
		storeWords(addr, toInt(data));
	}
	
	public final int loadWordUnchecked (final int addr) {
		final int[] page = pages[pageIndex(addr)];
		if (page != null) {
			return page[wordIndex(addr)];
		} else {
			System.out.println("invalid address " + Integer.toHexString(addr));
			return 0;
		}
	}
	
	public final int loadWord (final int addr) {
		if ((addr & 3) == 0) {
			return pages[pageIndex(addr)][wordIndex(addr)];
		} else {
			throw new IllegalArgumentException(Integer.toHexString(addr));
		}
	}
	
	public final void storeWord (final int addr, final int word) {
		if ((addr & 3) == 0) {
			pages[pageIndex(addr)][wordIndex(addr)] = word;
			if (addr >= system) {
				malta.update(addr);
			}
		} else {
			throw new IllegalArgumentException(Integer.toHexString(addr));
		}
	}
	
	public final byte loadByte (final int addr) {
		int w = pages[pageIndex(addr)][wordIndex(addr)];
		// 0,1,2,3 -> 3,2,1,0 -> 24,16,8,0
		int s = (3 - (addr & 3)) << 3;
		return (byte) (w >>> s);
	}
	
	public final void storeByte (final int addr, final byte b) {
		final int i = wordIndex(addr);
		// 0,1,2,3 -> 3,2,1,0 -> 24,16,8,0
		final int s = (3 - (addr & 3)) << 3;
		final int andm = ~(0xff << s);
		final int orm = (b & 0xff) << s;
		int[] page = pages[pageIndex(addr)];
		page[i] = (page[i] & andm) | orm;
	}
	
	public final short loadHalfWord (final int addr) {
		if ((addr & 1) == 0) {
			final int i = wordIndex(addr);
			// 0,2 -> 2,0 -> 16,0
			final int s = (2 - (addr & 2)) << 3;
			return (short) (pages[pageIndex(addr)][i] >>> s);
			
		} else {
			throw new IllegalArgumentException(Integer.toHexString(addr));
		}
	}
	
	public final void storeHalfWord (final int addr, final short hw) {
		if ((addr & 1) == 0) {
			final int i = wordIndex(addr);
			// 0,2 -> 2,0 -> 16,0
			final int s = (2 - (addr & 2)) << 3;
			final int andm = ~(0xffff << s);
			final int orm = (hw & 0xffff) << s;
			int[] page = pages[pageIndex(addr)];
			page[i] = (page[i] & andm) | orm;
		} else {
			throw new IllegalArgumentException(Integer.toHexString(addr));
		}
	}
	
	public final void storeWords (final int addr, final int[] data) {
		System.out.println("int mem store ints " + data.length);
		if ((addr & 3) == 0) {
			for (int n = 0; n < data.length; n++) {
				final int a = addr + (n * 4);
				pages[pageIndex(a)][wordIndex(a)] = data[n];
			}
		} else {
			throw new IllegalArgumentException(Integer.toHexString(addr));
		}
	}
	
	@Override
	public String toString () {
		int count = 0;
		for (int n = 0; n < pages.length; n++) {
			if (pages[n] != null) {
				count++;
			}
		}
		return String.format("IntMemory[pages=%d]", count);
	}
}
