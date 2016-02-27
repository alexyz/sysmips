package sys.malta;

import java.util.Calendar;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import sys.mips.Constants;
import sys.mips.Cpu;
import sys.mips.CpuExceptionParams;
import sys.util.Logger;
import sys.util.Symbols;

/**
 * this class kind-of represents the southbridge, but it is also doing device
 * mapping
 */
// COM port detection - 8250.c - autoconfig()
public class Malta implements Device {
	
	public static final int M_SDRAM = 0x0;
	public static final int M_UNCACHED_EX_H = 0x100;
	
	public static final int M_PCI1 = 0x0800_0000;
	
	// connected with value of [GT_PCI0IOLD]
	public static final int M_PIIX4 = 0x1000_0000;
	
	// 82371AB (PIIX4) 3.1.2. IO SPACE REGISTERS
	public static final int M_PIC_MASTER_CMD = M_PIIX4 + 0x20;
	public static final int M_PIC_MASTER_IMR = M_PIIX4 + 0x21;
	
	// programmable interval timer
	public static final int M_I8253_COUNTER_0 = M_PIIX4 + 0x40;
	public static final int M_I8253_COUNTER_1 = M_PIIX4 + 0x41;
	public static final int M_I8253_COUNTER_2 = M_PIIX4 + 0x42;
	public static final int M_I8253_TCW = M_PIIX4 + 0x43;
	
	public static final int M_RTCADR = M_PIIX4 + 0x70;
	public static final int M_RTCDAT = M_PIIX4 + 0x71;
	public static final int M_PIC_SLAVE_CMD = M_PIIX4 + 0xa0;
	public static final int M_PIC_SLAVE_IMR = M_PIIX4 + 0xa1;
	public static final int M_DMA2_MASK_REG = M_PIIX4 + 0xD4;
	
	// the first uart is on the isa bus connected to the PIIX4
	public static final int M_COM1 = M_PIIX4 + 0x3f8;
	
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
	public static final int M_SCSPEC2_BONITO = 0x1fe0_0010;
	
	private static final Logger log = new Logger("Malta");
	
	private static int indexToCalendar (int index) {
		switch (index) {
			case 0: 
				return Calendar.SECOND;
			case 2: 
				return Calendar.MINUTE;
			case 4:
				// depends on control register b hour format
				return Calendar.HOUR;
			case 6: 
				return Calendar.DAY_OF_WEEK;
			case 7: 
				return Calendar.DAY_OF_MONTH;
			case 8: 
				return Calendar.MONTH;
			case 9: 
				return Calendar.YEAR;
			default: 
				throw new RuntimeException("invalid index " + index);
		}
	}
	
	// the GT is the northbridge
	private final int baseAddr;
	private final GT gt;
	private final Display display;
	private final Uart com1;
	
	private int timerCounter0;
	private int timerControlWord = -1;
	private int timerCounterByte;
	private ScheduledFuture<?> timerFuture;
	private int rtcadr;
	private int rtcdat;
	private int picimr;
	
	public Malta (int baseAddr) {
		this.baseAddr = baseAddr;
		this.gt = new GT(baseAddr + M_GTBASE);
		this.com1 = new Uart(baseAddr + M_COM1, "COM1");
		this.display = new Display(baseAddr + M_DISPLAY);
	}
	
	public GT getGt () {
		return gt;
	}
	
