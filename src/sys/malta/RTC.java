package sys.malta;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

import sys.mips.*;
import sys.util.*;

/**
 * real time clock
 */
public class RTC extends Device {
	
	public static final int M_ADR = 0x0;
	public static final int M_DAT = 0x1;
	
	private static final long NS_IN_S = 1000_000_000;
	
	private static final int I_SEC = 0x0;
	private static final int I_SECALM = 0x1;
	private static final int I_MIN = 0x2;
	private static final int I_MINALM = 0x3;
	private static final int I_HOUR = 0x4;
	private static final int I_HOURALM = 0x5;
	private static final int I_DOW = 0x6;
	private static final int I_DOM = 0x7;
	private static final int I_MONTH = 0x8;
	private static final int I_YEAR = 0x9;
	private static final int I_REGA = 0xa;
	private static final int I_REGB = 0xb;
	private static final int I_REGC = 0xc;
	private static final int I_REGD = 0xd;
	
	/** update in progress */
	private static final int A_UIP = 0x80;
	private static final int DVX_NORMAL = 2;

	/** daylight savings enable */
	private static final int B_DSE = 0x1;
	/** control register b hour mode 0=12h 1=24h */
	private static final int B_HF24 = 0x2;
	/** control register b hour mode 0=bcd 1=binary */
	private static final int B_DMBIN = 0x4;
	/** square wave enable */
	private static final int B_SQWE = 0x8;
	/** update ended interrupt enable */
	private static final int B_UIE = 0x10;
	/** alarm interrupt enable */
	private static final int B_AIE = 0x20;
	/** periodic interrupt enable */
	private static final int B_PIE = 0x40;
	/** enable update cycles 0=auto update 1=no auto update */
	private static final int B_SET = 0x80;
	
	// all these are cleared on read
	/** update ended flag */
	private static final int C_UF = 0x10;
	/** alarm flag */
	private static final int C_AF = 0x20;
	/** periodic interrupt flag */
	private static final int C_PF = 0x40;
	/** interrupt request flag */
	private static final int C_IRQF = 0x80;
	
	public static void main (String[] args) throws Exception {
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		final RTC dev = new RTC(0) {
			@Override
			protected ScheduledExecutorService getExecutor () {
				return executor;
			}
		};
		dev.write(I_REGB, 0);
		System.out.println("12h+bcd time=" + dev.readtime(true));
		dev.write(I_REGB, B_HF24);
		System.out.println("24h+bcd time=" + dev.readtime(true));
		dev.write(I_REGB, B_DMBIN);
		System.out.println("12h+bin time=" + dev.readtime(false));
		dev.write(I_REGB, B_HF24 | B_DMBIN);
		System.out.println("24h+bin time=" + dev.readtime(false));
		
		for (int n = 0; n < 16; n++) {
			System.out.println(String.format("rsx %4s = %-16s", Integer.toBinaryString(n), new BigDecimal(rateSelectPeriod(n))));
		}
		
		// test interrupts...
		dev.write(I_REGB, 0);
		dev.write(I_REGA, 0x2f); // 2 = normal dvx, f = 500ms pi
		long st = System.nanoTime(), t;
		while ((t = System.nanoTime()) < st + NS_IN_S * 5) {
			int c1 = dev.read(I_REGC);
			if ((c1 & C_PF) != 0) {
				System.out.println("pf set at " + (1.0*(t-st))/NS_IN_S);
				int c2 = dev.read(I_REGC);
				if (c2 != 0) {
					throw new Exception("flag not cleared...");
				}
			}
			Thread.sleep(1);
		}
		
		// disable timer
		dev.write(I_REGA, 0x20);
		dev.read(I_REGC);
		Thread.sleep(1000);
		if (dev.read(I_REGC) != 0) {
			throw new Exception("flag set...");
		}
		
		executor.shutdown();
		System.out.println("done");
	}
	
