package sys.malta;

import sys.mips.Cpu;
import sys.util.Logger;
import sys.util.Symbols;

/**
 * 16550A emulation
 */
public class Uart implements Device {

	private static final Logger log = new Logger("Uart");
	
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
	
	private static int lcrWordLength(int x) {
		return (x & 0x3) + 5;
	}
	
	private static float lcrStopBits(int x) {
		if ((x & 0x4) == 0) {
			return 1;
		} else {
			if ((x & 0x3) == 0) {
				return 1.5f;
			} else {
				return 2;
			}
		}
	}
	
	private static char lcrParity(int x) {
		if ((x & 0x8) == 0) {
			return 'N';
		} else switch ((x & 0x30) >> 4) {
			case 0: return 'O';
			case 1: return 'E';
			case 2: return 'H';
			case 3: return 'L';
			default: throw new RuntimeException();
		}
	}
	
	private static boolean lcrBreak(int x) {
		return (x & 0x40) != 0;
	}
	
	private static boolean lcrDivisorLatch(int x) {
		return (x & 0x80) != 0;
	}
	
	private static boolean mcrForceDataTerminalReady(int x) {
		return (x & 0x1) != 0;
	}
	
	private static boolean mcrForceRequestToSend(int x) {
		return (x & 0x2) != 0;
	}
	
	private static boolean mcrOutput1(int x) {
		return (x & 0x4) != 0;
	}
	
	// enable uart interrupts?
	private static boolean mcrOutput2(int x) {
		return (x & 0x8) != 0;
	}
	
	private static boolean mcrLoopback(int x) {
		return (x & 0x10) != 0;
	}
	
	private static boolean fcrEnableFifo (int value) {
		return (value & 0x1) != 0;
	}
	
	private static boolean fcrClearReceiveFifo (int value) {
		return (value & 0x2) != 0;
	}

	private static boolean fcrClearTransmitFifo (int value) {
		return (value & 0x4) != 0;
	}
	
	private final int baseAddr;
	private final String name;
	private final StringBuilder consoleSb = new StringBuilder();
	
	private int ier;
	private int mcr;
	private int lcr;
	private int iir;
	
	public Uart(int baseAddr, String name) {
		this.baseAddr = baseAddr;
		this.name = name;
	}
	
	@Override
	public void init (Symbols sym) {
		log.println("init display at " + Integer.toHexString(baseAddr));
		sym.init(Display.class, "M_", "M_" + name, baseAddr, 1);
	}
	
	@Override
	public boolean isMapped (int addr) {
		return addr >= baseAddr && addr <= baseAddr + 7;
	}

	@Override
	public int systemRead (int addr, int size) {
		int offset = addr - baseAddr;
		
		switch (offset) {
			case M_COM1_RX_TX:
				return 0;
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
				throw new RuntimeException("unknown uart read " + offset);
		}
	}

	@Override
	public void systemWrite (int addr, int value, int size) {
		int offset = addr - baseAddr;
		if (size != 1) {
			throw new RuntimeException();
		}
		value = value & 0xff;
		
		switch (offset) {
			case M_COM1_RX_TX:
				if (mcrLoopback(mcr)) {
					// do these go into the xmit fifo?
					throw new RuntimeException("loopback");
				} else {
					consoleWrite(value);
				}
				return;
				
			case M_COM1_IER:
				log.println("set com1 ier %x", value);
				// we only want bottom 4 bits, linux might set more to autodetect other chips
				ier = (byte) (value & 0xf);
				return;
				
			case M_COM1_MCR: {
				mcr = (byte) value;
				boolean dtr = mcrForceDataTerminalReady(value);
				boolean rts = mcrForceRequestToSend(value);
				boolean out1 = mcrOutput1(value);
				boolean out2 = mcrOutput2(value);
				boolean loop = mcrLoopback(value);
				log.println("set com1 mcr %s %s %s %s %s",
						dtr ? "dtr" : "", rts ? "rts" : "", out1 ? "out1" : "", out2 ? "out2" : "", loop ? "loopback" : "");
				return;
			}
			case M_COM1_LCR: {
				lcr = (byte) value;
				int w = lcrWordLength(value);
				float s = lcrStopBits(value);
				char p = lcrParity(value);
				boolean br = lcrBreak(value);
				boolean dl = lcrDivisorLatch(value);
				log.println("set com1 lcr %d-%s-%.1f %s %s",
						w, p, s, br ? "break" : "", dl ? "dlab" : "");
				return;
			}
			case M_COM1_FCR_IIR_EFR: {
				boolean en = fcrEnableFifo(value);
				boolean cr = fcrClearReceiveFifo(value);
				boolean cx = fcrClearTransmitFifo(value);
				if (en) {
					// this will call autoconfig_16550a
					iir |= 0b1100_0000;
				}
				log.println("set com1 fcr %s %s %s",
						en ? "enable-fifo" : "", cr ? "clear-rcvr" : "", cx ? "clear-xmit" : "");
				return;
			}
			default:
				throw new RuntimeException("unknown uart write " + Cpu.getInstance().getMemory().getSymbols().getNameAddrOffset(addr));
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
			log.println(line.trim());
			if (line.contains("WARNING")) {
				log.println("calls=" + Cpu.getInstance().getCalls().callString());
			}
			Cpu.getInstance().getSupport().firePropertyChange("console", null, line);
			consoleSb.delete(0, consoleSb.length());
		}
	}
	
}
