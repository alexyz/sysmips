package sys.malta;

import sys.mips.Cpu;
import sys.util.Logger;
import sys.util.Symbols;

import static sys.malta.UartUtil.*;

/**
 * 16550A emulation
 * COM port detection - 8250.c - autoconfig()
 */
public class Uart implements Device {
	
	private final Logger log;
	private final int baseAddr;
	private final String name;
	private final StringBuilder consoleSb = new StringBuilder();
	private final byte[] rxFifo = new byte[16];
	private final boolean console;
	
	private int ier;
	private int mcr;
	private int lcr;
	private int iir;
	private int lsr;
	private int rxRead;
	private int rxWrite;
	
	public Uart(final int baseAddr, final String name, final boolean console) {
		this.log = new Logger(name);
		this.baseAddr = baseAddr;
		this.name = name;
		this.console = console;
		this.lsr |= LSR_THRE;
	}
	
	@Override
	public void init (final Symbols sym) {
		log.println("init uart " + name + " at " + Integer.toHexString(baseAddr));
		sym.init(UartUtil.class, "M_", "M_" + name + "_", baseAddr, 1);
	}
	
	@Override
	public boolean isMapped (final int addr) {
		// can't compare addr and baseAddr directly due to signed values
		final int offset = addr - baseAddr;
		return offset >= 0 && offset < 8;
	}
	
	@Override
	public int systemRead (final int addr, final int size) {
		if (size != 1) {
			throw new RuntimeException();
		}
		
		final int offset = addr - baseAddr;
		
		switch (offset) {
			case M_RX_TX:
				// should probably check if fifo enabled first
				if (rxRead == rxWrite) {
					log.println("uart rx underrun");
					return 0;
				} else {
					final byte x = rxFifo[rxRead];
					rxRead = (rxRead + 1) & 0xf;
					final int rem = ((rxWrite + 16) - rxRead) & 0xf;
					if (rem == 0) {
						// reset ready bit
						lsr &= ~LSR_DR;
					}
					log.println("uart receiver buffer read %x remaining %d", x, rem);
					return x;
				}
			case M_LSR: {
				final int x = lsr;
				//log.println("uart read lsr %x", x);
				// reset overrun bit on read
				lsr &= ~LSR_OE;
				return x;
			}
			case M_IER:
				log.println("uart read ier %x", ier);
				return ier;
			case M_MCR:
				log.println("uart read mcr %x", mcr);
				return mcr;
			case M_LCR:
				log.println("uart read lcr %x", lcr);
				return lcr;
			case M_FCR_IIR_EFR:
				log.println("uart read iir %x", iir);
				return iir;
			default:
				throw new RuntimeException("unknown uart read " + offset);
		}
	}
	
	@Override
	public void systemWrite (final int addr, final int valueInt, final int size) {
		if (size != 1) {
			throw new RuntimeException();
		}
		
		final int offset = addr - baseAddr;
		final byte value = (byte) valueInt;
		
		switch (offset) {
			case M_RX_TX:
				if ((mcr & MCR_LOOPBACK) != 0) {
					// this is a guess...
					// should trigger interrupt at some point?
					final int i = (rxWrite + 1) & 0xf;
					if (i != rxRead) {
						rxFifo[rxWrite] = value;
						rxWrite = i;
						// set ready bit
						lsr |= LSR_DR;
					} else {
						// set overrun bit
						//log.println("uart tx overrun");
						lsr |= LSR_OE;
					}
				} else {
					if (console) {
						consoleWrite(value);
					} else {
						log.println("write " + Integer.toHexString(value & 0xff));
					}
				}
				return;
				
			case M_IER: {
				final boolean ms = (value & IER_MSI) != 0;
				final boolean rda = (value & IER_RDAI) != 0;
				final boolean rls = (value & IER_RLSI) != 0;
				final boolean thre = (value & IER_THREI) != 0;
				log.println("set %s ier %x =%s%s%s%s",
						name, value, ms?" modem-status":"", rda?" received-data-available":"",
								rls?" received-line-status":"", thre?" transmitter-holding-register-empty":"");
				// we only want bottom 4 bits, linux might set more to autodetect other chips
				ier = (byte) (value & 0xf);
				return;
			}
			case M_MCR: {
				mcr = value;
				final boolean dtr = (value & MCR_DTR) != 0;
				final boolean rts = (value & MCR_RTS) != 0;
				final boolean out1 = (value & MCR_OUTPUT1) != 0;
				final boolean out2 = (value & MCR_OUTPUT2) != 0;
				final boolean loop = (value & MCR_LOOPBACK) != 0;
				log.println("set %s mcr %x =%s%s%s%s%s",
						name, value, dtr ? " dtr" : "", rts ? " rts" : "", out1 ? " out1" : "", out2 ? " out2" : "", loop ? " loopback" : "");
				return;
			}
			case M_LCR: {
				lcr = value;
				final int w = lcrWordLength(value);
				final float s = lcrStopBits(value);
				final char p = lcrParity(value);
				final boolean br = (value & LCR_BREAK) != 0;
				final boolean dl = (value & LCR_DLAB) != 0;
				log.println("set %s lcr %x = %d-%s-%.1f%s%s",
						name, value, w, p, s, br ? " break" : "", dl ? " dlab" : "");
				return;
			}
			case M_FCR_IIR_EFR: {
				final boolean en = (value & FCR_ENABLE_FIFO) != 0;
				final boolean cr = (value & FCR_CLEAR_RCVR) != 0;
				final boolean cx = (value & FCR_CLEAR_XMIT) != 0;
				if (en) {
					// this will call autoconfig_16550a
					iir |= IIR_FIFO;
				}
				if (!en || cr) {
					// clear the receive fifo
					rxRead = 0;
					rxWrite = 0;
				}
				log.println("set %s fcr %x =%s%s%s",
						name, value, en ? " enable-fifo" : "", cr ? " clear-rcvr" : "", cx ? " clear-xmit" : "");
				return;
			}
			default:
				throw new RuntimeException("unknown uart write " + Cpu.getInstance().getMemory().getSymbols().getNameAddrOffset(addr));
		}
	}
	
	private void consoleWrite (final byte value) {
		if (value >= 32 || value == '\n') {
			consoleSb.append((char) value);
		} else if (value != '\r') {
			consoleSb.append("{" + Integer.toHexString(value & 0xff) + "}");
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
