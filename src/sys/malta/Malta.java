package sys.malta;

import java.beans.PropertyChangeSupport;
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
	/** receive (write) / transmit (read) */
	public static final int M_COM1_RX_TX = M_COM1 + 0;
	/** interrupt enable */
	public static final int M_COM1_IER = M_COM1 + 1;
	/**
	 * fifo control register (writes) / interrupt identification register
	 * (reads) / extended features register (not supported)
	 */
	public static final int M_COM1_FCR_IIR_EFR = M_COM1 + 2;
	/** line control register */
	public static final int M_COM1_LCR = M_COM1 + 3;
	/** modem control register */
	public static final int M_COM1_MCR = M_COM1 + 4;
	/** line status register */
	public static final int M_COM1_LSR = M_COM1 + 5;
	/** modem status register */
	public static final int M_COM1_MSR = M_COM1 + 6;
	
	public static final int M_PCI2 = 0x1800_0000;
	
	public static final int M_GTBASE = 0x1be0_0000;
	
	public static final int M_CBUS = 0x1c00_0000;
	
	public static final int M_MONITORFLASH = 0x1e00_0000;
	public static final int M_RESERVED = 0x1e40_0000;
	
	public static final int M_DEVICES = 0x1f00_0000;
	public static final int M_DISPLAY = 0x1f00_0400;
	public static final int M_DISPLAY_LEDBAR = M_DISPLAY + 0x8;
	public static final int M_DISPLAY_ASCIIWORD = M_DISPLAY + 0x10;
	public static final int M_DISPLAY_ASCIIPOS0 = M_DISPLAY + 0x18;
	public static final int M_DISPLAY_ASCIIPOS1 = M_DISPLAY + 0x20;
	public static final int M_DISPLAY_ASCIIPOS2 = M_DISPLAY + 0x28;
	public static final int M_DISPLAY_ASCIIPOS3 = M_DISPLAY + 0x30;
	public static final int M_DISPLAY_ASCIIPOS4 = M_DISPLAY + 0x38;
	public static final int M_DISPLAY_ASCIIPOS5 = M_DISPLAY + 0x40;
	public static final int M_DISPLAY_ASCIIPOS6 = M_DISPLAY + 0x48;
	public static final int M_DISPLAY_ASCIIPOS7 = M_DISPLAY + 0x50;
	
	public static final int M_SCSPEC1 = 0x1f10_0010;
	
	public static final int M_BOOTROM = 0x1fc0_0000;
	public static final int M_REVISION = 0x1fc0_0010;
	
	public static final int M_SCSPEC2 = 0x1fd0_0010;
	public static final int M_SCSPEC2_BONITO = 0x1fe0_0010;
	
	/** enables both the XMIT and RCVR FIFOs */
	public static final int FCR_ENABLE_FIFO = 1;
	/** clears all bytes in the RCVR FIFO */
	public static final int FCR_CLEAR_RCVR = 2;
	/** clears all bytes in the XMIT FIFO */
	public static final int FCR_CLEAR_XMIT = 3;
	
	private static final Logger log = new Logger(Malta.class);
	
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
	
	// this should probably live on the cpu
	private final PropertyChangeSupport support = new PropertyChangeSupport(this);
	private final StringBuilder consoleSb = new StringBuilder();
	// the GT is the northbridge
	private final GT gt = new GT();
	private final byte[] asciiPos = new byte[8];
	
	private int offset;
	private int timerCounter0;
	private int timerControlWord = -1;
	private int timerCounterByte;
	private ScheduledFuture<?> timerFuture;
	private int ledBar = 0;
	private int asciiWord = 0;
	private int rtcadr;
	private int rtcdat;
	private int picimr;
	
	private byte ier;
	private byte mcr;
	private byte lcr;
	private byte iir;
	
	public Malta () {
		//
	}
	
	public GT getGt () {
		return gt;
	}
	
	@Override
	public void init (Symbols sym, int offset) {
		log.println("init malta at " + Integer.toHexString(offset));
		this.offset = offset;
		
		sym.put(offset + M_SDRAM, "M_SDRAM");
		sym.put(offset + M_UNCACHED_EX_H, "M_UNCACHED_EX_H", 0x80);
		sym.put(offset + M_PCI1, "M_PCI1");
		sym.put(offset + M_PIIX4, "M_PIIX4");
		sym.put(offset + M_PIC_MASTER_CMD, "M_PIC_MASTER_CMD", 1);
		sym.put(offset + M_PIC_MASTER_IMR, "M_PIC_MASTER_IMR", 1);
		sym.put(offset + M_I8253_COUNTER_0, "M_I8253_COUNTER_0", 1);
		sym.put(offset + M_I8253_COUNTER_1, "M_I8253_COUNTER_1", 1);
		sym.put(offset + M_I8253_COUNTER_2, "M_I8253_COUNTER_2", 1);
		sym.put(offset + M_I8253_TCW, "M_I8253_TCW", 1);
		sym.put(offset + M_RTCADR, "M_RTCADR");
		sym.put(offset + M_RTCDAT, "M_RTCDAT");
		sym.put(offset + M_PIC_SLAVE_CMD, "M_PIC_SLAVE_CMD", 1);
		sym.put(offset + M_PIC_SLAVE_IMR, "M_PIC_SLAVE_IMR", 1);
		sym.put(offset + M_DMA2_MASK_REG, "M_DMA2_MASK_REG");
		sym.put(offset + M_COM1_RX_TX, "M_COM1_RX_TX", 1);
		sym.put(offset + M_COM1_IER, "M_COM1_IER", 1);
		sym.put(offset + M_COM1_FCR_IIR_EFR, "M_COM1_FCR_IIR_EFR", 1);
		sym.put(offset + M_COM1_LCR, "M_COM1_LCR", 1);
		sym.put(offset + M_COM1_MCR, "M_COM1_MCR", 1);
		sym.put(offset + M_COM1_LSR, "M_COM1_LSR", 1);
		sym.put(offset + M_COM1_MSR, "M_COM1_MSR", 1);
		sym.put(offset + M_PCI2, "M_PCI2");
		sym.put(offset + M_GTBASE, "M_GTBASE");
		sym.put(offset + M_MONITORFLASH, "M_MONITORFLASH");
		sym.put(offset + M_RESERVED, "M_RESERVED");
		sym.put(offset + M_DEVICES, "M_DEVICES");
		sym.put(offset + M_DISPLAY, "M_DISPLAY");
		sym.put(offset + M_SCSPEC1, "M_SCSPEC1");
		sym.put(offset + M_SCSPEC2, "M_SCSPEC2");
		sym.put(offset + M_SCSPEC2_BONITO, "M_SCSPEC2_BONITO");
		sym.put(offset + M_CBUS, "M_CBUS");
		sym.put(offset + M_BOOTROM, "M_BOOTROM");
		sym.put(offset + M_REVISION, "M_REVISION", 8);
		
		gt.init(sym, offset + M_GTBASE);
	}
	
	@Override
	public int systemRead (int addr, int size) {
		switch (addr) {
			case M_REVISION:
				return 1;
			case M_COM1_LSR:
				// always ready?
				return 0x20;
			case M_PIC_MASTER_IMR:
				return picimr;
			case M_RTCDAT:
				// should compute this from rtcadr each time?
				return rtcdat;
			case M_COM1_IER:
				return ier;
			case M_COM1_MCR:
				return mcr;
			case M_COM1_LCR:
				return lcr;
			case M_COM1_FCR_IIR_EFR:
				return iir;
			default:
				break;
		}
		
		if (addr >= M_GTBASE && addr < M_GTBASE + 0x1000) {
			return gt.systemRead(addr - M_GTBASE, size);
			
		} else {
			throw new RuntimeException("unknown system read " + Cpu.getInstance().getMemory().getSymbols().getNameAddrOffset(offset+addr) + " size " + size);
		}
	}
	
	@Override
	public void systemWrite (final int addr, final int value, int size) {
		switch (addr) {
			case M_DISPLAY_ASCIIWORD:
				asciiWordWrite(value);
				return;
				
			case M_DISPLAY_LEDBAR:
				ledBarWrite(value);
				return;
				
			case M_DISPLAY_ASCIIPOS0:
				asciiPosWrite(0, value);
				return;
				
			case M_DISPLAY_ASCIIPOS1:
				asciiPosWrite(1, value);
				return;
				
			case M_DISPLAY_ASCIIPOS2:
				asciiPosWrite(2, value);
				return;
				
			case M_DISPLAY_ASCIIPOS3:
				asciiPosWrite(3, value);
				return;
				
			case M_DISPLAY_ASCIIPOS4:
				asciiPosWrite(4, value);
				return;
				
			case M_DISPLAY_ASCIIPOS5:
				asciiPosWrite(5, value);
				return;
				
			case M_DISPLAY_ASCIIPOS6:
				asciiPosWrite(6, value);
				return;
				
			case M_DISPLAY_ASCIIPOS7:
				asciiPosWrite(7, value);
				return;
				
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
				log.println("enable dma channel 4+" + value);
				return;
				
			case M_COM1_RX_TX:
				consoleWrite(value & 0xff);
				return;
				
			case M_COM1_IER:
				log.println("set com1 ier %x", value);
				// we only want bottom 4 bits, linux might set more to autodetect other chips
				ier = (byte) (value & 0xf);
				return;
				
			case M_COM1_MCR:
				log.println("set com1 mcr %x", value);
				ier = (byte) value;
				return;
				
			case M_COM1_LCR:
				log.println("set com1 lcr %x", value);
				lcr = (byte) value;
				return;
				
			case M_COM1_FCR_IIR_EFR:
				log.println("set com1 fcr %x", value);
				if (value == FCR_ENABLE_FIFO) {
					// this will call autoconfig_16550a
					iir |= 0b1100_0000;
				}
				return;
			
			case M_PIC_MASTER_CMD:
				log.println("pic master write command %x", value & 0xff);
				return;
				
			case M_PIC_MASTER_IMR:
				log.println("pic master write interrupt mask register %x", value & 0xff);
				// XXX should probably do something here...
				picimr = (byte) value;
				return;
				
			case M_PIC_SLAVE_CMD:
				log.println("pic slave write command %x", value & 0xff);
				return;
				
			case M_PIC_SLAVE_IMR:
				log.println("pic slave write interrupt mask register %x", value & 0xff);
				return;
				
			default:
				break;
		}
		
		if (addr >= M_GTBASE && addr < M_GTBASE + 0x1000) {
			gt.systemWrite(addr - M_GTBASE, value, size);
			return;
			
		} else if (addr >= M_UNCACHED_EX_H && addr < M_UNCACHED_EX_H + 0x100) {
			log.println("set uncached exception handler " + Symbols.getInstance().getNameOffset(offset + addr) + " <= " + Integer.toHexString(value));
			return;
			
		} else {
			throw new RuntimeException("unknown system write " + Symbols.getInstance().getNameOffset(offset + addr) + " <= " + Integer.toHexString(value));
		}
		
	}
	
	private void asciiPosWrite (int n, int value) {
		asciiPos[n] = (byte) value;
		support.firePropertyChange("display", null, displayText());
	}
	
	private void ledBarWrite (int value) {
		ledBar = value;
		support.firePropertyChange("display", null, displayText());
	}
	
	private void asciiWordWrite (int value) {
		asciiWord = value;
		support.firePropertyChange("display", null, displayText());
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

	private void consoleWrite (int value) {
		if ((value >= 32 && value < 127) || value == '\n') {
			consoleSb.append((char) value);
		} else if (value != '\r') {
			consoleSb.append("{" + Integer.toHexString(value) + "}");
		}
		if (value == '\n' || consoleSb.length() > 160) {
			final String line = consoleSb.toString();
			log.println("console: " + line.trim());
			if (line.contains("WARNING")) {
				log.println("calls=" + Cpu.getInstance().getCalls().callString());
			}
			support.firePropertyChange("console", null, line);
			consoleSb.delete(0, consoleSb.length());
		}
	}
	
	public PropertyChangeSupport getSupport () {
		return support;
	}
	
	public String displayText() {
		final StringBuilder sb = new StringBuilder();
		sb.append(Integer.toBinaryString(ledBar)).append(" ");
		sb.append(Integer.toHexString(asciiWord)).append(" ");
		for (int n = 0; n < 8; n++) {
			int w = asciiPos[n] & 0xff;
			sb.append(w != 0 ? (char) w : ' ');
		}
		return sb.toString();
	}
	
}