	private String readtime (boolean bcd) throws Exception {
		// wait for uip
		int s;
		for (s = 0; (read(I_REGA) & A_UIP) == 0; s++) {
			Thread.sleep(1);
		}
		//System.out.println("uip set after " + s);
		for (s = 0; (read(I_REGA) & A_UIP) != 0; s++) {
			Thread.sleep(1);
		}
		//System.out.println("uip clear after " + s);
		return Arrays.toString(new String[] {
				readstr(I_YEAR, bcd),
				readstr(I_MONTH, bcd),
				readstr(I_DOM, bcd),
				readstr(I_DOW, bcd),
				readstr(I_HOUR, bcd),
				readstr(I_MIN, bcd),
				readstr(I_SEC, bcd)
		});
	}
	
	private String readstr (int i, boolean hex) {
		return hex ? "x" + Integer.toHexString(read(i)) : Integer.toString(read(i));
	}
	
	private int read (int i) {
		storeByte(M_ADR, (byte) i);
		return loadByte(M_DAT);
	}
	
	private void write (int i, int v) {
		storeByte(M_ADR, (byte) i);
		storeByte(M_DAT, (byte) v);
	}
	
	private final Logger log = new Logger("RTC");
	// XXX these are bytes represented as ints for convenience
	private int rtcadr;
	private int rtcdat;
	// these should really be indexes into the cmos ram
	private int controla;
	private int controlb;
	private int controlc;
	private double period;
	private Future<?> timerFuture;
	
	public RTC(int baseAddr) {
		super(baseAddr);
		// binary not bcd
		// if this is missing you get a weird error about persistent clock invalid
		this.controlb = B_HF24 | B_DMBIN;
	}
	
	@Override
	public void init (Symbols sym) {
		sym.init(getClass(), "M_", "M_RTC_", baseAddr, 1);
	}
	
	@Override
	public boolean isMapped (int addr) {
		final int offset = addr - baseAddr;
		return offset >= 0 && offset < 2;
	}
	
	@Override
	public byte loadByte (final int addr) {
		final int offset = addr - baseAddr;
		
		switch (offset) {
			case M_DAT:
				// should compute this from rtcadr each time?
				return (byte) rtcdat;
				
			default:
				throw new RuntimeException();
		}
	}
	
	@Override
	public void storeByte (final int addr, final byte value) {
		final int offset = offset(addr);
		
		switch (offset) {
			case M_ADR:
				rtcAdrWrite(value & 0xff);
				return;
				
			case M_DAT:
				rtcDatWrite(value & 0xff);
				return;
				
			default:
				throw new RuntimeException();
		}
	}

	/** convert to binary/bcd */
	private int toDataMode(int v) {
		return ((controlb & B_DMBIN) != 0) ? v : ((v / 10) << 4 | (v % 10));
	}
	
	private void rtcAdrWrite (final int value) {
		// mc146818rtc.h
		// 0 = seconds, 2 = minutes, 4 = hours, 6 = dow, 7 = dom, 8 = month, 9 = year
		log.println(0, "rtc adr write " + value);
		rtcadr = value & 0xff;
		// should freeze this if reg c set is 1
		final Calendar c = new GregorianCalendar();
		
		switch (value) {
			case I_SEC:
				rtcdat = toDataMode(c.get(Calendar.SECOND));
				break;
			case I_MIN:
				rtcdat = toDataMode(c.get(Calendar.MINUTE));
				break;
			case I_HOUR:
				// depends on control register b hour format
				if ((controlb & B_HF24) != 0) {
					rtcdat = toDataMode(c.get(Calendar.HOUR_OF_DAY));
				} else {
					boolean pm = c.get(Calendar.AM_PM) == Calendar.PM;
					rtcdat = toDataMode(c.get(Calendar.HOUR) | (pm ? 0x80 : 0));
				}
				break;
			case I_DOW:
				rtcdat = toDataMode(c.get(Calendar.DAY_OF_WEEK));
				break;
			case I_DOM:
				rtcdat = toDataMode(c.get(Calendar.DAY_OF_MONTH));
				break;
			case I_MONTH:
				rtcdat = toDataMode(c.get(Calendar.MONTH) + 1);
				break;
			case I_YEAR:
				// its only a byte...
				rtcdat = toDataMode(c.get(Calendar.YEAR) % 100);
				break;
			case I_SECALM:
			case I_MINALM:
			case I_HOURALM:
				// alarm
				rtcdat = 0;
				break;
			case I_REGA: {
				// register a
				// update in progress
				final boolean uip = c.get(Calendar.MILLISECOND) >= 990;
				rtcdat = controla | (uip ? A_UIP : 0);
				break;
			}
			case I_REGB:
				// register b
				rtcdat = controlb;
				break;
			case I_REGC:
				// cleared on read
				rtcdat = controlc;
				controlc = 0;
				break;
			default:
				throw new RuntimeException(String.format("invalid rtc adr %x", value));
		}
	}
	
