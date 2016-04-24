package sys.malta;

import java.util.Calendar;

import sys.util.Logger;
import sys.util.Symbols;

/**
 * real time clock
 */
public class RTC implements Device {
	
	public static final int M_ADR = 0x0;
	public static final int M_DAT = 0x1;
	
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
	
	
}
