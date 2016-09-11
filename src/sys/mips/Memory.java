package sys.mips;

import java.io.PrintStream;

import sys.malta.Malta;
import sys.util.Logger;
import sys.util.Symbols;

/**
 * int[] backed memory as described in the MIPS32 4K processor core family
 * software user's manual.
 * <p>
 * although memory can be considered to be a device, it doesn't actually
 * implement the Device interface as it is a special case (referred to directly
 * by cpu).
 */
public final class Memory {
	
	private static final Logger log = new Logger("Memory");
	/**
	 * kseg0 - 0x80.. to 0x9f.. (512MB) - direct mapping to physical memory
	 * address 0
	 */
	private static final int KSEG0 = 0x8000_0000;
	/**
	 * kseg1 - 0xa0.. to 0xbf.. (512MB) - direct mapping to physical memory
	 * address 0, uncached, intercepted by malta board services
	 */
	private static final int KSEG1 = 0xa000_0000;
	/** kseg2 - 0xc0.. to 0xdf... (512MB) - mapped through tlb */
	private static final int KSEG2 = 0xc000_0000;
	/** kseg3 - 0xe0.. to 0xff... (512MB) - mapped through tlb */
	private static final int KSEG3 = 0xe000_0000;
	
	private static final int KSEG_MASK = 0x1fff_ffff;
	
	/** backing data as int (shift addr left 2 for index) */
	private final int[] data;
	private final int wordAddrXor;
	private final int halfWordAddrXor;
	private final boolean littleEndian;
	private final Entry[] entries = new Entry[16];
	//private final int[] pe = new int[16*3];
	private final Malta malta;
	
	private boolean kernelMode;
	private int asid;
	
	public Memory(int size, boolean littleEndian) {
		this.data = new int[size >>> 2];
		this.littleEndian = littleEndian;
		this.wordAddrXor = littleEndian ? 0 : 3;
		this.halfWordAddrXor = littleEndian ? 0 : 2;
		for (int n = 0; n < entries.length; n++) {
			entries[n] = new Entry();
		}
		this.malta = new Malta(KSEG1);
	}
	
	public Entry getEntry (int n) {
		return entries[n];
	}
	
	public int getAsid () {
		return asid;
	}
	
	public void setAsid (int asid) {
		this.asid = asid;
	}
	
