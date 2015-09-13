package sys.malta;

import java.lang.reflect.*;

import sys.mips.Cpu;
import sys.util.Logger;
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
	/** Interrupt Cause Register */ 
	public static final int GT_IC = 0xc18;
	/** CPU Interrupt Mask Register */
	public static final int GT_CPU_IM = 0xc1c;
	/** PCI_0 Interrupt Cause Mask Register */
	public static final int GT_PCI0_ICM = 0xc24;
	/** PCI_0 SErr0 Mask */
	public static final int GT_PCI0_SERR0 = 0xc28;
	/** CPU Select Cause Register */ 
	public static final int GT_CPU_SC = 0xc70;
	/** PCI_0 Interrupt Select Register */ 
	public static final int GT_PCI0_ISC = 0xc74;
	/** High Interrupt Cause Register */ 
	public static final int GT_ICH = 0xc98;
	/** CPU High Interrupt Mask Register */ 
	public static final int GT_CPU_IMH = 0xc9c;
	/** PCI_0 High Interrupt Cause Mask Register */
	public static final int GT_PCI0_ICMH = 0xca4;
	/** PCI_1 SErr1 Mask */
	public static final int GT_PCI1_SERR1 = 0xca8;
	/** PCI_1 Configuration Address */
	public static final int GT_PCI1_CFGADDR = 0xcf0;
	/** PCI_1 Configuration Data Virtual Register */
	public static final int GT_PCI1_CFGDATA = 0xcf4;
	/** PCI_0 Configuration Address */
	public static final int GT_PCI0_CFGADDR = 0xcf8;
	/** PCI_0 Configuration Data Virtual Register */
	public static final int GT_PCI0_CFGDATA = 0xcfc;

	private static final Logger log = new Logger(GT.class);
	
	private static int reg (final int cfgaddr) {
		return (cfgaddr >>> 2) & 0x3f;
	}

	private static int func (final int cfgaddr) {
		return (cfgaddr >>> 8) & 0x7;
	}

	private static int dev (final int cfgaddr) {
		return (cfgaddr >>> 11) & 0x1f;
	}

	private static int bus (final int cfgaddr) {
		return (cfgaddr >>> 16) & 0xff;
	}

	private static int en (final int cfgaddr) {
		return (cfgaddr >>> 31) & 0x1;
	}
	
	private final IOMemory iomem = new IOMemory();
	private int offset;
	
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
		log.println("set irq " + irq);
		iomem.putWord(GT_PCI0_IACK, irq);
	}

	@Override
	public void init (Symbols sym, int offset) {
		log.println("init gt at " + Integer.toHexString(offset));
		this.offset = offset;
		
		for (Field f : GT.class.getFields()) {
			final int m = f.getModifiers();
			if (Modifier.isPublic(m) && Modifier.isStatic(m) && Modifier.isFinal(m) && f.getType().isInstance(String.class)) {
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
		Cpu cpu = Cpu.getInstance();
		
		// XXX swap here?
		value = swap(value);
		String name = cpu.getMemory().getSymbols().getNameAddrOffset(offset + addr);
		final String msg = Integer.toHexString(addr) + " (" + name + ") <= " + Integer.toHexString(value) + " size " + size;
		log.println("write " + msg);
		iomem.put(addr, value, size);
		
		switch (addr) {
			case GT_PCI0IOREMAP:
				log.println("set PCI 0 IO remap " + value);
				if (value != 0) {
					throw new RuntimeException("unknown remap");
				}
				break;
				
			case GT_IC:
			case GT_PCI0_CMD:
				log.println("ignore" + value);
				break;
				
			case GT_PCI0_CFGADDR:
				setAddr(value);
				break;
			
			case GT_PCI0_CFGDATA:
				setData(value);
				break;
				
			default:
				throw new RuntimeException("invalid gt write " + msg);
		}
		
	}
	
	private void setAddr (final int cfgaddr) {
		// pci configuration space
		int en = en(cfgaddr);
		int bus = bus(cfgaddr);
		int dev = dev(cfgaddr);
		int func = func(cfgaddr);
		int reg = reg(cfgaddr);
		String msg = String.format("address=%x en=%x bus=%x dev=%x func=%x reg=%x", cfgaddr, en, bus, dev, func, reg);
		log.println("pci0 set addr " + msg);
		
		if (bus == 0 && func == 0) {
			iomem.putWord(GT_PCI0_CFGDATA, 0);
			
		} else {
			throw new RuntimeException("gt pci0 set addr " + msg);
		}
	}

	private void setData (int value) {
		int cfgaddr = iomem.getWord(GT_PCI0_CFGADDR);
		int en = en(cfgaddr);
		int bus = bus(cfgaddr);
		int dev = dev(cfgaddr);
		int func = func(cfgaddr);
		int reg = reg(cfgaddr);
		log.println(String.format("set PCI0 config data %x en=%x bus=%x dev=%x func=%x reg=%x", value, en, bus, dev, func, reg));
		
		if (bus == 0 && dev == 0 && func == 0) {
			log.println("set GT_PCI0_CFGADDR_CONFIGEN_BIT");
			
		} else {
			throw new RuntimeException("set unknown bus/device/function data");
		}
	}

}
