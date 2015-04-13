package sys.mips;

import java.io.PrintStream;

public final class Memory {
	
	/** 1mb page size (divided by 4) */
	private static final int PAGELEN = 0x40000;
	
	public static int[] toInt (byte[] bytes) {
		if ((bytes.length & 3) != 0) {
			throw new RuntimeException();
		}
		final int[] words = new int[bytes.length >> 2];
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
	
	public final void storeBytes (final int addr, final byte[] data) {
		storeWords(addr, toInt(data));
	}
	
	public final int loadWord (final int addr) {
		if ((addr & 3) == 0) {
			final int[] page = pages[pageIndex(addr)];
			if (page != null) {
				final int w = page[wordIndex(addr)];
				if (addr > system) {
					fireSystemRead(addr, w);
				}
				return w;
			} else {
				throw new IllegalArgumentException("load unmapped " + Integer.toHexString(addr));
			}
		} else {
			throw new IllegalArgumentException("load unaligned " + Integer.toHexString(addr));
		}
	}
	
	/** load word from system area, doesn't call system listener */
	public final int loadWordSystem (final int addr) {
		return pages[pageIndex(addr + system)][wordIndex(addr)];
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
		if ((addr & 3) == 0) {
			final int[] page = pages[pageIndex(addr)];
			if (page != null) {
				page[wordIndex(addr)] = word;
				if (addr >= system) {
					fireSystemWrite(addr, word);
				}
			} else {
				throw new IllegalArgumentException("store unmapped " + Integer.toHexString(addr));
			}
		} else {
			throw new IllegalArgumentException("store unaligned " + Integer.toHexString(addr));
		}
	}
	
	/** store word into system area, doesn't call system listener */
	public void storeWordSystem (final int addr, final int word) {
		pages[pageIndex(addr + system)][wordIndex(addr)] = word;
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
		final int i = wordIndex(addr);
		// 0,1,2,3 -> 3,2,1,0 -> 24,16,8,0
		final int s = (3 - (addr & 3)) << 3;
		final int andm = ~(0xff << s);
		final int orm = (b & 0xff) << s;
		final int[] page = pages[pageIndex(addr)];
		if (page != null) {
			page[i] = (page[i] & andm) | orm;
			if (addr >= system) {
				fireSystemWrite(addr, b);
			}
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
	
	public final void storeWords (final int addr, final int[] data) {
		System.out.println("int mem store ints " + data.length);
		for (int n = 0; n < data.length; n++) {
			final int a = addr + (n * 4);
			storeWord(a, data[n]);
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
