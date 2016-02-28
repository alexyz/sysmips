package sys.malta;

import sys.mips.Cpu;
import sys.util.Logger;
import sys.util.Symbols;

/**
 * malta device mapping and board specific functions as described in the malta
 * user's manual
 */
public class Malta implements Device {
	
	/** the on board memory (128mb max) */
	public static final int M_SDRAM = 0x0;
	public static final int M_UNCACHED_EX_H = 0x100;
	
	public static final int M_PCI1 = 0x0800_0000;
	// connected with value of [GT_PCI0IOLD]
	public static final int M_PIIX4 = 0x1000_0000;
	public static final int M_PCI2 = 0x1800_0000;
	public static final int M_GTBASE = 0x1be0_0000;
	public static final int M_CBUS = 0x1c00_0000;
	public static final int M_MONITORFLASH = 0x1e00_0000;
	public static final int M_RESERVED = 0x1e40_0000;
	public static final int M_DEVICES = 0x1f00_0000;
	public static final int M_DISPLAY = 0x1f00_0400;
	public static final int M_SCSPEC1 = 0x1f10_0010;
	public static final int M_BOOTROM = 0x1fc0_0000;
	public static final int M_REVISION = 0x1fc0_0010;
	public static final int M_SCSPEC2 = 0x1fd0_0010;
	
	private static final Logger log = new Logger("Malta");
	
	private final int baseAddr;
	/** the northbridge */
	private final GT gt;
	private final MaltaDisplay display;
	/** the southbridge */
	private final PIIX4 p4;
	
	public Malta (int baseAddr) {
		this.baseAddr = baseAddr;
		this.p4 = new PIIX4(baseAddr + M_PIIX4);
		this.gt = new GT(baseAddr + M_GTBASE);
		this.display = new MaltaDisplay(baseAddr + M_DISPLAY);
	}
	
	public void setIrq (int irq) {
		gt.setIrq(irq);
	}
	
	@Override
	public void init (Symbols sym) {
		log.println("init malta at " + Integer.toHexString(baseAddr));

		display.init(sym);
		p4.init(sym);
		gt.init(sym);
		
		sym.init(Malta.class, "M_", null, baseAddr, 4);
	}
	
	@Override
	public boolean isMapped (int addr) {
		// root device, everything is mapped
		throw new RuntimeException();
	}
	
	@Override
	public int systemRead (int addr, int size) {
		if (gt.isMapped(addr)) {
			return gt.systemRead(addr, size);
			
		} else if (p4.isMapped(addr)) {
			return p4.systemRead(addr, size);
			
		} else if (display.isMapped(addr)) {
			return display.systemRead(addr, size);
			
		} else {
			int offset = addr - baseAddr;
			switch (offset) {
				case M_REVISION:
					return 1;
				default:
					throw new RuntimeException("unknown system read " + Cpu.getInstance().getMemory().getSymbols().getNameAddrOffset(addr) + " size " + size);
			}
		}
	}
	
	@Override
	public void systemWrite (final int addr, final int value, int size) {
		if (gt.isMapped(addr)) {
			gt.systemWrite(addr, value, size);
			
		} else if (p4.isMapped(addr)) {
			p4.systemWrite(addr, value, size);
			
		} else if (addr >= baseAddr + M_UNCACHED_EX_H && addr < baseAddr + M_UNCACHED_EX_H + 0x100) {
			// TODO this should map straight through to memory
			//log.println("set uncached exception handler " + Symbols.getInstance().getNameOffset(baseAddr + addr) + " <= " + Integer.toHexString(value));
			
		} else if (display.isMapped(addr)) {
			display.systemWrite(addr, value, size);
			
		} else {
			int offset = addr - baseAddr;
			switch (offset) {
				default:
					throw new RuntimeException("unknown system write " + Symbols.getInstance().getNameAddrOffset(addr) + " <= " + Integer.toHexString(value));
			}
		}
		
	}

}