	private void rtcDatWrite (final int value) {
		switch (rtcadr) {
			case I_REGA:
				setControlA(value);
				break;
			case I_REGB:
				setControlB(value);
				break;
			default:
				throw new RuntimeException(String.format("unexpected rtc write adr %x dat %x", rtcadr, value));
		}
	}

	private void setControlA (final int value) {
		int rsx = value & 0xf;
		double rsp = rateSelectPeriod(rsx);
		int dvx = (value >> 4) & 0x7;
		log.println("set control a %x rsx: %x rsp: %f dvx: %x", value, rsx, rsp, dvx);
		if (dvx != DVX_NORMAL) {
			throw new RuntimeException(String.format("unknown dvx %x", dvx));
		}
		
		controla = value & 0x7f;
		
		if (period != rsp) {
			log.println("period changed from " + period + " to " + rsp);
			period = rsp;
			if (timerFuture != null) {
				timerFuture.cancel(false);
				timerFuture = null;
			}
			
			if (rsp > 0) {
				long rspNs = (long) (rsp * NS_IN_S);
				timerFuture = getExecutor().scheduleAtFixedRate(() -> fireInt(), rspNs, rspNs, TimeUnit.NANOSECONDS);
			}
		}
	}
	
	private void fireInt() {
		if ((controlb & 0x40) != 0) {
			// add the exception...
			Cpu.getInstance().addException(new CpuExceptionParams(CpuConstants.EX_TRAP));
			throw new RuntimeException("periodic interrupt");
		} else {
			// just set the pf flag
			controlc |= C_PF;
		}
	}

	private static double rateSelectPeriod (int rsx) {
		double p = 0;
		if (rsx >= 3) {
			p = 1 / Math.pow(2, 16 - rsx);
		} else if (rsx >= 1) {
			p = 1 / Math.pow(2, 9 - rsx);
		}
		return p;
	}
	
	private void setControlB (final int value) {
		log.println("set control b %x: %s", value, controlbString(value));
		if ((value & ~(B_DMBIN | B_HF24)) != 0) {
			throw new RuntimeException("unexpected b " + value);
		}
		controlb = value;
	}
	
	public static String controlbString(int value) {
		List<String> l = new ArrayList<>();
		if ((value & 0x1) != 0) l.add("0:daylightsavings");
		if ((value & 0x2) != 0) l.add("1:24hour");
		if ((value & 0x4) != 0) l.add("2:binary");
		if ((value & 0x8) != 0) l.add("3:squarewave");
		if ((value & 0x10) != 0) l.add("4:updateendedinterrupt");
		if ((value & 0x20) != 0) l.add("5:alarminterrupt");
		if ((value & 0x40) != 0) l.add("6:periodicinterrupt");
		if ((value & 0x80) != 0) l.add("7:set");
		return l.toString();
	}
	
	protected ScheduledExecutorService getExecutor() {
		return Cpu.getInstance().getExecutor();
	}
	
}
