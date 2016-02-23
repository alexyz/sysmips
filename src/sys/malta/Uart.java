package sys.malta;

import sys.mips.Cpu;
import sys.util.Logger;
import sys.util.Symbols;

/**
 * 16550A emulation
 */
public class Uart implements Device {

	/** receive (write) / transmit (read) */
	public static final int M_COM1_RX_TX = 0;
	/** interrupt enable */
	public static final int M_COM1_IER = 1;
	/**
	 * fifo control register (writes) / interrupt identification register
	 * (reads) / extended features register (not supported)
	 */
	public static final int M_COM1_FCR_IIR_EFR = 2;
	/** line control register */
	public static final int M_COM1_LCR = 3;
	/** modem control register */
	public static final int M_COM1_MCR = 4;
	/** line status register */
	public static final int M_COM1_LSR = 5;
	/** modem status register */
	public static final int M_COM1_MSR = 6;
	
	/** enables both the XMIT and RCVR FIFOs */
	public static final int FCR_ENABLE_FIFO = 1;
	/** clears all bytes in the RCVR FIFO */
	public static final int FCR_CLEAR_RCVR = 2;
	/** clears all bytes in the XMIT FIFO */
	public static final int FCR_CLEAR_XMIT = 3;
	
	private static final Logger log = new Logger(Display.class);
	
	private final String name;
	private final StringBuilder consoleSb = new StringBuilder();
	
	private int offset;
	private byte ier;
	private byte mcr;
	private byte lcr;
	private byte iir;
	
	public Uart(String name) {
		this.name = name;
	}
	
	@Override
	public void init (Symbols sym, int offset) {
		log.println("init display at " + Integer.toHexString(offset));
		this.offset = offset;
		sym.init(Display.class, "M_", "M_" + name, offset, 1);
	}
	
	@Override
	public boolean isMapped (int addr) {
		return addr >= 0 && addr <= 7;
	}

	@Override
	public int systemRead (int addr, int size) {
		switch (addr) {
			case M_COM1_LSR:
				// always ready?
				return 0x20;
			case M_COM1_IER:
				return ier;
			case M_COM1_MCR:
				return mcr;
			case M_COM1_LCR:
				return lcr;
			case M_COM1_FCR_IIR_EFR:
				return iir;
			default:
				throw new RuntimeException("unknown uart read");
		}
	}

	@Override
	public void systemWrite (int addr, int value, int size) {
		switch (addr) {
			case M_COM1_RX_TX:
				consoleWrite(value & 0xff);
				return;
				
			case M_COM1_IER:
				log.println("set com1 ier %x", value);
				// we only want bottom 4 bits, linux might set more to autodetect other chips
				ier = (byte) (value & 0xf);
				return;
				
			case M_COM1_MCR:
				log.println("set com1 mcr %x", value);
				ier = (byte) value;
				return;
				
			case M_COM1_LCR:
				log.println("set com1 lcr %x", value);
				lcr = (byte) value;
				return;
				
			case M_COM1_FCR_IIR_EFR:
				log.println("set com1 fcr %x", value);
				if (value == FCR_ENABLE_FIFO) {
					// this will call autoconfig_16550a
					iir |= 0b1100_0000;
				}
				return;
			default:
				throw new RuntimeException("unknown uart write " + Cpu.getInstance().getMemory().getSymbols().getNameAddrOffset(offset + addr));
		}
	}


	private void consoleWrite (int value) {
		if ((value >= 32 && value < 127) || value == '\n') {
			consoleSb.append((char) value);
		} else if (value != '\r') {
			consoleSb.append("{" + Integer.toHexString(value) + "}");
		}
		if (value == '\n' || consoleSb.length() > 160) {
			final String line = consoleSb.toString();
			log.println("console: " + line.trim());
			if (line.contains("WARNING")) {
				log.println("calls=" + Cpu.getInstance().getCalls().callString());
			}
			Cpu.getInstance().getSupport().firePropertyChange("console", null, line);
			consoleSb.delete(0, consoleSb.length());
		}
	}
	
}