	@Override
	public void init (Symbols sym) {
		log.println("init malta at " + Integer.toHexString(baseAddr));
		
		display.init(sym);
		com1.init(sym);
		gt.init(sym);
		
		sym.put(baseAddr + M_SDRAM, "M_SDRAM");
		sym.put(baseAddr + M_UNCACHED_EX_H, "M_UNCACHED_EX_H", 0x80);
		sym.put(baseAddr + M_PCI1, "M_PCI1");
		sym.put(baseAddr + M_PIIX4, "M_PIIX4");
		sym.put(baseAddr + M_PIC_MASTER_CMD, "M_PIC_MASTER_CMD", 1);
		sym.put(baseAddr + M_PIC_MASTER_IMR, "M_PIC_MASTER_IMR", 1);
		sym.put(baseAddr + M_I8253_COUNTER_0, "M_I8253_COUNTER_0", 1);
		sym.put(baseAddr + M_I8253_COUNTER_1, "M_I8253_COUNTER_1", 1);
		sym.put(baseAddr + M_I8253_COUNTER_2, "M_I8253_COUNTER_2", 1);
		sym.put(baseAddr + M_I8253_TCW, "M_I8253_TCW", 1);
		sym.put(baseAddr + M_RTCADR, "M_RTCADR");
		sym.put(baseAddr + M_RTCDAT, "M_RTCDAT");
		sym.put(baseAddr + M_PIC_SLAVE_CMD, "M_PIC_SLAVE_CMD", 1);
		sym.put(baseAddr + M_PIC_SLAVE_IMR, "M_PIC_SLAVE_IMR", 1);
		sym.put(baseAddr + M_DMA2_MASK_REG, "M_DMA2_MASK_REG");
		sym.put(baseAddr + M_PCI2, "M_PCI2");
		sym.put(baseAddr + M_GTBASE, "M_GTBASE");
		sym.put(baseAddr + M_MONITORFLASH, "M_MONITORFLASH");
		sym.put(baseAddr + M_RESERVED, "M_RESERVED");
		sym.put(baseAddr + M_DEVICES, "M_DEVICES");
		sym.put(baseAddr + M_DISPLAY, "M_DISPLAY");
		sym.put(baseAddr + M_SCSPEC1, "M_SCSPEC1");
		sym.put(baseAddr + M_SCSPEC2, "M_SCSPEC2");
		sym.put(baseAddr + M_SCSPEC2_BONITO, "M_SCSPEC2_BONITO");
		sym.put(baseAddr + M_CBUS, "M_CBUS");
		sym.put(baseAddr + M_BOOTROM, "M_BOOTROM");
		sym.put(baseAddr + M_REVISION, "M_REVISION", 8);
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
			
		} else if (com1.isMapped(addr)) {
			return com1.systemRead(addr, size);
			
		} else {
			int offset = addr - baseAddr;
			switch (offset) {
				case M_REVISION:
					return 1;
				case M_PIC_MASTER_IMR:
					return picimr;
				case M_RTCDAT:
					// should compute this from rtcadr each time?
					return rtcdat;
				default:
					throw new RuntimeException("unknown system read " + Cpu.getInstance().getMemory().getSymbols().getNameAddrOffset(addr) + " size " + size);
			}
		}
	}
	
	@Override
	public void systemWrite (final int addr, final int value, int size) {
		if (gt.isMapped(addr)) {
			gt.systemWrite(addr, value, size);
			
		} else if (com1.isMapped(addr)) {
			com1.systemWrite(addr, value, size);
			
		} else if (addr >= baseAddr + M_UNCACHED_EX_H && addr < baseAddr + M_UNCACHED_EX_H + 0x100) {
			//log.println("set uncached exception handler " + Symbols.getInstance().getNameOffset(baseAddr + addr) + " <= " + Integer.toHexString(value));
			
		} else if (display.isMapped(addr)) {
			display.systemWrite(addr, value, size);
			
		} else {
			int offset = addr - baseAddr;
			switch (offset) {
				case M_I8253_TCW:
					timerControlWrite((byte) value);
					return;
					
				case M_I8253_COUNTER_0:
					timerCounter0Write((byte) value);
					return;
					
				case M_RTCADR:
					rtcAdrWrite(value);
					return;
					
				case M_RTCDAT:
					rtcDatWrite(value);
					return;
					
				case M_DMA2_MASK_REG:
					// information in asm/dma.h
					log.println("enable dma channel 4+ ignored" + value);
					return;
					
				case M_PIC_MASTER_CMD:
					log.println("pic master write command %x ignored", value & 0xff);
					return;
					
				case M_PIC_MASTER_IMR:
					log.println("pic master write interrupt mask register %x", value & 0xff);
					// XXX should probably do something here...
					picimr = (byte) value;
					return;
					
				case M_PIC_SLAVE_CMD:
					log.println("pic slave write command %x ignored", value & 0xff);
					return;
					
				case M_PIC_SLAVE_IMR:
					log.println("pic slave write interrupt mask register %x ignored", value & 0xff);
					return;
					
				default:
					throw new RuntimeException("unknown system write " + Symbols.getInstance().getNameOffset(addr) + " <= " + Integer.toHexString(value));
			}
		}
		
	}
	
	private void rtcAdrWrite (final int value) {
		// mc146818rtc.h
		// 0 = seconds, 2 = minutes, 4 = hours, 6 = dow, 7 = dom, 8 = month, 9 = year
		log.println(0, "rtc adr write " + value);
		rtcadr = (byte) value;
		if (value == 0xa) {
			// update in progress
			boolean uip = (System.currentTimeMillis() % 1000) >= 990;
			rtcdat = (byte) (uip ? 0x80 : 0);
		} else if (value == 0xb) {
			rtcdat = (byte) 4;
		} else {
			final int f = indexToCalendar(value);
			rtcdat = (byte) Calendar.getInstance().get(f);
		}
	}
	
	private void rtcDatWrite (final int value) {
		if (rtcadr == 0xb && value == 4) {
			// set mode binary
			return;
		} else if (rtcadr == 0xb && value == 0) {
			// set mode bcd (ugh!)
			return;
		}
		throw new RuntimeException("unexpected rtc write adr " + Integer.toHexString(rtcadr) + " dat " + Integer.toHexString(value));
	}
	
	private void timerControlWrite (final int value) {
		log.println("timer control word write " + Integer.toHexString(value));
		// i8253.c init_pit_timer
		// 34 = binary, rate generator, r/w lsb then msb
		// 38 = software triggered strobe
		if (value == 0x34 || value == 0x38) {
			timerControlWord = value;
			timerCounterByte = 0;
		} else {
			throw new RuntimeException("unexpected tcw write " + Integer.toHexString(value));
		}
	}
	
	private void timerCounter0Write (final byte value) {
		log.println("timer counter 0 write " + Integer.toHexString(value));
		// lsb then msb
		// #define CLOCK_TICK_RATE 1193182
		// #define LATCH  ((CLOCK_TICK_RATE + HZ/2) / HZ)
		// default HZ_250
		// linux sets this to 12a5 (4773 decimal) = ((1193182 + 250/2) / 250)
		// hz = 1193182/(c-0.5)
		// dur = (c-0.5)/1193182
		
		if (timerCounterByte == 0) {
			timerCounter0 = value & 0xff;
			timerCounterByte++;
			
		} else if (timerCounterByte == 1) {
			timerCounter0 = (timerCounter0 & 0xff) | ((value & 0xff) << 8);
			timerCounterByte = 0;
			if (timerFuture != null) {
				timerFuture.cancel(false);
			}
			
			Cpu cpu = Cpu.getInstance();
			ScheduledExecutorService e = cpu.getExecutor();
			CpuExceptionParams ep = new CpuExceptionParams(Constants.EX_INTERRUPT, MaltaUtil.INT_SB_INTR, MaltaUtil.IRQ_TIMER);
			Runnable r = () -> cpu.addException(ep);
			
			if (timerControlWord == 0x34) {
				// counter never reaches 0...
				double hz = 1193182.0 / (timerCounter0 - 1.5);
				long dur = Math.round(1000000.0 / hz);
				log.println("schedule pit at fixed rate " + hz + " hz " + dur + " us");
				timerFuture = e.scheduleAtFixedRate(r, dur, dur, TimeUnit.MICROSECONDS);
				
			} else if (timerControlWord == 0x38) {
				double hz = 1193182.0 / (timerCounter0 - 0.5);
				long dur = Math.round(1000000.0 / hz);
				log.println("schedule pit once " + hz + " hz " + dur + " us");
				timerFuture = e.schedule(r, dur, TimeUnit.MICROSECONDS);
			}
			
		} else {
			throw new RuntimeException("tcw write " + timerCounterByte);
		}
	}
	
}
