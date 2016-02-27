package sys.malta;

import sys.mips.Cpu;
import sys.util.Logger;
import sys.util.Symbols;

/**
 * malta display
 */
public class Display implements Device {
	
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
	
	private final byte[] asciiPos = new byte[8];
	private final int baseAddr;
	
	private int ledBar = 0;
	private int asciiWord = 0;
	
	public Display(int baseAddr) {
		this.baseAddr = baseAddr;
	}
	
	@Override
	public void init (Symbols sym) {
		log.println("init display at " + Integer.toHexString(baseAddr));
		sym.init(Display.class, "M_", null, baseAddr, 4);
	}
	
	@Override
	public boolean isMapped (int addr) {
		return addr >= baseAddr && addr <= baseAddr + 80;
	}
	
	@Override
	public int systemRead (int addr, int size) {
		throw new RuntimeException("display read");
	}
	
	@Override
	public void systemWrite (final int addr, final int value, int size) {
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
	
	private void asciiPosWrite (int n, int value) {
		asciiPos[n] = (byte) value;
		Cpu.getInstance().getSupport().firePropertyChange("display", null, displayText());
	}
	
	private void ledBarWrite (int value) {
		ledBar = value;
		Cpu.getInstance().getSupport().firePropertyChange("display", null, displayText());
	}
	
	private void asciiWordWrite (int value) {
		asciiWord = value;
		Cpu.getInstance().getSupport().firePropertyChange("display", null, displayText());
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
