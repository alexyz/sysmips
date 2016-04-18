package sys.malta;

import sys.util.Logger;
import sys.util.Symbols;

/**
 * PIIX4 style 82C59 interrupt controller
 */
public class PIC implements Device {
	
	/** Init Command Word 1, Operational Command Word 2 and 3 */
	public static final int M_CMD = 0;
	/** Init Command Word 2, 3 and 4, Operational Command Word 1 */
	public static final int M_DATA = 1;
	
	private final Logger log;
	private final int baseAddr;
	private final boolean master;
	
	private int imr;
	private int irr;
	private int isr;
	private int icw;
	private int ivba;
	private boolean cascade;
	private boolean nested;
	private boolean buf;
	private boolean autoend;
	private boolean micro;
	
	public PIC(final int baseAddr, boolean master) {
		this.baseAddr = baseAddr;
		this.master = master;
		this.log = new Logger("PIC" + (master ? 1 : 2));
	}
	
	@Override
	public void init (final Symbols sym) {
		sym.init(getClass(), "M_", "M_PIC" + (master ? 1 : 2), baseAddr, 1);
	}
	
	@Override
	public boolean isMapped (final int addr) {
		final int offset = addr - baseAddr;
		return offset >= 0 && offset < 2;
	}
	
	@Override
	public int systemRead (final int addr, final int size) {
		final int offset = addr - baseAddr;
		
		switch (offset) {
			case M_CMD:
				log.println("read command");
				throw new RuntimeException();
				
			case M_DATA:
				log.println("read imr %x", imr);
				return imr;
				
			default:
				throw new RuntimeException();
		}
	}
	
	@Override
	public void systemWrite (final int addr, final int size, final int valueInt) {
		final int offset = addr - baseAddr;
		final int value = valueInt & 0xff;
		
		switch (offset) {
			case M_CMD:
				writeCommand(value);
				return;
			case M_DATA:
				writeData(value);
				return;
			default:
				throw new RuntimeException();
		}
	}

	private void writeCommand (final int value) {
		log.println("write command %x", value);
		
		if ((value & 0x10) != 0) {
			if (value == 0x11) {
				log.println("pic init command word 1");
				imr = 0;
				icw = 1;
				// expect 3 more command words...
				return;
			} else {
				throw new RuntimeException("unexpected icw1");
			}
			
		} else if ((value & 0x8) == 0) {
			log.println("pic operation control word 2");
			throw new RuntimeException();
			
		} else {
			log.println("pic operation control word 3");
			throw new RuntimeException();
		}
	}

	private void writeData (final int value) {
		log.println("write data %x", value);
		
		switch (icw) {
			case 0:
				log.println("write operation command word 1: %x", value);
				imr = value;
				return;
				
			case 1:
				log.println("write init command word 2: %x", value);
				ivba = value >> 3;
				icw++;
				return;
				
			case 2:
				log.println("write init command word 3: %x", value);
				if (master) {
					cascade = (value & 0x4) != 0;
					log.println("cascade: " + cascade);
				}
				icw++;
				return;
				
			case 3:
				log.println("write init command word 4: %x", value);
				nested = (value & 0x10) != 0;
				buf = (value & 0x8) != 0;
				autoend = (value & 0x2) != 0;
				micro = (value & 0x1) != 0;
				log.println("nested: " + nested + " buffered: " + buf + " autoend: " + autoend + " micro: " + micro);
				icw = 0;
				return;
				
			default:
				throw new RuntimeException("unknown icw " + icw);
		}
	}
	
}
