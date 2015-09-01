package sys.malta;

import java.lang.reflect.*;

import sys.mips.CpuLogger;
import sys.util.Symbols;

import static sys.util.ByteOrder.*;

/**
 * this class kind-of represents the northbridge, but doesn't really handle any
 * device mapping
 */
public class GT implements Device {
	
	/** SCS[1:0]* Low Decode Address */
	public static final int GT_SCS10LD = 0x8;
	/** SCS[1:0]* High Decode Address */
	public static final int GT_SCS10HD = 0x10;
	/** SCS[3:2]* Low Decode Address */
	public static final int GT_SCS32LD = 0x18;
	/** SCS[3:2]* High Decode Address */
	public static final int GT_SCS32HD = 0x20;
	/** CS[2:0]* Low Decode Address */
	public static final int GT_CS20LD = 0x28;
	/** CS[2:0]* High Decode Address */
	public static final int GT_CS20HD = 0x30;
	/** CS[3]* & Boot CS* Low Decode Address */
	public static final int GT_CS3BOOTLD = 0x38;
	/** CS[3]* & Boot CS* High Decode Address */
	public static final int GT_CS3BOOTHD = 0x40;
	/** PCI_0 I/O Low Decode Address */
	// setting this affects ioremap
	public static final int GT_PCI0IOLD = 0x48;
	/** PCI_0 I/O High Decode Address */
	public static final int GT_PCI0IOHD = 0x50;
	/** PCI_0 Memory 0 Low Decode Address */
	public static final int GT_PCI0M0LD = 0x58;
	/** PCI_0 Memory 0 High Decode Address */
	public static final int GT_PCI0M0HD = 0x60;
	/** Internal Space Decode */
	public static final int GT_ISD = 0x68;
	/** PCI_0 Memory 1 Low Decode Address */
	public static final int GT_PCI0M1LD = 0x80;
	/** PCI_0 Memory 1 High Decode Address */
	public static final int GT_PCI0M1HD = 0x88;
	/** PCI_1 I/O Low Decode Address */
	public static final int GT_PCI1IOLD = 0x90;
	/** PCI_1 I/O High Decode Address */
	public static final int GT_PCI1IOHD = 0x98;
	/** PCI_1 Memory 0 Low Decode Address */
	public static final int GT_PCI1M0LD = 0xa0;
	/** PCI_1 Memory 0 High Decode Address */
	public static final int GT_PCI1M0HD = 0xa8;
	/** PCI_1 Memory 1 Low Decode Address */
	public static final int GT_PCI1M1LD = 0xb0;
	/** PCI_1 Memory 1 High Decode Address */
	public static final int GT_PCI1M1HD = 0xb8;
	/** SCS[1:0]* Address Remap */
	public static final int GT_SCS10AR = 0xd0;
	/** SCS[3:2]* Address Remap */
	public static final int GT_SCS32AR = 0xd8;
	/** CS[2:0]* Address Remap */
	public static final int GT_CS20R = 0xe0;
	/** CS[3]* & Boot CS* Address Remap */
	public static final int GT_CS3BOOTR = 0xe8;
	/** PCI_0 IO Address Remap */
	public static final int GT_PCI0IOREMAP = 0xf0;
	/** PCI_0 Memory 0 Address Remap */
	public static final int GT_PCI0M0REMAP = 0xf8;
	/** PCI_0 Memory 1 Address Remap */
	public static final int GT_PCI0M1REMAP = 0x100;
	/** PCI_1 IO Address Remap */
	public static final int GT_PCI1IOREMAP = 0x108;
	/** PCI_1 Memory 0 Address Remap */
	public static final int GT_PCI1M0REMAP = 0x110;
	/** PCI_1 Memory 1 Address Remap */
	public static final int GT_PCI1M1REMAP = 0x118;
	/** PCI_1 Interrupt Acknowledge Virtual Register */
	public static final int GT_PCI1_IACK = 0xc30;
	/** PCI_0 Interrupt Acknowledge Virtual Register */
	public static final int GT_PCI0_IACK = 0xc34;
	/** PCI_0 Command */
	public static final int GT_PCI0_CMD = 0xc00;
	/** PCI_1 Configuration Address */
	public static final int GT_PCI1_CFGADDR = 0xcf0;
	/** PCI_1 Configuration Data Virtual Register */
	public static final int GT_PCI1_CFGDATA = 0xcf4;
	/** PCI_0 Configuration Address */
	public static final int GT_PCI0_CFGADDR = 0xcf8;
	/** PCI_0 Configuration Data Virtual Register */
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
	
	public void setIrq (int irq) {
		CpuLogger.getInstance().info("gt set irq " + irq);
		iomem.putWord(GT_PCI0_IACK, irq);
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
		final CpuLogger log = CpuLogger.getInstance();
		
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
		final CpuLogger log = CpuLogger.getInstance();
		
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
		final CpuLogger log = CpuLogger.getInstance();
		
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
