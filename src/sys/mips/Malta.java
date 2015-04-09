package sys.mips;

import java.beans.PropertyChangeSupport;
import java.io.IOException;

import javax.swing.*;

import sys.linux.Linux;

public class Malta {
	
	public static final int DISPLAY = 0x1f000400;
	public static final int LEDBAR = DISPLAY + 0x8;
	public static final int ASCIIWORD = DISPLAY + 0x10;
	public static final int ASCIIPOS = DISPLAY + 0x18;
	
	private final Cpu cpu;
	private final PropertyChangeSupport support = new PropertyChangeSupport(this);
	
	public Malta (Cpu cpu) {
		this.cpu = cpu;
	}

	public void update (int addr) {
		Memory mem = cpu.getMemory();
		int data = mem.loadWord(addr);
		System.out.println("malta update: " + Integer.toHexString(addr) + " => " + Integer.toHexString(data));
		int maddr = addr - mem.getSystem();
		if (maddr >= DISPLAY && maddr < DISPLAY + 0x100) {
			support.firePropertyChange("display", "", displayText());
		} else {
			throw new RuntimeException("unknown malta update");
		}
	}
	
	public PropertyChangeSupport getSupport () {
		return support;
	}
	
	public String displayText() {
		final Memory mem = cpu.getMemory();
		final StringBuilder sb = new StringBuilder();
		final int system = mem.getSystem();
		
		for (int n = 0; n < 8; n++) {
			int w = mem.loadWord(system + ASCIIPOS + (n * 8)) & 0xff;
			sb.append(w != 0 ? (char) w : " ");
		}
		
		return sb.toString();
	}
	
}
