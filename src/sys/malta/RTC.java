package sys.malta;

import java.util.Calendar;
import java.util.GregorianCalendar;

import sys.util.Logger;
import sys.util.Symbols;

/**
 * real time clock
 */
public class RTC implements Device {
	
	public static final int M_ADR = 0x0;
	public static final int M_DAT = 0x1;

	/** control register b hour mode 24h/12h */
	private static int B_HOURMODE = 0x2;
	/** control register b hour mode binary/bcd */
	private static int B_DATAMODE = 0x4;
	
	private final Logger log = new Logger("RTC");
	private final int baseAddr;
	private int rtcadr;
	private int rtcdat;
	private int controla;
	private int controlb;
	private int controlc;
	
	public RTC(int baseAddr) {
		this.baseAddr = baseAddr;
		// binary not bcd
		// if this is missing you get a weird error about persistent clock invalid
		this.controlb = B_HOURMODE | B_DATAMODE;
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
	
	
	private void rtcAdrWrite (final int value) {
		// mc146818rtc.h
		// 0 = seconds, 2 = minutes, 4 = hours, 6 = dow, 7 = dom, 8 = month, 9 = year
		log.println(0, "rtc adr write " + value);
		rtcadr = value & 0xff;
		final Calendar c = new GregorianCalendar();
		
		if (value >= 0 && value <= 9 && (controlb & B_DATAMODE) == 0) {
			throw new RuntimeException("bcd read");
		}
		
		switch (value) {
			case 0x0:
				rtcdat = c.get(Calendar.SECOND);
				break;
			case 0x2:
				rtcdat = c.get(Calendar.MINUTE);
				break;
			case 0x4:
				// depends on control register b hour format
				if ((controlb & B_HOURMODE) != 0) {
					rtcdat = c.get(Calendar.HOUR_OF_DAY);
				} else {
					boolean pm = c.get(Calendar.AM_PM) == Calendar.PM;
					rtcdat = c.get(Calendar.HOUR) | (pm ? 0x80 : 0);
				}
				break;
			case 0x6:
				rtcdat = c.get(Calendar.DAY_OF_WEEK);
				break;
			case 0x7:
				rtcdat = c.get(Calendar.DAY_OF_MONTH);
				break;
			case 0x8:
				rtcdat = c.get(Calendar.MONTH);
				break;
			case 0x9:
				rtcdat = c.get(Calendar.YEAR);
				break;
			case 0x1:
			case 0x3:
			case 0x5:
				// alarm
				rtcdat = 0;
				break;
			case 0xa: {
				// register a
				// update in progress
				final boolean uip = c.get(Calendar.MILLISECOND) >= 990;
				rtcdat = controla | (uip ? 0x80 : 0);
				break;
			}
			case 0xb:
				// register b
				rtcdat = controlb;
				break;
			case 0xc:
				rtcdat = controlc;
				break;
			default:
				throw new RuntimeException(String.format("invalid rtc adr %x", value));
		}
	}
	
	private void rtcDatWrite (final int value) {
		switch (rtcadr) {
			case 0xa:
				setControlA(value);
				break;
			case 0xb:
				setControlB(value);
				break;
			default:
				throw new RuntimeException(String.format("unexpected rtc write adr %x dat %x", rtcadr, value));
		}
	}

	private void setControlA (final int value) {
		int rsx = value & 0xf;
		double p = rsxPeriod(rsx);
		int dvx = (value >> 4) & 0x7;
		log.println("set control a %x rsx: %x p: %f dvx: %x", value, rsx, p, dvx);
		if (dvx != 2) {
			throw new RuntimeException(String.format("unknown dvx %x", dvx));
		}
		controla = value & 0x7f;
		if ((controlb & 0x40) != 0) {
			throw new RuntimeException("periodic interrupt");
		} else {
			// set the pf flag
			controlc |= 0x40;
		}
	}

	private double rsxPeriod (int rsx) {
		double p = 0;
		if (rsx >= 3) {
			p = 0.5 / Math.pow(2, rsx - 15);
		} else if (rsx >= 1) {
			p = 0.5 / Math.pow(2, rsx + 6);
		}
		return p;
	}
	
	private void setControlB (final int value) {
		log.println("set control b %x", value);
		switch (value) {
			case 0:
				log.println("set bcd 12-h");
				break;
			case 4:
				log.println("set bcd 24-h");
				break;
			case 2:
				log.println("set binary 12-h");
				break;
			case 6:
				log.println("set binary 24-h");
				break;
			default:
				throw new RuntimeException("unknown b value " + value);
		}
		controlb = value;
	}
	
}
