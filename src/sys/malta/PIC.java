package sys.malta;

import sys.mips.Device;
import sys.util.Logger;
import sys.util.Symbols;

/**
 * PIIX4 style 82C59 interrupt controller
 * TODO this really needs to intercept calls to addException...
 * device.addexception() -> ?
 */
public class PIC extends Device {
	
	/** Address 0: Init Command Word 1, Operational Command Word 2 and 3 */
	public static final int M_CMD = 0;
	/** Address 1: Init Command Word 2, 3 and 4, Operational Command Word 1 */
	public static final int M_DATA = 1;
	
	/** init icw1 */
	private static final int CMD_ICW1 = 0x10;
	/** init ocw3 or ocw2 */
	private static final int CMD_OCW3 = 0x8;
	
	/** icw4 needed */
	private static final int ICW1_ICW4NEEDED = 0x1;
	/** single/cascade */
	private static final int ICW1_SINGLE = 0x2;
	/** address interval 4/8 */
	private static final int ICW1_ADI4 = 0x4;
	/** level triggered mode */
	private static final int ICW1_LTM = 0x8;
	/** a5-a7 of interrupt vector address (MCS mode) */
	private static final int ICW1_IVA5 = 0xe0;
	/** 8086/MCS mode */
	private static final int ICW4_8086MODE = 0x1;
	/** auto eoi/normal eoi */
	private static final int ICW4_AUTOEOI = 0x2;
	/** buffered mode */
	private static final int ICW4_BUFFER = 0xc;
	/** fully nested mode */
	private static final int ICW4_NESTED = 0x10;
	
	public static void main (String[] args) {
		// write command words, init words
		// test interrupt masking/mapping/cascading...
		PIC dev = new PIC(0, true);
		//dev.systemWrite();
	}
	
	private final Logger log;
	private final boolean master;
	
	/** 
	 * Address 0
	 */
	private int icw1;
	
	/**
	 * Address 1
	 * 0-7: a8-a15 of interrupt vector address (MCS mode)
	 * 3-7: t3-t7 of interrupt vector address (8086 mode)
	 */
	private int icw2;
	
	/**
	 * Address 1
	 * 0-7: has slave (master)
	 * 0-3: slave id (slave)
	 */
	private int icw3;
	
	/**
	 * Address 1
	 */
	private int icw4;
	
	/**
	 * the interrupt mask register
	 * Address 1
	 * 0-7: interrupt mask set
	 */
	private int ocw1;
	
	/**
	 * Address 0
	 * 0-3: interrupt request level
	 * 5-7: end of interrupt mode
	 */
	private int ocw2;
	
	/**
	 * Address 0
	 * 0,1: read register cmd
	 * 2: poll command
	 * 5,6: special mask mode
	 */
	private int ocw3;
	
	/** data mode: 0 = imr, 2 = icw2, 3 = icw3, 4 = icw4 */
	private int init;
	
	public PIC(final int baseAddr, boolean master) {
		super(baseAddr);
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
	public byte loadByte (final int addr) {
		final int offset = addr - baseAddr;
		
		switch (offset) {
			case M_CMD:
				log.println("read command");
				throw new RuntimeException();
				
			case M_DATA:
				//log.println("read ocw1 %x", ocw1);
				// read the imr
				return (byte) ocw1;
				
			default:
				throw new RuntimeException();
		}
	}
	
	@Override
	public void storeByte (final int addr, final byte value) {
		final int offset = addr - baseAddr;
		
		switch (offset) {
			case M_CMD:
				writeCommand(value & 0xff);
				return;
			case M_DATA:
				writeData(value & 0xff);
				return;
			default:
				throw new RuntimeException();
		}
	}

	/** write address 0 */
	private void writeCommand (final int value) {
		//log.println("write command %x", value);
		
		if ((value & CMD_ICW1) != 0) {
			log.println("write ICW1 %x (was %x)", value, icw1);
			icw1 = value;
			// clear imr
			ocw1 = 0;
			// IR7 input assigned priority 7?
			// XXX ocw2 = 0x7?
			// slave mode address set to 7
			// XXX icw3 = 0x7?
			// special mask mode cleared
			ocw3 &= ~0x60;
			// status read set to IRR?
			// IC4 cleared if 0
			if ((value & 0x1) != 0) {
				icw4 = 0;
			}
			// expect ICW2...
			init = 2;
			return;
			
		} else if ((value & CMD_OCW3) == 0) {
			if (value != 0x60) {
				log.println("write OCW2 (IRL/EOI) %x (was %x)", value, ocw2);
				//throw new RuntimeException("worrying OCW2 " + Integer.toHexString(value));
			}
			ocw2 = value;
			
		} else {
			log.println("write OCW3 (command) %x (was %x)", value, ocw3);
			ocw3 = value;
		}
	}

	private void writeData (final int value) {
		//log.println("write data %x", value);
		
		switch (init) {
			case 0:
				if (value < 0xe0) {
					log.println("write OCW1 (IMR) %x (was %x)", value, ocw1);
					//throw new RuntimeException("worrying OCW1 " + Integer.toHexString(value));
				}
				// interrupt mask
				ocw1 = value;
				return;
				
			case 2:
				log.println("write ICW2 (IVA) %x (was %x)", value, icw2);
				icw2 = value;
				init = !isSingle() ? 3 : isIcw4Needed() ? 4 : 0;
				return;
			
			case 3:
				log.println("write ICW3 (HS/SID) %x (was %x)", value, icw3);
				icw3 = value;
				init = isIcw4Needed() ? 4 : 0;
				return;
			
			case 4:
				log.println("write ICW4 (mode) %x (was %x)", value, icw4);
				icw4 = value;
				init = 0;
				return;
				
			default:
				throw new RuntimeException("unknown init " + init);
		}
	}

	private boolean isIcw4Needed () {
		return (icw1 & ICW1_ICW4NEEDED) != 0;
	}

	private boolean isSingle () {
		return (icw1 & ICW1_SINGLE) != 0;
	}
	
}
