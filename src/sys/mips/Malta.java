package sys.mips;

import java.beans.PropertyChangeSupport;

public class Malta implements SystemListener {
	
	public static final int M_SDRAM = 0x0;
	public static final int M_PCI1 = 0x0800_0000;
	// TODO not sure about this, connected with value of GT_PCI0IOLD
	public static final int M_IOBASE = 0x1000_0000;
	public static final int M_DMA2_MASK_REG = M_IOBASE + 0xD4;
	public static final int M_PORT = M_IOBASE + 0x03f8;
	public static final int M_UART_TX = M_PORT;
	public static final int M_UART_LSR = M_PORT + 5;
	public static final int M_PCI2 = 0x1800_0000;
	public static final int M_GTBASE = 0x1be0_0000;
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
	
	private final Cpu cpu = new Cpu();
	private final PropertyChangeSupport support = new PropertyChangeSupport(this);
	private final StringBuilder consoleSb = new StringBuilder();
	private final CpuLogger log = cpu.getLog();
	private final GT gt = new GT(cpu, M_GTBASE);
	
	public Malta () {
		// initialise the memory for linux and malta
		final Memory mem = cpu.getMemory();
		mem.setSystem(SYSTEM);
		for (int n = 0; n < 16; n++) {
			mem.initPage(LINUX + 0x100000 * n);
		}
		mem.initPage(SYSTEM + M_SDRAM);
		mem.initPage(SYSTEM + M_IOBASE);
		mem.initPage(SYSTEM + M_DEVICES);
		mem.initPage(SYSTEM + M_FLASH2);
		mem.initPage(SYSTEM + M_GTBASE);
		
		final Symbols sym = mem.getSymbols();
		sym.put(SYSTEM + M_SDRAM, "M_SDRAM");
		sym.put(SYSTEM + M_PCI1, "M_PCI1");
		sym.put(SYSTEM + M_IOBASE, "M_IOBASE");
		sym.put(SYSTEM + M_DMA2_MASK_REG, "M_DMA2_MASK_REG");
		sym.put(SYSTEM + M_PORT, "M_PORT", 65536);
		sym.put(SYSTEM + M_UART_LSR, "M_UART_LSR", 1);
		sym.put(SYSTEM + M_PCI2, "M_PCI2");
		sym.put(SYSTEM + M_GTBASE, "M_GTBASE");
		sym.put(SYSTEM + M_FLASH1, "M_FLASH1");
		sym.put(SYSTEM + M_RESERVED, "M_RESERVED");
		sym.put(SYSTEM + M_DEVICES, "M_DEVICES");
		sym.put(SYSTEM + M_DISPLAY, "M_DISPLAY");
		sym.put(SYSTEM + M_SCSPEC1, "M_SCSPEC1");
		sym.put(SYSTEM + M_SCSPEC2, "M_SCSPEC2");
		sym.put(SYSTEM + M_SCSPEC2_BONITO, "M_SCSPEC2_BONITO");
		sym.put(SYSTEM + M_UNUSED, "M_UNUSED");
		sym.put(SYSTEM + M_FLASH2, "M_FLASH2");
		sym.put(SYSTEM + M_REVISION, "M_REVISION", 8);
		                 
		mem.storeWordSystem(M_REVISION, 1);
		mem.storeByteSystem(M_UART_LSR, (byte) 0x20);
		
		gt.init();
		
		mem.setSystemListener(this);
	}
	
	public Cpu getCpu () {
		return cpu;
	}
	
	@Override
	public void systemRead (int addr, int value) {
		log.debug("system read " + cpu.getMemory().getSymbols().getName(SYSTEM + addr) + " => " + Integer.toHexString(value));
		switch (addr) {
			case M_UART_LSR:
			case M_REVISION:
				break;
			default:
				if (gt.read(addr)) {
					break;
				}
				throw new RuntimeException("unknown malta read " + cpu.getMemory().getSymbols().getName(SYSTEM + addr));
		}
	}
	
	@Override
	public void systemWrite (int addr, int value) {
		switch (addr) {
			case M_DMA2_MASK_REG:
				// information in asm/dma.h
				log.info("enable dma channel 4+" + value);
				break;
			case M_UART_TX:
				consoleWrite(value);
				break;
			default:
				if (addr >= M_DISPLAY && addr < M_DISPLAY + 0x100) {
					support.firePropertyChange("display", null, displayText());
				} else if (gt.write(addr)) {
					//
				} else {
					throw new RuntimeException("unknown system write " + cpu.getMemory().getSymbols().getName(SYSTEM + addr) + " <= " + Integer.toHexString(value));
				}
		}
	}

	private void consoleWrite (int value) {
		if ((value >= 32 && value < 127) || value == '\n') {
			consoleSb.append((char) value);
		} else if (value != '\r') {
			consoleSb.append("{" + Integer.toHexString(value) + "}");
		}
		if (value == '\n' || consoleSb.length() > 80) {
			support.firePropertyChange("console", null, consoleSb.toString());
			consoleSb.delete(0, consoleSb.length());
		}
	}
	
	public PropertyChangeSupport getSupport () {
		return support;
	}
	
	public String displayText() {
		final Memory mem = cpu.getMemory();
		final StringBuilder sb = new StringBuilder();
		final int leds = mem.loadWordSystem(M_DISPLAY_LEDBAR) & 0xff;
		sb.append(Integer.toBinaryString(leds)).append(" ");
		final int word = mem.loadWordSystem(M_DISPLAY_ASCIIWORD);
		sb.append(Integer.toHexString(word)).append(" ");
		for (int n = 0; n < 8; n++) {
			int w = mem.loadWordSystem(M_DISPLAY_ASCIIPOS + (n * 8)) & 0xff;
			sb.append(w != 0 ? (char) w : ' ');
		}
		return sb.toString();
	}
	
}
