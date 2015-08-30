package sys.mips;

import java.lang.reflect.*;

import static sys.ByteOrder.*;

/**
 * this class kind-of represents the northbridge, but doesn't really handle any
 * device mapping
 */
public class GT implements Device {
	
	/** SCS[1:0]* Low Decode Address */
	public static final int GT_SCS10LD = 0x008;
	/** SCS[1:0]* High Decode Address */
	public static final int GT_SCS10HD = 0x010;
	/** SCS[3:2]* Low Decode Address */
	public static final int GT_SCS32LD = 0x018;
	/** SCS[3:2]* High Decode Address */
	public static final int GT_SCS32HD = 0x020;
	/** CS[2:0]* Low Decode Address */
	public static final int GT_CS20LD = 0x028;
	/** CS[2:0]* High Decode Address */
	public static final int GT_CS20HD = 0x030;
	/** CS[3]* & Boot CS* Low Decode Address */
	public static final int GT_CS3BOOTLD = 0x038;
	/** CS[3]* & Boot CS* High Decode Address */
	public static final int GT_CS3BOOTHD = 0x040;
	/** PCI_0 I/O Low Decode Address */
	// setting this affects ioremap
	public static final int GT_PCI0IOLD = 0x048;
	/** PCI_0 I/O High Decode Address */
	public static final int GT_PCI0IOHD = 0x050;
	/** PCI_0 Memory 0 Low Decode Address */
	public static final int GT_PCI0M0LD = 0x058;
	/** PCI_0 Memory 0 High Decode Address */
	public static final int GT_PCI0M0HD = 0x060;
	
	public static final int GT_ISD = 0x068;
	
	/** PCI_0 Memory 1 Low Decode Address */
	public static final int GT_PCI0M1LD = 0x080;
	/** PCI_0 Memory 1 High Decode Address */
	public static final int GT_PCI0M1HD = 0x088;
	/** PCI_1 I/O Low Decode Address */
	public static final int GT_PCI1IOLD = 0x090;
	/** PCI_1 I/O High Decode Address */
	public static final int GT_PCI1IOHD = 0x098;
	/** PCI_1 Memory 0 Low Decode Address */
	public static final int GT_PCI1M0LD = 0x0a0;
	/** PCI_1 Memory 0 High Decode Address */
	public static final int GT_PCI1M0HD = 0x0a8;
	/** PCI_1 Memory 1 Low Decode Address */
	public static final int GT_PCI1M1LD = 0x0b0;
	/** PCI_1 Memory 1 High Decode Address */
	public static final int GT_PCI1M1HD = 0x0b8;
	
	/** SCS[1:0]* Address Remap */
	public static final int GT_SCS10AR = 0x0d0;
	/** SCS[3:2]* Address Remap */
	public static final int GT_SCS32AR = 0x0d8;
	/** CS[2:0]* Address Remap */
	public static final int GT_CS20R = 0x0e0;
	/** CS[3]* & Boot CS* Address Remap */
	public static final int GT_CS3BOOTR = 0x0e8;
	
	/** PCI_0 IO Address Remap */
	public static final int GT_PCI0IOREMAP = 0x0f0;
	/** PCI_0 Memory 0 Address Remap */
	public static final int GT_PCI0M0REMAP = 0x0f8;
	/** PCI_0 Memory 1 Address Remap */
	public static final int GT_PCI0M1REMAP = 0x100;
	/** PCI_1 IO Address Remap */
	public static final int GT_PCI1IOREMAP = 0x108;
	/** PCI_1 Memory 0 Address Remap */
	public static final int GT_PCI1M0REMAP = 0x110;
	/** PCI_1 Memory 1 Address Remap */
	public static final int GT_PCI1M1REMAP = 0x118;
	
	/** pci 0 command register */
	public static final int GT_PCI0_CMD = 0xc00;
	public static final int GT_PCI1_CFGADDR = 0xcf0;
	public static final int GT_PCI1_CFGDATA = 0xcf4;
	/** PCI_0 Configuration Address */
	public static final int GT_PCI0_CFGADDR = 0xcf8;
	public static final int GT_PCI0_CFGDATA = 0xcfc;
	
	private final IOMemory iomem = new IOMemory();
	
