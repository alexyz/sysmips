package sys.malta;

import java.util.ArrayList;
import java.util.List;

import sys.mips.Cpu;
import sys.mips.CpuConstants;
import sys.mips.CpuExceptionParams;
import sys.util.Logger;
import sys.util.Symbols;

/**
 * 8042 keyboard controller as documented in various contradictory sources
 */
public class KBC implements Device {
	
	/** keyboard data read/write */
	public static final int M_KBC_DATA = 0;
	/** keyboard command (write), status (read) */
	public static final int M_KBC_CMD = 4;
	
	private final Logger log = new Logger("KBC");
	private final int baseAddr;
	
	// there are really bytes represented as ints for convenience
	
	/**
	 * data byte (to be read from data port)
	 */
	private int data;
	
	/** 
	 * status byte
	 * bit 0: output buffer full
	 * bit 1: input buffer full
	 * bit 2: system
	 * bit 3: command/data
	 * bit 4: keyboard not inhibited/inhibited
	 * bit 5: transmit timeout
	 * bit 6: receive timeout
	 * bit 7: parity error
	 */
	private int status;
	
	/** config byte (ibm reference calls this command byte) */
	private int config;
	
	/** last command issued to command port (if required) */
	private int cmd;
	
	public KBC(int baseAddr) {
		this.baseAddr = baseAddr;
	}
	
	@Override
	public void init (Symbols sym) {
		sym.init(KBC.class, "M_", null, baseAddr, 1);
	}
	
	@Override
	public boolean isMapped (int addr) {
		final int offset = addr - baseAddr;
		return offset >= 0 && offset <= M_KBC_CMD;
	}
	
	@Override
	public int systemRead (int addr, int size) {
		final int offset = addr - baseAddr;
		
		switch (offset) {
			case M_KBC_DATA: // 60
				log.println("read keydata %x", data);
				// no more data...
				status = 0;
				return data;
				
			case M_KBC_CMD: // 64
				log.println("read status %x", status);
				return status;
				
			default:
				throw new RuntimeException();
		}
	}
	
	@Override
	public void systemWrite (int addr, int size, int value) {
		final int offset = addr - baseAddr;
		value &= 0xff;
		
		switch (offset) {
			case M_KBC_DATA:
				data(value);
				return;
				
			case M_KBC_CMD:
				command(value);
				return;
				
			default:
				throw new RuntimeException();
		}
	}

	private void command (final int value) {
		//log.println("keycmd %x", value);
		
		switch (value) {
			case 0x20:
				log.println("command %x: read config", value);
				data = config;
				status = 1;
				return;
			case 0x60:
				log.println("command %x: write config", value);
				// wait for the next byte...
				cmd = value;
				status = 0;
				return;
			case 0xa7:
				log.println("command %x: disable aux", value);
				// disable aux clock?
				config |= 0x20;
				status = 0;
				log.println("cmd now " + cfgString(config));
				return;
			case 0xa8:
				log.println("command %x: enable aux", value);
				config &= ~0x20;
				status = 0;
				log.println("cmd now " + cfgString(config));
				return;
			case 0xa9:
				log.println("command %x: test aux", value);
				// success
				data = 0;
				status = 1;
				return;
			case 0xaa:
				log.println("command %x: self test", value);
				// success
				data = 0x55;
				status = 1;
				return;
			case 0xd3:
				log.println("command %x: write to aux out", value);
				// wait for the next byte...
				cmd = value;
				status = 0;
				return;
			default:
				throw new RuntimeException(String.format("unknown keycmd %x", value));
		}
	}
	
	private void data (int value) {
		//log.println("keydata %x", value);
		
		switch (cmd) {
			case 0x60:
				// write to cfg
				log.println("keydata: write cmd byte %x", value);
				config = value;
				status = 0;
				cmd = 0;
				// XXX should write system flag to status
				log.println("cmd now " + cfgString(config));
				return;
				
			case 0xd3:
				log.println("keydata: write aux out %x", value);
				// output buffer full?
				data = value;
				status = 1;
				cmd = 0;
				// XXX is this right?
				if ((config & 0x1) != 0) {
					log.println("not adding mouse interrupt...");
				} else {
					log.println("adding mouse interrupt...");
					final CpuExceptionParams ep = new CpuExceptionParams(CpuConstants.EX_INTERRUPT, MaltaUtil.INT_SOUTHBRIDGE_INTR, MaltaUtil.IRQ_MOUSE);
					Cpu.getInstance().addException(ep);
				}
				return;
				
			default:
				throw new RuntimeException(String.format("unknown keydata %x for cmd %x", value, cmd));
		}
	}

	private static List<String> cfgString (int cfg) {
		List<String> l = new ArrayList<>();
		if ((cfg & 0x1) != 0) l.add("0:enableoutint");
		if ((cfg & 0x2) !=  0) l.add("1:reserved");
		if ((cfg & 0x4) !=  0) l.add("2:system");
		if ((cfg & 0x8) !=  0) l.add("3:inhibitoverride");
		if ((cfg & 0x10) != 0) l.add("4:disablekeyboard");
		if ((cfg & 0x20) != 0) l.add("5:disableaux");
		if ((cfg & 0x40) != 0) l.add("6:pccompat");
		if ((cfg & 0x80) != 0) l.add("7:reserved");
		return l;
	}
	
}
