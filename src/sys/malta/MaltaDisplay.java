package sys.malta;

import sys.mips.Cpu;
import sys.util.Logger;
import sys.util.Symbols;

/**
 * malta on board display
 */
public class MaltaDisplay implements Device {
	
	public static final int M_DISPLAY_LEDBAR = 0x8;
	public static final int M_DISPLAY_ASCIIWORD = 0x10;
	public static final int M_DISPLAY_ASCIIPOS0 = 0x18;
	public static final int M_DISPLAY_ASCIIPOS1 = 0x20;
	public static final int M_DISPLAY_ASCIIPOS2 = 0x28;
	public static final int M_DISPLAY_ASCIIPOS3 = 0x30;
	public static final int M_DISPLAY_ASCIIPOS4 = 0x38;
	public static final int M_DISPLAY_ASCIIPOS5 = 0x40;
	public static final int M_DISPLAY_ASCIIPOS6 = 0x48;
	public static final int M_DISPLAY_ASCIIPOS7 = 0x50;
	
	private static final Logger log = new Logger("Display");
	
	private final char[] asciiPos = new char[8];
	private final int baseAddr;
	
	private int ledBar = 0;
	private int asciiWord = 0;
	
	public MaltaDisplay(final int baseAddr) {
		this.baseAddr = baseAddr;
	}
	
	@Override
	public void init (final Symbols sym) {
		log.println("init display at " + Integer.toHexString(baseAddr));
		sym.init(MaltaDisplay.class, "M_", null, baseAddr, 4);
	}
	
	@Override
	public boolean isMapped (final int addr) {
		final int offset = addr - baseAddr;
		return offset >= 0 && offset < 0x100;
	}
	
	@Override
	public int systemRead (final int addr, final int size) {
		throw new RuntimeException("display read");
	}
	
	@Override
	public void systemWrite (final int addr, final int size, final int value) {
		final int offset = addr - baseAddr;
		
		switch (offset) {
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
				
			default:
				throw new RuntimeException("display write");
		}
	}
	
	private void asciiPosWrite (final int n, final int value) {
		asciiPos[n] = (char) (value & 0xff);
		String text = new String(asciiPos);
		Cpu.getInstance().getSupport().firePropertyChange("displaytext", null, text);
	}
	
	private void ledBarWrite (final int value) {
		ledBar = value;
		Cpu.getInstance().getSupport().firePropertyChange("displayled", null, ledBar);
	}
	
	private void asciiWordWrite (final int value) {
		asciiWord = value;
		Cpu.getInstance().getSupport().firePropertyChange("displayword", null, asciiWord);
	}
	
}