	public GT () {
//		iomem.putw(GT_SCS10LD, 0x0);
//		iomem.putw(GT_SCS10HD, 0x7);
//		iomem.putw(GT_SCS10AR, 0x0);
//		
//		iomem.putw(GT_SCS32LD, 0x8);
//		iomem.putw(GT_SCS32HD, 0xf);
//		iomem.putw(GT_SCS32AR, 0x8);
//		
//		iomem.putw(GT_CS20LD, 0xe0);
//		iomem.putw(GT_CS20HD, 0x70);
//		iomem.putw(GT_CS20R, 0xe0);
//		
//		iomem.putw(GT_CS3BOOTLD, 0xf8);
//		iomem.putw(GT_CS3BOOTHD, 0x7f);
//		iomem.putw(GT_CS3BOOTR, 0xf8);
		
		iomem.putWord(GT_PCI0IOLD, 0x80);
		iomem.putWord(GT_PCI0IOHD, 0xf);
		iomem.putWord(GT_PCI0M0LD, 0x90);
		iomem.putWord(GT_PCI0M0HD, 0x1f);
		iomem.putWord(GT_PCI0M1LD, 0x790);
		iomem.putWord(GT_PCI0M1HD, 0x1f);
		iomem.putWord(GT_PCI0IOREMAP, 0x80);
		iomem.putWord(GT_PCI0M0REMAP, 0x90);
		iomem.putWord(GT_PCI0M1REMAP, 0x790);
		
//		iomem.putw(GT_PCI1IOLD, 0x100);
//		iomem.putw(GT_PCI1IOHD, 0xf);
//		iomem.putw(GT_PCI1M0LD, 0x110);
//		iomem.putw(GT_PCI1M0HD, 0x1f);
//		iomem.putw(GT_PCI1M1LD, 0x120);
//		iomem.putw(GT_PCI1M1HD, 0x2f);
//		iomem.putw(GT_PCI1IOREMAP, 0x100);
//		iomem.putw(GT_PCI1M0REMAP, 0x110);
//		iomem.putw(GT_PCI1M1REMAP, 0x120);
	}

	@Override
	public void init (Symbols sym, int offset) {
		System.out.println("init gt at " + Integer.toHexString(offset));
		
		for (Field f : GT.class.getFields()) {
			final int m = f.getModifiers();
			if (Modifier.isPublic(m) && Modifier.isStatic(m) && Modifier.isFinal(m)) {
				try {
					sym.put(offset + f.getInt(null), f.getName(), 4);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		
	}

	@Override
	public int systemRead (int addr, int size) {
		// swap due to an expected bug in GT
		return swap(iomem.get(addr, size));
	}
	
	@Override
	public void systemWrite (int addr, int value, int size) {
		CpuLogger log = Cpu.getInstance().getLog();
		// XXX swap here?
		value = swap(value);
		log.info("gt write " + Integer.toHexString(addr) + " <= " + Integer.toHexString(value));
		iomem.put(addr, value, size);
		
		switch (addr) {
			case GT_PCI0IOREMAP:
				log.info("set PCI 0 IO remap " + value);
				if (value != 0) {
					throw new RuntimeException("unknown remap");
				}
				break;
				
			case GT_PCI0_CMD:
				log.info("ignore PCI0 command " + value);
				break;
				
			case GT_PCI0_CFGADDR:
				setAddr(value);
				break;
			
			case GT_PCI0_CFGDATA:
				setData(value);
				break;
				
			default:
				throw new RuntimeException("invalid gt write");
		}
		
	}
	
	private void setAddr (final int cfgaddr) {
		CpuLogger log = Cpu.getInstance().getLog();
		// pci configuration space
		int en = (cfgaddr >>> 31) & 0x1;
		int bus = (cfgaddr >>> 16) & 0xff;
		int dev = (cfgaddr >>> 11) & 0x1f;
		int func = (cfgaddr >>> 8) & 0x7;
		int reg = (cfgaddr >>> 2) & 0x3f;
		log.info("set PCI0 config address %x en=%d bus=%d dev=%d func=%d reg=%d", cfgaddr, en, bus, dev, func, reg);
		
		if (bus == 0 && dev == 0 && func == 0) {
			iomem.putWord(GT_PCI0_CFGDATA, 0);
			
		} else {
			throw new RuntimeException("set unknown bus/device/function addr");
		}
	}
	
	private void setData (int value) {
		CpuLogger log = Cpu.getInstance().getLog();
		int cfgaddr = iomem.getWord(GT_PCI0_CFGADDR);
		int en = (cfgaddr >>> 31) & 0x1;
		int bus = (cfgaddr >>> 16) & 0xff;
		int dev = (cfgaddr >>> 11) & 0x1f;
		int func = (cfgaddr >>> 8) & 0x7;
		int reg = (cfgaddr >>> 2) & 0x3f;
		log.info("set PCI0 config data %x en=%d bus=%d dev=%d func=%d reg=%d", value, en, bus, dev, func, reg);
		
		if (bus == 0 && dev == 0 && func == 0) {
			log.info("set GT_PCI0_CFGADDR_CONFIGEN_BIT");
			
		} else {
			throw new RuntimeException("set unknown bus/device/function data");
		}
	}

}
