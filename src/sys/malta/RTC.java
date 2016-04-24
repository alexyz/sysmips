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
	
	private final Logger log = new Logger("RTC");
	private final int baseAddr;
	private int rtcadr;
	private int rtcdat;
	
	public RTC(int baseAddr) {
		this.baseAddr = baseAddr;
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
		
		switch (value) {
			case 0x0:
				rtcdat = c.get(Calendar.SECOND);
				break;
			case 0x2:
				rtcdat = c.get(Calendar.MINUTE);
				break;
			case 0x4:
				// depends on control register b hour format
				rtcdat = c.get(Calendar.HOUR);
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
//				final boolean uip = c.get(Calendar.MILLISECOND) >= 990;
//				if (uip) {
//					log.println("update in progress");
//				}
//				rtcdat = (uip ? 0x80 : 0);
				rtcdat = 0;
				break;
			}
			case 0xb:
				// register b
				rtcdat = (byte) 4;
				break;
			default:
				throw new RuntimeException("invalid index " + value);
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
	
	
}
