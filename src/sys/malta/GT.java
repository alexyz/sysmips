package sys.malta;

import sys.mips.Cpu;
import sys.util.Logger;
import sys.util.Symbols;

import static sys.util.ByteOrder.*;

import static sys.malta.GTUtil.*;

/**
 * this class kind-of represents the northbridge, but doesn't really handle any
 * device mapping
 */
public class GT implements Device {
	
	private static final Logger log = new Logger(GT.class);
	
	private int offset;
	private int configData;
	private int configAddr;
	private int irq;
	
	public GT () {
		//
	}
	
	public void setIrq (int irq) {
		log.println("set irq " + irq);
		this.irq = irq;
	}
	
	@Override
	public void init (Symbols sym, int offset) {
		log.println("init gt at " + Integer.toHexString(offset));
		this.offset = offset;
		sym.initInts(GTUtil.class, offset);
	}
	
	@Override
	public int systemRead (int addr, int size) {
		// swap due to an expected bug in GT
		return swapInt(systemRead2(addr, size));
	}
	
	private int systemRead2 (int addr, int size) {
		switch (addr) {
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
				return irq;
			case GT_INTERRUPT_CAUSE:
				return 0;
			default:
				throw new RuntimeException("could not read " + Integer.toHexString(addr));
		}
	}
	
	@Override
	public void systemWrite (int addr, int value, int size) {
		Cpu cpu = Cpu.getInstance();
		
		// XXX swap here?
		value = swapInt(value);
		String name = cpu.getMemory().getSymbols().getNameAddrOffset(offset + addr);
		log.println(String.format("write addr=%x name=%s <= value %x size %d", addr, name, value, size));
		
		switch (addr) {
			case GT_PCI0IOREMAP:
				log.println("set PCI 0 IO remap " + value);
				if (value != 0) {
					throw new RuntimeException("unknown remap");
				}
				break;
				
			case GT_INTERRUPT_CAUSE:
				// GT_INTRCAUSE_OFS
				log.println("ignore set interrupt cause " + value);
				break;
				
			case GT_PCI0_CMD:
				if (value == 0) {
					log.println("ignore set command " + value);
				} else {
					// enable byte swapping?
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
				throw new RuntimeException(String.format("invalid gt write %x", addr));
		}
		
	}
	
	private void setAddr (final int configAddr) {
		// pci configuration space
		this.configAddr = configAddr;
		
		int en = en(configAddr);
		int bus = bus(configAddr);
		int dev = dev(configAddr);
		int func = func(configAddr);
		int reg = reg(configAddr);
		log.println(String.format("set pci0 addr=%x en=%x bus=%x dev=%x func=%x reg=%x", configAddr, en, bus, dev, func, reg));
		
		if (bus == 0 && func == 0) {
			this.configData = 0;
			
		} else {
			throw new RuntimeException("could not set gt addr " + Integer.toHexString(configAddr));
		}
	}
	
	private void setData (final int value) {
		int en = en(configAddr);
		int bus = bus(configAddr);
		int dev = dev(configAddr);
		int func = func(configAddr);
		int reg = reg(configAddr);
		log.println(String.format("set PCI0 data %x en=%x bus=%x dev=%x func=%x reg=%x", value, en, bus, dev, func, reg));
		
		if (bus == 0 && dev == 0 && func == 0) {
			log.println("set GT_PCI0_CFGADDR_CONFIGEN_BIT");
			
		} else {
			throw new RuntimeException("could not set gt data " + Integer.toHexString(value));
		}
	}
	
}
