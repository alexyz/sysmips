package sys.mips;

import java.io.PrintStream;

public final class Memory {
	
	private static final int KSEG0 = 0x8000_0000;
	private static final int KSEG1 = 0xa000_0000;
	private static final int KSEG2 = 0xc000_0000;
	private static final int KSEG3 = 0xe000_0000;
	private static final int KSEG_MASK = 0x1fff_ffff;
	
	private final int[] data;
	private final Symbols symbols = new Symbols();
	private final int wordAddrXor;
	private final int halfWordAddrXor;
	private final boolean littleEndian;
	
	private Malta malta;
	private boolean kernelMode;
	/// int asid...
	
	public Memory (int size, boolean littleEndian) {
		this.data = new int[size >> 2];
		this.littleEndian = littleEndian;
		this.wordAddrXor = littleEndian ? 0 : 3;
		this.halfWordAddrXor = littleEndian ? 0 : 2;
	}
	
	public void init () {
		System.out.println("init memory");
		symbols.put(KSEG0, "KSEG0");
		symbols.put(KSEG1, "KSEG1");
		symbols.put(KSEG2, "KSEG2");
		symbols.put(KSEG3, "KSEG3");
		malta.init(symbols, KSEG1);
		System.out.println("symbols=" + symbols);
	}
	
	public boolean isLittleEndian () {
		return littleEndian;
	}
	
	public boolean isKernelMode () {
		return kernelMode;
	}
	
	public void setKernelMode (boolean kernelMode) {
		this.kernelMode = kernelMode;
	}
	
	public Malta getMalta () {
		return malta;
	}
	
	public void setMalta (Malta malta) {
		this.malta = malta;
	}
	
	public Symbols getSymbols () {
		return symbols;
	}
	
	public final int loadWord (final int vaddr) {
		if (isSystem(vaddr)) {
			return malta.systemRead(vaddr & KSEG_MASK, 4);
		} else {
			return loadWordImpl(translate(vaddr));
		}
	}
	
	public final void storeWord (final int vaddr, final int value) {
		if (isSystem(vaddr)) {
			malta.systemWrite(vaddr & KSEG_MASK, value, 4);
		} else {
			storeWordImpl(translate(vaddr), value);
		}
	}
	
	public final short loadHalfWord (final int vaddr) {
		if (isSystem(vaddr)) {
			return (short) malta.systemRead(vaddr & KSEG_MASK, 2);
		} else {
			return loadHalfWordImpl(translate(vaddr));
		}
	}
	
	public final void storeHalfWord (final int vaddr, final short value) {
		if (isSystem(vaddr)) {
			malta.systemWrite(vaddr & KSEG_MASK, value, 2);
		} else {
			storeHalfWordImpl(translate(vaddr), value);
		}
	}
	
	public final byte loadByte (final int vaddr) {
		if (isSystem(vaddr)) {
			return (byte) malta.systemRead(vaddr & KSEG_MASK, 1);
		} else {
			return loadByteImpl(translate(vaddr));
		}
	}
	
	public final void storeByte (final int vaddr, final byte value) {
		if (isSystem(vaddr)) {
			malta.systemWrite(vaddr & KSEG_MASK, value, 1);
		} else {
			storeByteImpl(translate(vaddr), value);
		}
	}
	
	/**
	 * translate virtual address to physical
	 */
	public final int translate (int vaddr) {
		if (vaddr >= 0) {
			// useg/kuseg
			return lookup(vaddr);
		} else if (kernelMode) {
			if (vaddr < KSEG1) {
				// kseg0 (direct)
				return vaddr & KSEG_MASK;
			} else if (vaddr < KSEG2) {
				// kseg1 (direct, system)
				throw new RuntimeException("cannot translate kseg1: " + Integer.toHexString(vaddr));
			} else {
				// kseg2,3
				return lookup(vaddr);
			}
		} else {
			throw new RuntimeException("cannot translate kseg as user: " + Integer.toHexString(vaddr));
		}
	}
	
	private final boolean isSystem (int vaddr) {
		return vaddr >= KSEG1 && vaddr < KSEG2 && kernelMode;
	}
	
	/**
	 * translate virtual address to physical
	 */
	private final int lookup (int vaddr) {
		throw new RuntimeException("unimplemented lookup " + Integer.toHexString(vaddr));
	}
	
