package sys.mips;

import java.beans.PropertyChangeSupport;
import java.io.RandomAccessFile;

import sys.elf.*;

public class Malta implements SystemListener {
	
	// mips malta chapter 4 - memory map
	public static final int DEVICES = 0x1f00_0000;
	public static final int DISPLAY = 0x1f00_0400;
	public static final int DISPLAY_LEDBAR = DISPLAY + 0x8;
	public static final int DISPLAY_ASCIIWORD = DISPLAY + 0x10;
	public static final int DISPLAY_ASCIIPOS = DISPLAY + 0x18;
	public static final int REVISION = 0x1FC0_0010;
	
	// not sure where these come from
	public static final int LINUX = 0x8000_0000;
	public static final int SYSTEM = 0xa000_0000;
	public static final int CORE_LV  = 1;
	
	private final Cpu cpu = new Cpu();
	private final PropertyChangeSupport support = new PropertyChangeSupport(this);
	
	public Malta () {
		// initialise the memory for linux and malta
		final Memory mem = cpu.getMemory();
		for (int n = 0; n < 16; n++) {
			mem.init(LINUX + 0x100000 * n);
		}
		for (int n = 0; n < 16; n++) {
			mem.init(SYSTEM + DEVICES + 0x100000 * n);
		}
		mem.storeWord(SYSTEM + REVISION, CORE_LV);
		mem.setSystem(SYSTEM);
		mem.setSystemListener(this);
	}

	public void load (ELF32 elf, RandomAccessFile file) throws Exception {
		for (int ph = 0; ph < elf.header.programHeaders; ph++) {
			ELF32Program program = elf.programs[ph];
			if (program.type == ELF32Program.PT_LOAD) {
				file.seek(program.fileOffset);
				final byte[] data = new byte[program.memorySize];
				file.read(data, 0, program.fileSize);
				cpu.getMemory().storeBytes(program.virtualAddress, data);
			}
		}
		
		for (ELF32Symbol symbol : elf.symbols) {
			if (symbol.getBind() == ELF32Symbol.STB_GLOBAL) {
				cpu.getMemory().getSymbols().getMap().put(symbol.valueAddress & 0xffffffffL, symbol.name);
			}
		}
		
		cpu.setPc(elf.header.entryAddress);
	}
	
	public Cpu getCpu () {
		return cpu;
	}
	
	@Override
	public void update (int addr, int value) {
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
		// TODO display leds as bullets
		for (int n = 0; n < 8; n++) {
			int w = mem.loadWord(system + DISPLAY_ASCIIPOS + (n * 8)) & 0xff;
			sb.append(w != 0 ? (char) w : " ");
		}
		
		return sb.toString();
	}

}