	public void init (Cpu cpu) {
		log.println("init memory");
		Symbols symbols = cpu.getSymbols();
		symbols.put(KSEG0, "KSEG0");
		symbols.put(KSEG1, "KSEG1");
		symbols.put(KSEG2, "KSEG2");
		symbols.put(KSEG3, "KSEG3");
		malta.init(symbols);
		log.println("symbols=" + symbols);
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
	
	public final int loadWord (final int vaddr) {
		if ((vaddr & 3) == 0) {
			if (isSystem(vaddr)) {
				return malta.systemRead(vaddr, 4);
			} else {
				int i = translate(vaddr, false) >>> 2;
				return data[i];
			}
		} else {
			throw new CpuException(new CpuExceptionParams(CpuConstants.EX_ADDR_ERROR_LOAD, vaddr));
		}
	}
	
	public final void storeWord (final int vaddr, final int value) {
		if ((vaddr & 3) == 0) {
			if (isSystem(vaddr)) {
				malta.systemWrite(vaddr, 4, value);
			} else {
				int i = translate(vaddr, true) >>> 2;
				data[i] = value;
			}
		} else {
			throw new CpuException(new CpuExceptionParams(CpuConstants.EX_ADDR_ERROR_STORE, vaddr));
		}
	}
	
	public final short loadHalfWord (final int vaddr) {
		if ((vaddr & 1) == 0) {
			if (isSystem(vaddr)) {
				return (short) malta.systemRead(vaddr, 2);
			} else {
				final int i = translate(vaddr, false) >>> 2;
				final int w = data[i];
				// 0,2 -> 2,0 -> 16,0
				final int s = ((vaddr & 2) ^ halfWordAddrXor) << 3;
				return (short) (w >>> s);
			}
		} else {
			throw new CpuException(new CpuExceptionParams(CpuConstants.EX_ADDR_ERROR_LOAD, vaddr));
		}
	}
	
	public final void storeHalfWord (final int vaddr, final short value) {
		if ((vaddr & 1) == 0) {
			if (isSystem(vaddr)) {
				malta.systemWrite(vaddr, 2, value);
			} else {
				final int i = translate(vaddr, true) >>> 2;
				final int w = data[i];
				// 0,2 -> 2,0 -> 16,0
				final int s = ((vaddr & 2) ^ halfWordAddrXor) << 3;
				final int andm = ~(0xffff << s);
				final int orm = (value & 0xffff) << s;
				data[i] = (w & andm) | orm;
			}
		} else {
			throw new CpuException(new CpuExceptionParams(CpuConstants.EX_ADDR_ERROR_STORE, vaddr));
		}
	}
	
	public final byte loadByte (final int vaddr) {
		if (isSystem(vaddr)) {
			return (byte) malta.systemRead(vaddr, 1);
		} else {
			final int i = translate(vaddr, false) >>> 2;
			final int w = data[i];
			// 0,1,2,3 xor 0 -> 0,1,2,3
			// 0,1,2,3 xor 3 -> 3,2,1,0
			// 0,1,2,3 -> 3,2,1,0 -> 24,16,8,0
			final int s = ((vaddr & 3) ^ wordAddrXor) << 3;
			return (byte) (w >>> s);
		}
	}
	
	public final void storeByte (final int vaddr, final byte value) {
		if (isSystem(vaddr)) {
			malta.systemWrite(vaddr, 1, value);
		} else {
			final int i = translate(vaddr, true) >>> 2;
			final int w = data[i];
			// if xor=0: 0,1,2,3 -> 0,8,16,24
			// if xor=3: 0,1,2,3 -> 3,2,1,0 -> 24,16,8,0
			final int s = ((vaddr & 3) ^ wordAddrXor) << 3;
			final int andm = ~(0xff << s);
			final int orm = (value & 0xff) << s;
			data[i] = (w & andm) | orm;
		}
	}
	
	/**
	 * translate virtual address to physical. store affects dirty bit if true
	 * and type of exception thrown if address is invalid.
	 */
	public final int translate (final int vaddr, final boolean store) {
		final boolean kernelMode = this.kernelMode;
		if (kernelMode && vaddr < KSEG2) {
			// kseg0/kseg1 (direct)
			return vaddr & KSEG_MASK;
		} else if (vaddr >= 0 || (kernelMode && vaddr >= KSEG2)) {
			// useg/kuseg/kseg2/kseg3 (translated, slow)
			return lookup(vaddr, store);
		} else {
			throw new RuntimeException("cannot translate kseg as user: " + Integer.toHexString(vaddr));
		}
	}
	
	private final boolean isSystem (final int vaddr) {
		return vaddr >= KSEG1 && vaddr < KSEG2 && kernelMode;
	}
	
	public final int probe (final int vpn2) {
		for (int n = 0; n < entries.length; n++) {
			Entry e = entries[n];
			if (e.virtualPageNumber2 == vpn2 && (e.addressSpaceId == asid || e.global)) {
				log.println("tlb probe = " + n);
				return n;
			}
		}
		log.println("tlb probe miss");
		return -1;
	}
	
	/**
	 * lookup virtual address using tlb
	 */
	private final int lookup (final int vaddr, final boolean store) {
		int i = lookup1(vaddr, store);
		// need to populate the packed entries...
		//int j = lookup2(vaddr, store);
		return i;
	}
	
	private final int lookup1 (final int vaddr, final boolean store) {	
		final int vpn2 = CpuFunctions.vpn2(vaddr);
		final int eo = CpuFunctions.evenodd(vaddr);
		boolean refill = true;
		
		// log.debug("lookup vaddr=" + Integer.toHexString(vaddr) + " asid=" +
		// Integer.toHexString(asid) + " vpn2=" + Integer.toHexString(vpn2));
		
		for (int n = 0; n < entries.length; n++) {
			Entry e = entries[n];
			// log.debug("entry[" + n + "]=" + e);
			
			if (e.virtualPageNumber2 == vpn2 && (e.addressSpaceId == asid || e.global)) {
				// log.debug("tlb hit");
				EntryData d = e.data[eo];
				if (!d.valid) {
					log.println("entry invalid (not a refill)...");
					refill = false;
					break;
				}
				
				if (store && !d.dirty) {
					// XXX set dirty if write? how does os know?
					d.dirty = true;
				}
				
				final int paddr = (d.physicalFrameNumber << 12) | (vaddr & 0xfff);
				// log.debug("translated " + Integer.toHexString(vaddr) + " to "
				// + Integer.toHexString(paddr));
				return paddr;
			}
		}
		
		log.println("tlb miss");
		
//		for (int n = 0; n < entries.length; n++) {
//			Entry e = entries[n];
//			log.println("entry[" + n + "]=" + e);
//		}
		
		// TODO also need to throw modified exception if page is read only...
		throw new CpuException(new CpuExceptionParams(store ? CpuConstants.EX_TLB_STORE : CpuConstants.EX_TLB_LOAD, vaddr, refill));
	}
	
	/** load boxed word, null if unmapped */
	public Integer loadWordSafe (final int paddr) {
		// hack to translate addr
		final int vaddr = paddr & KSEG_MASK;
		final int i = vaddr >>> 2;
		if (i >= 0 && i < data.length) {
			final int w = data[i];
			return Integer.valueOf(w);
			
		} else {
			return null;
		}
	}
	
	/** load boxed word, null if unmapped */
	public Long loadDoubleWordSafe (final int paddr) {
		final int i = paddr >>> 2;
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
		return String.format("Memory[size=%d le=%s]", data.length, littleEndian);
	}
}
