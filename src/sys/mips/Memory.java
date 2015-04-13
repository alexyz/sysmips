package sys.mips;

import java.io.PrintStream;

public final class Memory {
	
	/** 1mb page size (divided by 4) */
	private static final int PAGELEN = 0x40000;
	
	/** return index of 1mb page */
	private static int pageIndex (int addr) {
		return addr >>> 20;
	}
	
	/** return index of word within 1mb page */
	private static int wordIndex (int addr) {
		return (addr & 0xfffff) >> 2;
	}
	
	private final int[][] pages = new int[0x1000][];
	private final Symbols symbols = new Symbols();
	
	private int system;
	private SystemListener systemListener;
	
	public Memory () {
		//
	}
	
	public Symbols getSymbols () {
		return symbols;
	}
	
	public void setSystem (int system) {
		this.system = system;
	}
	
	public void setSystemListener (SystemListener systemListener) {
		this.systemListener = systemListener;
	}
	
	/** create 1mb page, must be aligned */
	public void initPage (int addr) {
		if ((addr & 0xfffff) == 0) {
			final int[] page = pages[pageIndex(addr)];
			if (page == null) {
				pages[pageIndex(addr)] = new int[PAGELEN];
			} else {
				throw new IllegalArgumentException("existing " + Integer.toHexString(addr));
			}
		} else {
			throw new IllegalArgumentException("unaligned " + Integer.toHexString(addr));
		}
	}
	
	public final byte loadByte (final int addr) {
		final int[] page = pages[pageIndex(addr)];
		if (page != null) {
			final int w = page[wordIndex(addr)];
			if (addr > system) {
				fireSystemRead(addr, w);
			}
			// 0,1,2,3 -> 3,2,1,0 -> 24,16,8,0
			final int s = (3 - (addr & 3)) << 3;
			return (byte) (w >>> s);
		} else {
			throw new IllegalArgumentException("load unmapped " + Integer.toHexString(addr));
		}
	}
	
	public final void storeByte (final int addr, final byte b) {
		storeByteImpl(addr, b);
		if (addr >= system) {
			fireSystemWrite(addr, b);
		}
	}

	/** store byte into system area, no system write event */
	public void storeByteSystem (final int systemAddr, final byte b) {
		storeByteImpl(systemAddr + system, b);
	}
	
	private final void storeByteImpl (final int addr, final byte b) {
		// 0,1,2,3 -> 3,2,1,0 -> 24,16,8,0
		final int[] page = pages[pageIndex(addr)];
		if (page != null) {
			final int i = wordIndex(addr);
			final int s = (3 - (addr & 3)) << 3;
			final int andm = ~(0xff << s);
			final int orm = (b & 0xff) << s;
			page[i] = (page[i] & andm) | orm;
		} else {
			throw new IllegalArgumentException("store unmapped " + Integer.toHexString(addr));
		}
	}
	
	public final short loadHalfWord (final int addr) {
		if ((addr & 1) == 0) {
			final int[] page = pages[pageIndex(addr)];
			if (page != null) {
				final int i = wordIndex(addr);
				final int w = page[i];
				if (addr > system) {
					fireSystemRead(addr, w);
				}
				// 0,2 -> 2,0 -> 16,0
				final int s = (2 - (addr & 2)) << 3;
				return (short) (w >>> s);
			} else {
				throw new IllegalArgumentException("load unmapped " + Integer.toHexString(addr));
			}
		} else {
			throw new IllegalArgumentException("load unaligned " + Integer.toHexString(addr));
		}
	}
	
	public final void storeHalfWord (final int addr, final short hw) {
		if ((addr & 1) == 0) {
			final int[] page = pages[pageIndex(addr)];
			if (page != null) {
				// 0,2 -> 2,0 -> 16,0
				final int s = (2 - (addr & 2)) << 3;
				final int andm = ~(0xffff << s);
				final int orm = (hw & 0xffff) << s;
				final int i = wordIndex(addr);
				page[i] = (page[i] & andm) | orm;
				if (addr >= system) {
					fireSystemWrite(addr, hw);
				}
			} else {
				throw new IllegalArgumentException("store unmapped " + Integer.toHexString(addr));
			}
		} else {
			throw new IllegalArgumentException("store unaligned " + Integer.toHexString(addr));
		}
	}
	
	public final int loadWord (final int addr) {
		int w = loadWordImpl(addr);
		if (addr > system) {
			fireSystemRead(addr, w);
		}
		return w;
	}

	/** load word from system area, doesn't call system listener */
	public final int loadWordSystem (final int addr) {
		return pages[pageIndex(addr + system)][wordIndex(addr)];
	}
	
	private final int loadWordImpl (final int addr) {
		if ((addr & 3) == 0) {
			final int[] page = pages[pageIndex(addr)];
			if (page != null) {
				return page[wordIndex(addr)];
			} else {
				throw new IllegalArgumentException("load unmapped " + Integer.toHexString(addr));
			}
		} else {
			throw new IllegalArgumentException("load unaligned " + Integer.toHexString(addr));
		}
	}
	
	/** load boxed word, null if unmapped */
	public final Integer loadWordSafe (final int addr) {
		final int[] page = pages[pageIndex(addr)];
		if (page != null) {
			return Integer.valueOf(page[wordIndex(addr)]);
		} else {
			return null;
		}
	}
	
	public final void storeWord (final int addr, final int word) {
		storeWordImpl(addr, word);
		if (addr >= system) {
			fireSystemWrite(addr, word);
		}
	}

	/** store word into system area, unchecked, doesn't call system listener */
	public void storeWordSystem (final int systemAddr, final int word) {
		storeWordImpl(systemAddr + system, word);
	}
	
	private final void storeWordImpl (final int addr, final int word) {
		if ((addr & 3) == 0) {
			final int[] page = pages[pageIndex(addr)];
			if (page != null) {
				page[wordIndex(addr)] = word;
			} else {
				throw new IllegalArgumentException("store unmapped " + Integer.toHexString(addr));
			}
		} else {
			throw new IllegalArgumentException("store unaligned " + Integer.toHexString(addr));
		}
	}
	
	public void print (PrintStream ps) {
		ps.println("memory map");
		for (int n = 0; n < pages.length; n++) {
			final int[] page = pages[n];
			if (page != null) {
				float c = 0;
				for (int i = 0; i < page.length; i++) {
					if (page[i] != 0) {
						c++;
					}
				}
				ps.println("  addr 0x" + Integer.toHexString(n * 0x100000) + " usage " + (c / 0x100000));
			}
		}
	}

	private void fireSystemRead (int addr, int value) {
		if (systemListener != null) {
			systemListener.systemRead(addr - system, value);
		}
	}
	
	private void fireSystemWrite (int addr, int value) {
		if (systemListener != null) {
			systemListener.systemWrite(addr - system, value);
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
		return String.format("Memory[pages=%d symbols=%s]", count, symbols);
	}
}
