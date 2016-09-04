package sys.malta;

import java.util.*;

import sys.util.*;

/**
 * real time clock
 */
public class RTC implements Device {
	
	public static final int M_ADR = 0x0;
	public static final int M_DAT = 0x1;
	
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

	/** control register b hour mode 0=12h 1=24h */
	private static final int B_HF24 = 0x2;
	/** control register b hour mode 0=bcd 1=binary */
	private static final int B_DMBIN = 0x4;
	
	public static void main (String[] args) throws Exception {
		final RTC dev = new RTC(0);
		dev.write(I_REGB, 0);
		System.out.println("12h+bcd time=" + dev.readtime(true));
		dev.write(I_REGB, B_HF24);
		System.out.println("24h+bcd time=" + dev.readtime(true));
		dev.write(I_REGB, B_DMBIN);
		System.out.println("12h+bin time=" + dev.readtime(false));
		dev.write(I_REGB, B_HF24 | B_DMBIN);
		System.out.println("24h+bin time=" + dev.readtime(false));
		
		// XXX test interrupts...
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
		return hex ? Integer.toHexString(read(i)) + "x" : Integer.toString(read(i)) + "d";
	}
	
	private int read (int i) {
		systemWrite(M_ADR, 1, i);
		return (byte) systemRead(M_DAT, 1);
	}
	
	private void write (int i, int v) {
		systemWrite(M_ADR, 1, (byte) i);
		systemWrite(M_DAT, 1, (byte) v);
	}
	
	private final Logger log = new Logger("RTC");
	private final int baseAddr;
	// XXX these are bytes represented as ints for convenience
	private int rtcadr;
	private int rtcdat;
	private int controla;
	private int controlb;
	private int controlc;
	
	public RTC(int baseAddr) {
		this.baseAddr = baseAddr;
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
	public int systemRead (int addr, int size) {
		final int offset = addr - baseAddr;
		
		switch (offset) {
			case M_DAT:
				// should compute this from rtcadr each time?
				return rtcdat;
				
			default:
				throw new RuntimeException();
		}
	}
	
	@Override
	public void systemWrite (int addr, int size, int value) {
		final int offset = addr - baseAddr;
		value = value & 0xff;
		
		switch (offset) {
			case M_ADR:
				rtcAdrWrite(value);
				return;
				
			case M_DAT:
				rtcDatWrite(value);
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
				rtcdat = controlc;
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
		if ((controlb & 0x40) != 0) {
			throw new RuntimeException("periodic interrupt");
		} else {
			// FIXME do this on the tap not now...
			// set the pf flag
			controlc |= 0x40;
			throw new RuntimeException("periodic interrupt flag");
		}
	}

	private static double rateSelectPeriod (int rsx) {
		double p = 0;
		if (rsx >= 3) {
			p = 0.5 / Math.pow(2, rsx - 15);
		} else if (rsx >= 1) {
			p = 0.5 / Math.pow(2, rsx + 6);
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
	
}
