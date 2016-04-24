package sys.malta;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import sys.mips.*;
import sys.util.Logger;
import sys.util.Symbols;

/**
 * the intel 82371AB (PIIX4) southbridge (and bits of the SMSC FDC37M81 super io
 * controller)
 */
public class PIIX4 implements Device {
	
	// I8259 programmable interrupt controller
	public static final int M_PIC_MASTER = 0x20;
	
	// I8253 programmable interval timer
	public static final int M_PIT = 0x40;
	
	// I8042 ps/2 keyboard/mouse microcontroller (provided by superio)
	/** keyboard data read/write */
	public static final int M_KEYBOARD = 0x60;
	
	// real time clock
	public static final int M_RTC = 0x70;
	
	public static final int M_PIC_SLAVE = 0xa0;
	
	public static final int M_DMA2_MASK_REG = 0xd4;
	
	// 16650 uarts (provided by superio)
	public static final int M_COM2 = 0x2f8;
	public static final int M_COM1 = 0x3f8;
	
	private static final Logger log = new Logger("PIIX4");
	
	private final Device[] devices;
	private final int baseAddr;
	private final Uart com1;
	private final Uart com2;
	private final PIC pic1;
	private final PIC pic2;
	private final KBC kbc;
	private final RTC rtc;
	private final PIT pit;
	
	public PIIX4(final int baseAddr) {
		this.baseAddr = baseAddr;
		this.com1 = new Uart(baseAddr + M_COM1, 1, "Uart:COM1");
		this.com1.setConsole(true);
		this.com2 = new Uart(baseAddr + M_COM2, 1, "Uart:COM2");
		this.pic1 = new PIC(baseAddr + M_PIC_MASTER, true);
		this.pic2 = new PIC(baseAddr + M_PIC_SLAVE, false);
		this.kbc = new KBC(baseAddr + M_KEYBOARD);
		this.rtc = new RTC(baseAddr + M_RTC);
		this.pit = new PIT(baseAddr + M_PIT);
		this.devices = new Device[] { com1, com2, pic1, pic2, kbc, rtc, pit };
	}
	
	@Override
	public void init (final Symbols sym) {
		sym.init(PIIX4.class, "M_", null, baseAddr, 1);
		for (Device d : devices) {
			d.init(sym);
		}
	}
	
	@Override
	public boolean isMapped (final int addr) {
		final int offset = addr - baseAddr;
		return offset >= 0 && offset < 0xd00;
	}
	
	@Override
	public int systemRead (final int addr, final int size) {
		for (Device d : devices) {
			if (d.isMapped(addr)) {
				return d.systemRead(addr, size);
			}
		}
		
		final int offset = addr - baseAddr;
		switch (offset) {
			default:
				throw new RuntimeException("unknown system read offset " + Integer.toHexString(offset));
		}
	}
	
	@Override
	public void systemWrite (final int addr, final int size, final int value) {
		for (Device d : devices) {
			if (d.isMapped(addr)) {
				d.systemWrite(addr, size, value);
				return;
			}
		}
		
		final int offset = addr - baseAddr;
		switch (offset) {
			case M_DMA2_MASK_REG:
				// information in asm/dma.h
				log.println("enable dma channel 4+ ignored" + value);
				return;
				
			default:
				throw new RuntimeException("unknown system write offset " + Integer.toHexString(offset));
		}
	}
	
	
}
