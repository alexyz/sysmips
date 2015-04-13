package sys.mips;

import java.beans.PropertyChangeSupport;

public class Malta implements SystemListener {
	
	public static final int M_SDRAM = 0x0;
	public static final int M_PORT = 0x03f8;
	public static final int M_UART_TX = M_PORT;
	public static final int M_UART_LSR = M_PORT + 5;
	public static final int M_PCI1 = 0x0800_0000;
	public static final int M_PCI2 = 0x1800_0000;
	public static final int M_GTBASE = 0x1be0_0000;
	public static final int M_GT_PCI0_CMD = M_GTBASE + 0xc00;
	public static final int M_GT_PCI0IOLD = M_GTBASE + 0x048;
	public static final int M_GT_PCI0IOREMAP = M_GTBASE + 0x0f0;
	public static final int M_UNUSED = 0x1c00_0000;
	public static final int M_FLASH1 = 0x1e00_0000;
	public static final int M_RESERVED = 0x1e40_0000;
	public static final int M_DEVICES = 0x1f00_0000;
	public static final int M_DISPLAY = 0x1f00_0400;
	public static final int M_DISPLAY_LEDBAR = M_DISPLAY + 0x8;
	public static final int M_DISPLAY_ASCIIWORD = M_DISPLAY + 0x10;
	public static final int M_DISPLAY_ASCIIPOS = M_DISPLAY + 0x18;
	public static final int M_SCSPEC1 = 0x1f10_0010;
	public static final int M_FLASH2 = 0x1fc0_0000;
	public static final int M_REVISION = 0x1fc0_0010;
	public static final int M_SCSPEC2 = 0x1fd0_0010;
	public static final int M_SCSPEC2_BONITO = 0x1fe0_0010;
	
	// not sure where these come from
	public static final int LINUX = 0x8000_0000;
	public static final int SYSTEM = 0xa000_0000;
	public static final int CORE_LV = 1;
	
	private final Cpu cpu = new Cpu();
	private final PropertyChangeSupport support = new PropertyChangeSupport(this);
	private final StringBuilder consoleSb = new StringBuilder();
	
	public Malta () {
		// initialise the memory for linux and malta
		final Memory mem = cpu.getMemory();
		for (int n = 0; n < 16; n++) {
			mem.initPage(LINUX + 0x100000 * n);
		}
		mem.initPage(SYSTEM + M_SDRAM);
		mem.initPage(SYSTEM + M_DEVICES);
		mem.initPage(SYSTEM + M_FLASH2);
		mem.initPage(SYSTEM + M_GTBASE);
		
		final Symbols sym = mem.getSymbols();
		sym.put(SYSTEM + M_DEVICES, "M_DEVICES");
		sym.put(SYSTEM + M_DISPLAY, "M_DISPLAY");
		sym.put(SYSTEM + M_FLASH1, "M_FLASH1");
		sym.put(SYSTEM + M_FLASH2, "M_FLASH2");
		sym.put(SYSTEM + M_PCI1, "M_PCI1");
		sym.put(SYSTEM + M_PCI2, "M_PCI2");
		sym.put(SYSTEM + M_RESERVED, "M_RESERVED");
		sym.put(SYSTEM + M_GTBASE, "M_GTBASE");
		sym.put(SYSTEM + M_GT_PCI0_CMD, "M_GT_PCI0_CMD", 8);
		sym.put(SYSTEM + M_GT_PCI0IOLD, "M_GT_PCI0IOLD", 8);
		sym.put(SYSTEM + M_SCSPEC1, "M_SCSPEC1");
		sym.put(SYSTEM + M_SCSPEC2, "M_SCSPEC2");
		sym.put(SYSTEM + M_SCSPEC2_BONITO, "M_SCSPEC2_BONITO");
		sym.put(SYSTEM + M_SDRAM, "M_SDRAM");
		sym.put(SYSTEM + M_UNUSED, "M_UNUSED");
		sym.put(SYSTEM + M_REVISION, "M_REVISION", 8);
		sym.put(SYSTEM + M_GT_PCI0IOREMAP, "M_GT_PCI0IOREMAP", 8);
		sym.put(SYSTEM + M_PORT, "M_PORT", 65536);
		sym.put(SYSTEM + M_UART_LSR, "M_UART_LSR", 1);
		
		mem.setSystem(SYSTEM);
		// set the system controller revision
		mem.storeWordSystem(M_REVISION, CORE_LV);
		// allow io
		// UART_LSR_THRE
		mem.storeByteSystem(M_UART_LSR, (byte) 0x20);
		mem.setSystemListener(this);
	}
	
	public Cpu getCpu () {
		return cpu;
	}
	
	@Override
	public void systemRead (int addr, int value) {
		System.out.println("system read " + cpu.getMemory().getSymbols().getName(SYSTEM + addr) + " => " + Integer.toHexString(value));
		switch (addr) {
			case M_REVISION:
			case M_GT_PCI0IOLD:
			case M_GT_PCI0_CMD:
			case M_GT_PCI0IOREMAP:
			case M_UART_LSR:
				break;
			default:
				throw new RuntimeException("unknown malta read " + Integer.toHexString(addr));
		}
	}
	
	@Override
	public void systemWrite (int addr, int value) {
		System.out.println("system write " + cpu.getMemory().getSymbols().getName(SYSTEM + addr) + " <= " + Integer.toHexString(value));
		switch (addr) {
			case M_GT_PCI0_CMD:
				System.out.println("ignore pci command " + value);
				break;
			case M_UART_TX:
				if (value >= 32) {
					consoleSb.append((char) value);
				} else {
					// TODO flush to ui...
					throw new RuntimeException("console=" + consoleSb);
				}
				break;
			default:
				if (addr >= M_DISPLAY && addr < M_DISPLAY + 0x100) {
					support.firePropertyChange("display", "", displayText());
				} else {
					throw new RuntimeException("unknown malta write " + Integer.toHexString(addr) + ", " + Integer.toHexString(value));
				}
		}
	}
	
	public PropertyChangeSupport getSupport () {
		return support;
	}
	
	public String displayText() {
		final Memory mem = cpu.getMemory();
		final StringBuilder sb = new StringBuilder();
		// should display leds
		for (int n = 0; n < 8; n++) {
			int w = mem.loadWordSystem(M_DISPLAY_ASCIIPOS + (n * 8)) & 0xff;
			sb.append(w != 0 ? (char) w : ' ');
		}
		
		return sb.toString();
	}
	
}
