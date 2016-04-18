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
	public static final int M_PIT_COUNTER_0 = 0x40;
	public static final int M_PIT_COUNTER_1 = 0x41;
	public static final int M_PIT_COUNTER_2 = 0x42;
	public static final int M_PIT_TCW = 0x43;
	
	// I8042 ps/2 keyboard/mouse microcontroller (provided by superio)
	/** keyboard data read/write */
	public static final int M_PS2_DATA = 0x60;
	/** keyboard command (write), status (read) */
	public static final int M_PS2_CMDSTATUS = 0x64;
	
	// real time clock
	public static final int M_RTC_ADR = 0x70;
	public static final int M_RTC_DAT = 0x71;
	
	public static final int M_PIC_SLAVE = 0xa0;
	
	public static final int M_DMA2_MASK_REG = 0xd4;
	
	// 16650 uarts (provided by superio)
	public static final int M_COM2 = 0x2f8;
	public static final int M_COM1 = 0x3f8;
	
	private static final Logger log = new Logger("PIIX4");
	/** fire irq 1 on data */
	private static final int PS2_CFG_KEYINT = 0x1;
	/** fire irq 12 on data */
	private static final int PS2_CFG_AUXINT = 0x2;
	private static final int PS2_CFG_SYSTEM = 0x4;
	private static final int PS2_CFG_KEYDISABLE = 0x10;
	private static final int PS2_CFG_AUXDISABLE = 0x20;
	private static final int PS2_CFG_KEYTRANS = 0x40;
	
	private static int indexToCalendar (final int index) {
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
	
	private final Device[] devices;
	private final int baseAddr;
	private final Uart com1;
	private final Uart com2;
	private final PIC pic1;
	private final PIC pic2;
	
	private int timerCounter0;
	private int timerControlWord = -1;
	private int timerCounterByte;
	private Future<?> timerFuture;
	private int rtcadr;
	private int rtcdat;
	
	// there are really bytes represented as ints for convenience
	private int keydata;
	private int keystatus;
	private int keycfg;
	private int keycmd;
	
	public PIIX4(final int baseAddr) {
		this.baseAddr = baseAddr;
		this.com1 = new Uart(baseAddr + M_COM1, 1, "Uart:COM1");
		this.com2 = new Uart(baseAddr + M_COM2, 1, "Uart:COM2");
		this.pic1 = new PIC(baseAddr + M_PIC_MASTER, true);
		this.pic2 = new PIC(baseAddr + M_PIC_SLAVE, false);
		this.devices = new Device[] { com1, com2, pic1, pic2 };
		
		com1.setConsole(true);
		// system flag, nothing else?
		keycfg = 0x4;
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
//			case M_PIC_MASTER_IMR:
//				if (true) throw new RuntimeException("read pic master imr");
//				return picimr;
				
			case M_RTC_DAT:
				// should compute this from rtcadr each time?
				return rtcdat;
				
			case M_PS2_DATA: // 60
				log.println("read keydata %x", keydata);
				// no more data...
				keystatus = 0;
				return keydata;
				
			case M_PS2_CMDSTATUS: // 64
				log.println("read keystatus %x", keystatus);
				return keystatus;
				
			default:
				throw new RuntimeException("unknown system read " + Cpu.getInstance().getMemory().getSymbols().getNameAddrOffset(addr) + " size " + size);
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
			case M_PIT_TCW:
				timerControlWrite((byte) value);
				return;
				
			case M_PIT_COUNTER_0:
				timerCounter0Write((byte) value);
				return;
				
			case M_RTC_ADR:
				rtcAdrWrite(value);
				return;
				
			case M_RTC_DAT:
				rtcDatWrite(value);
				return;
				
			case M_DMA2_MASK_REG:
				// information in asm/dma.h
				log.println("enable dma channel 4+ ignored" + value);
				return;
				
//			case M_PIC_MASTER_CMD:
//				log.println("pic master write command %x", value & 0xff);
//				if ((value & 0x10) != 0) {
//					log.println("pic master ICW1");
//					pic1imr = 0;
//				}
//				return;
//				
//			case M_PIC_MASTER_IMR:
//				if (true) throw new RuntimeException("pic master imr " + Integer.toHexString(value & 0xff));
//				log.println("pic master write interrupt mask register %x", value & 0xff);
//				// XXX should probably do something here...
//				// XXX this will do a sign extension...
//				picimr = (byte) value;
//				return;
				
//			case M_PIC_SLAVE_CMD:
//				if (true) throw new RuntimeException("pic slave cmd " + Integer.toHexString(value & 0xff));
//				log.println("pic slave write command %x ignored", value & 0xff);
//				return;
//				
//			case M_PIC_SLAVE_IMR:
//				if (true) throw new RuntimeException("pic slave imr " + Integer.toHexString(value & 0xff));
//				log.println("pic slave write interrupt mask register %x ignored", value & 0xff);
//				return;
				
			case M_PS2_CMDSTATUS:
				keyCmdWrite(value & 0xff);
				return;
				
			case M_PS2_DATA:
				keyDataWrite(value & 0xff);
				return;
				
			default:
				throw new RuntimeException("unknown system write " + Symbols.getInstance().getNameAddrOffset(addr) + " <= " + Integer.toHexString(value));
		}
	}
	
	private void keyCmdWrite (final int value) {
		log.println("keycmd %x", value);
		
		switch (value) {
			case 0x20:
				log.println("keycmd %x: read config", value);
				keystatus = 1;
				keydata = keycfg;
				return;
			case 0x60:
				log.println("keycmd %x: write config", value);
				// wait for the next byte...
				keystatus = 0;
				keycmd = value;
				return;
			case 0xa7:
				log.println("keycmd %x: disable aux", value);
				// disable aux int?
				//keycfg &= ~0x2;
				// disable aux clock?
				keycfg |= 0x20;
				keystatus = 0;
				log.println(keyCfgString());
				return;
			case 0xa8:
				log.println("keycmd %x: enable aux", value);
				keycfg &= ~0x20;
				keystatus = 0;
				log.println(keyCfgString());
				return;
			case 0xa9:
				log.println("keycmd %x: test aux", value);
				keystatus = 1;
				// success
				keydata = 0;
				return;
			case 0xaa:
				log.println("keycmd %x: test", value);
				keystatus = 1;
				// success
				keydata = 0x55;
				return;
			case 0xd3:
				log.println("keycmd %x: write to aux out", value);
				keycmd = value;
				return;
			default:
				throw new RuntimeException(String.format("unknown keycmd %x", value));
		}
	}
	
	private void keyDataWrite (int value) {
		log.println("keydata %x", value);
		switch (keycmd) {
			case 0x60:
				// write to cfg
				log.println("keydata cfg: %x", value);
				keycfg = value;
				log.println(keyCfgString());
				return;
				
			case 0xd3:
				log.println("keydata aux out %x", value);
				// output buffer full?
				keystatus = 1;
				if ((keycfg & PS2_CFG_AUXINT) != 0) {
					// XXX is this right?
					log.println("adding mouse interrupt...");
					final CpuExceptionParams ep = new CpuExceptionParams(CpuConstants.EX_INTERRUPT, MaltaUtil.INT_SOUTHBRIDGE_INTR, MaltaUtil.IRQ_MOUSE);
					Cpu.getInstance().addException(ep);
				}
				return;
				
			default:
				throw new RuntimeException(String.format("unknown keydata %x for cmd %x", value, keycmd));
		}
	}

	private String keyCfgString () {
		// port1 = keyboard
		// port2 = mouse
		boolean p1int = (keycfg & PS2_CFG_KEYINT) != 0;
		boolean p2int = (keycfg & PS2_CFG_AUXINT) != 0;
		boolean sf = (keycfg & PS2_CFG_SYSTEM) != 0;
		boolean p1cldis = (keycfg & PS2_CFG_KEYDISABLE) != 0;
		boolean p2cldis = (keycfg & PS2_CFG_AUXDISABLE) != 0;
		boolean p1tr = (keycfg & PS2_CFG_KEYTRANS) != 0;
		return String.format("keycfg[%x =%s%s%s%s%s%s]", 
				keycfg, p1int ? " keyint" : "", p2int ? " auxint" : "", sf ? " system" : "", 
						p1cldis ? " keyclockdisabled" : "", p2cldis ? " auxclockdisabled" : "", p1tr ? " keytranslate" : "");
	}
	
	private void rtcAdrWrite (final int value) {
		// mc146818rtc.h
		// 0 = seconds, 2 = minutes, 4 = hours, 6 = dow, 7 = dom, 8 = month, 9 = year
		log.println(0, "rtc adr write " + value);
		rtcadr = (byte) value;
		if (value == 0xa) {
			// update in progress
			final boolean uip = (System.currentTimeMillis() % 1000) >= 990;
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
			
			final Cpu cpu = Cpu.getInstance();
			final ScheduledExecutorService e = cpu.getExecutor();
			final CpuExceptionParams ep = new CpuExceptionParams(CpuConstants.EX_INTERRUPT, MaltaUtil.INT_SOUTHBRIDGE_INTR, MaltaUtil.IRQ_TIMER);
			final Runnable r = () -> cpu.addException(ep);
			
			if (timerControlWord == 0x34) {
				// counter never reaches 0...
				final double hz = 1193182.0 / (timerCounter0 - 1.5);
				final long dur = Math.round(1000000.0 / hz);
				log.println("schedule pit at fixed rate " + hz + " hz " + dur + " us");
				timerFuture = e.scheduleAtFixedRate(r, dur, dur, TimeUnit.MICROSECONDS);
				
			} else if (timerControlWord == 0x38) {
				final double hz = 1193182.0 / (timerCounter0 - 0.5);
				final long dur = Math.round(1000000.0 / hz);
				log.println("schedule pit once " + hz + " hz " + dur + " us");
				timerFuture = e.schedule(r, dur, TimeUnit.MICROSECONDS);
			}
			
		} else {
			throw new RuntimeException("tcw write " + timerCounterByte);
		}
	}
	
}
