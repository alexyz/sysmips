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
	
	private int icw1;
	private int icw2;
	private int icw3;
	private int icw4;
	private int ocw1;
	private int ocw2;
	private int ocw3;
	private int init;
	
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
				log.println("read ocw1 %x", ocw1);
				return ocw1;
				
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
			log.println("pic init command word 1 %x", value);
			boolean needed = (value & 0x1) != 0;
			boolean single = (value & 0x2) != 0;
			boolean adi = (value & 0x4) != 0;
			boolean ltim = (value & 0x8) != 0;
			log.println("icw4needed: " + needed + " single: " + single + " adi: " + adi + " ltim: " + ltim);
			// expect more command words...
			init = 1;
			return;
			
		} else if ((value & 0x8) == 0) {
			log.println("pic operation control word 2: %x", value);
			ocw2 = value;
			
		} else {
			log.println("pic operation control word 3: %x", value);
			ocw3 = value;
		}
	}

	private void writeData (final int value) {
		log.println("write data %x", value);
		
		switch (init) {
			case 0:
				log.println("write operation command word 1: %x", value);
				// interrupt mask
				ocw1 = value;
				return;
				
			case 1:
				log.println("write init command word 2: %x", value);
				icw2 = value;
				init++;
				return;
				
			case 2:
				log.println("write init command word 3: %x", value);
				icw3 = value;
				if (master) {
					boolean cascade = (value & 0x4) != 0;
					log.println("cascade: " + cascade);
				}
				init = (icw1 & 0x1) != 0 ? init+1 : 0;
				return;
				
			case 3: {
				log.println("write init command word 4: %x", value);
				icw4 = value;
				boolean nested = (value & 0x10) != 0;
				boolean buf = (value & 0x8) != 0;
				boolean autoend = (value & 0x2) != 0;
				boolean pm = (value & 0x1) != 0;
				log.println("nested: " + nested + " buffered: " + buf + " autoend: " + autoend + " 8086: " + pm);
				init = 0;
				return;
			}
				
			default:
				throw new RuntimeException("unknown init " + init);
		}
	}
	
}