	private final byte loadByteImpl (final int paddr) {
		try {
			final int i = paddr >> 2;
			final int w = data[i];
			// 0,1,2,3 xor 0 -> 0,1,2,3
			// 0,1,2,3 xor 3 -> 3,2,1,0
			// 0,1,2,3 -> 3,2,1,0 -> 24,16,8,0
			final int s = ((paddr & 3) ^ wordAddrXor) << 3;
			return (byte) (w >>> s);
			
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("load unmapped " + Integer.toHexString(paddr), e);
		}
	}
	
	private final void storeByteImpl (final int paddr, final byte b) {
		try {
			final int i = paddr >> 2;
			final int w = data[i];
			// if xor=0: 0,1,2,3 -> 0,8,16,24
			// if xor=3: 0,1,2,3 -> 3,2,1,0 -> 24,16,8,0
			final int s = ((paddr & 3) ^ wordAddrXor) << 3;
			final int andm = ~(0xff << s);
			final int orm = (b & 0xff) << s;
			data[i] = (w & andm) | orm;
			
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("load unmapped " + Integer.toHexString(paddr), e);
		}
	}
	
	private final short loadHalfWordImpl (final int paddr) {
		if ((paddr & 1) == 0) {
			try {
				final int i = paddr >> 2;
				final int w = data[i];
				// 0,2 -> 2,0 -> 16,0
				final int s = ((paddr & 2) ^ halfWordAddrXor) << 3;
				return (short) (w >>> s);
				
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new IllegalArgumentException("load unmapped " + Integer.toHexString(paddr), e);
			}
		} else {
			throw new IllegalArgumentException("load unaligned " + Integer.toHexString(paddr));
		}
	}
	
	private final void storeHalfWordImpl (final int paddr, final short hw) {
		if ((paddr & 1) == 0) {
			try {
				final int i = paddr >> 2;
				final int w = data[i];
				// 0,2 -> 2,0 -> 16,0
				final int s = ((paddr & 2) ^ halfWordAddrXor) << 3;
				final int andm = ~(0xffff << s);
				final int orm = (hw & 0xffff) << s;
				data[i] = (w & andm) | orm;
				
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new IllegalArgumentException("store unmapped " + Integer.toHexString(paddr), e);
			}
		} else {
			throw new IllegalArgumentException("store unaligned " + Integer.toHexString(paddr));
		}
	}
	
	private final int loadWordImpl (final int paddr) {
		if ((paddr & 3) == 0) {
			try {
				final int i = paddr >> 2;
				final int w = data[i];
				return w;
				
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new IllegalArgumentException("load unmapped " + Integer.toHexString(paddr), e);
			}
		} else {
			// cpu.execExn(ex_addr) - doesn't stop execution
			// throw new exn(ex_addr) - requires catch in run
			throw new IllegalArgumentException("load unaligned " + Integer.toHexString(paddr));
		}
	}
	
	private final void storeWordImpl (final int paddr, final int word) {
		if ((paddr & 3) == 0) {
			try {
				final int i = paddr >> 2;
				data[i] = word;
				
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new IllegalArgumentException("store unmapped " + Integer.toHexString(paddr), e);
			}
		} else {
			throw new IllegalArgumentException("store unaligned " + Integer.toHexString(paddr));
		}
	}
	
	/** load boxed word, null if unmapped */
	public Integer loadWordSafe (final int paddr) {
		final int i = paddr >> 2;
		if (i >= 0 && i < data.length) {
			final int w = data[i];
			return Integer.valueOf(w);
			
		} else {
			return null;
		}
	}
	
	/** load boxed word, null if unmapped */
	public Long loadDoubleWordSafe (final int paddr) {
		final int i = paddr >> 2;
		if (i >= 0 && i < data.length - 1) {
			final long w1 = data[i] & 0xffff_ffffL;
			final long w2 = data[i + 1] & 0xffff_ffffL;
			// XXX might need swap
			return Long.valueOf((w1 << 32) | w2);
			
		} else {
			return null;
		}
	}
	
	public void print (PrintStream ps) {
		ps.println("memory map");
		// for each 1mb block in words
		for (int j = 0; j < data.length; j += 0x40000) {
			float c = 0;
			for (int i = 0; i < 0x40000; i++) {
				if (data[j + i] != 0) {
					c++;
				}
			}
			ps.println("  addr 0x" + Integer.toHexString(j * 4) + " usage " + (c / 0x100000));
		}
	}
	
	@Override
	public String toString () {
		return String.format("Memory[size=%d le=%s sym]", data.length, littleEndian, symbols);
	}
}
