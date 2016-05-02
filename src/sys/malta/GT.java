package sys.malta;

import sys.mips.Cpu;
import sys.util.Logger;
import sys.util.Symbols;

import static sys.util.ByteOrder.*;

import static sys.malta.GTUtil.*;

/**
 * the GT64120A northbridge
 */
public class GT implements Device {
	
	private static final Logger log = new Logger("GT");
	
	private final int baseAddr;
	
	private int configData;
	private int configAddr;
	private int irq;
	/** byte swapping (set to true for big endian) */
	private boolean masterByteSwap;
	
	public GT (final int baseAddr) {
		this.baseAddr = baseAddr;
	}
	
	public void setIrq (final int irq) {
		//log.println("set irq " + irq);
		this.irq = irq;
	}
	
	@Override
	public void init (final Symbols sym) {
		log.println("init gt at " + Integer.toHexString(baseAddr));
		sym.init(GTUtil.class, "GT_", null, baseAddr, 4);
	}
	
	@Override
	public boolean isMapped (final int addr) {
		final int offset = addr - baseAddr;
		return offset >= 0 && offset < 0x1000;
	}
	
	@Override
	public int systemRead (final int addr, final int size) {
		int v = systemRead2(addr, size);
		return masterByteSwap ? swapInt(v) : v;
	}
	
	private int systemRead2 (final int addr, final int size) {
		final int offset = addr - baseAddr;
		switch (offset) {
			case GT_PCI0IOLD:
				return 0x80;
			case GT_PCI0IOHD:
				return 0xf;
			case GT_PCI0M0LD:
				return 0x90;
			case GT_PCI0M0HD:
				return 0x1f;
			case GT_PCI0M1LD:
				return 0x790;
			case GT_PCI0M1HD:
				return 0x1f;
			case GT_PCI0IOREMAP:
				return 0x80;
			case GT_PCI0M0REMAP:
				return 0x90;
			case GT_PCI0M1REMAP:
				return 0x790;
			case GT_PCI0_CFGADDR:
				return configAddr;
			case GT_PCI0_CFGDATA:
				return configData;
			case GT_PCI0_IACK:
				// malta-int.c GT_PCI0_IACK_OFS
				return irq;
			case GT_INTERRUPT_CAUSE:
				log.println("get ignored interrupt cause");
				return 0;
			default:
				throw new RuntimeException("could not read " + Integer.toHexString(offset));
		}
	}
	
	@Override
	public void systemWrite (final int addr, final int size, int value) {
		final int offset = addr - baseAddr;
		final Cpu cpu = Cpu.getInstance();
		if (masterByteSwap) {
			value = swapInt(value);
		}
		
		final String name = cpu.getSymbols().getNameAddrOffset(addr);
		log.println(String.format("write addr=%x name=%s <= value %x size %d", offset, name, value, size));
		
		switch (offset) {
			case GT_PCI0IOREMAP:
				log.println("set PCI 0 IO remap %x", value);
				if (value != 0) {
					throw new RuntimeException("unknown remap");
				}
				break;
				
			case GT_INTERRUPT_CAUSE:
				// GT_INTRCAUSE_OFS
				// should this be doing something?
				// or only if there is a pci device attached?
				log.println("ignore set interrupt cause %x", value);
				break;
				
			case GT_PCI0_CMD:
				switch (value) {
					case 0:
					case 0x10001:
						masterByteSwap = (value & 0x1) == 0;
						break;
					default:
						throw new RuntimeException(String.format("invalid gt command %x size %d", value, size));
				}
				break;
				
			case GT_PCI0_CFGADDR:
				setAddr(value);
				break;
				
			case GT_PCI0_CFGDATA:
				setData(value);
				break;
				
			default:
				throw new RuntimeException(String.format("invalid gt write %x", offset));
		}
		
	}
	
	private void setAddr (final int configAddr) {
		// pci configuration space
		this.configAddr = configAddr;
		
		final int en = en(configAddr);
		final int bus = bus(configAddr);
		final int dev = dev(configAddr);
		final int func = func(configAddr);
		final int reg = reg(configAddr);
		log.println(String.format("set pci0 addr=%x en=%x bus=%x dev=%x func=%x reg=%x", configAddr, en, bus, dev, func, reg));
		
		if (bus == 0 && func == 0) {
			this.configData = 0;
			
		} else {
			throw new RuntimeException("could not set gt addr " + Integer.toHexString(configAddr));
		}
	}
	
	private void setData (final int value) {
		final int en = en(configAddr);
		final int bus = bus(configAddr);
		final int dev = dev(configAddr);
		final int func = func(configAddr);
		final int reg = reg(configAddr);
		log.println(String.format("set PCI0 data %x en=%x bus=%x dev=%x func=%x reg=%x", value, en, bus, dev, func, reg));
		
		if (bus == 0 && dev == 0 && func == 0) {
			log.println("set GT_PCI0_CFGADDR_CONFIGEN_BIT");
			
		} else {
			throw new RuntimeException("could not set gt data " + Integer.toHexString(value));
		}
	}
	
}
