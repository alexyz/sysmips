package sys.malta;

import java.util.*;

import sys.mips.Cpu;
import sys.mips.CpuConstants;
import sys.mips.CpuExceptionParams;
import sys.mips.InstructionUtil;
import sys.util.Logger;
import sys.util.Symbols;

/**
 * 8042 keyboard controller as documented in various contradictory sources
 * @see linux/drivers/input/keyboard/atkbd.c
 * @see linux/include/linux/i8042.h
 */
public class KBC implements Device {
	
	/** keyboard data read/write */
	public static final int M_DATA = 0;
	/** keyboard command (write), status (read) */
	public static final int M_CMDSTATUS = 4;
	
	/** read command byte */
	public static final int CMD_READCB = 0x20;
	/** write command byte */
	public static final int CMD_WRITECB = 0x60;
	/** disable aux interface */
	public static final int CMD_DISABLEAUX = 0xa7;
	/** enable aux interface */
	public static final int CMD_ENABLEAUX = 0xa8;
	/** aux interface test */
	public static final int CMD_IFTESTAUX = 0xa9;
	/** self test */
	public static final int CMD_SELFTEST = 0xaa;
	/** keyboard interface test */
	public static final int CMD_IFTESTKEY = 0xab;
	/** disable keyboard interface */
	public static final int CMD_DISABLEKEY = 0xad;
	/** enable keyboard interface */
	public static final int CMD_ENABLEKEY = 0xae;
	/** write keyboard output buffer */
	public static final int CMD_WRITEKEYOUT = 0xd2;
	/** write aux output buffer */
	public static final int CMD_WRITEAUXOUT = 0xd3;
	/** write aux input buffer (write to device) */
	public static final int CMD_WRITEAUXIN = 0xd4;
	
	/** bit 0: output buffer full */
	public static final int ST_OUTPUTFULL = 0x1;
	/** bit 1: input buffer full */
	public static final int ST_INPUTFULL = 0x2;
	/** bit 2: system */
	public static final int ST_SYSTEM = 0x4;
	/** bit 3: 0=data 1=command */
	public static final int ST_CMDDATA = 0x8;
	/** bit 4: inhibit keyboard (linux calls this keylock?) */
	public static final int ST_NOTINHIBITED = 0x10;
	/** bit 5: aux data available */
	public static final int ST_AUXDATA = 0x20;
	/** bit 6: timeout error */
	public static final int ST_TIMEOUT = 0x40;
	/** bit 7: parity error */
	public static final int ST_PARITY = 0x80;
	
	/** bit 0: key interrupt enable */
	public static final int CB_ENABLEKEYINT = 0x1;
	/** bit 1: aux interrupt enable */
	public static final int CB_ENABLEAUXINT = 0x2;
	public static final int CB_SYSTEM = 0x4;
	public static final int CB_OVERRIDE = 0x8;
	/** bit 4: keyboard port clock disable */
	public static final int CB_DISABLEKEY = 0x10;
	/** bit 5: aux port clock disable */
	public static final int CB_DISABLEAUX = 0x20;
	public static final int CB_KEYTRANS = 0x40;
	public static final int CB_RESERVED = 0x80;
	
	// keyboard commands
	public static final int KB_SETLED = 0xed;
	public static final int KB_ECHO = 0xee;
	public static final int KB_SCANCODESET = 0xf0;
	public static final int KB_IDENTIFY = 0xf2;
	public static final int KB_SETTYPE = 0xf3;
	public static final int KB_ENABLESCAN = 0xf4;
	public static final int KB_DISABLESCAN = 0xf5;
	public static final int KB_DEFAULT = 0xf6;
	public static final int KB_ALLTYPE = 0xf7;
	public static final int KB_ALLMAKEREL = 0xf8;
	public static final int KB_ALLMAKE = 0xf9;
	public static final int KB_ALLTYPEMAKEREL = 0xfa;
	public static final int KB_SPECIFICTYPE = 0xfb;
	public static final int KB_SPECIFICMAKEREL = 0xfc;
	public static final int KB_SPECIFICMAKE = 0xfd;
	public static final int KB_RESEND = 0xfe;
	public static final int KB_RESET = 0xff;
	
	public static void main (String[] args) {
		Deque<CpuExceptionParams> pa = new ArrayDeque<>();
		CpuExceptionParams p;
		KBC dev = new KBC(0) {
			@Override
			protected void addException (CpuExceptionParams p) {
				pa.add(p);
			}
		};
		int v;
		
		v = dev.writecmd(CMD_SELFTEST, -1, true);
		System.out.println("test = " + Integer.toHexString(v) + " should be 55");
		
		v = dev.writecmd(CMD_READCB, -1, true);
		System.out.println("cb = " + Integer.toHexString(v) + " should be 0");
		
		dev.writecmd(CMD_DISABLEAUX, -1, false);
		System.out.println("disabled aux");
		v = dev.writecmd(CMD_READCB, -1, true);
		System.out.println("cb = " + Integer.toHexString(v) + " should be 20");
		dev.writecmd(CMD_ENABLEAUX, -1, false);
		System.out.println("enabled aux");
		v = dev.writecmd(CMD_READCB, -1, true);
		System.out.println("cb = " + Integer.toHexString(v) + " should be 0");
		
		dev.writecmd(CMD_DISABLEKEY, -1, false);
		System.out.println("disabled key");
		v = dev.writecmd(CMD_READCB, -1, true);
		System.out.println("cb = " + Integer.toHexString(v) + " should be 10");
		dev.writecmd(CMD_ENABLEKEY, -1, false);
		System.out.println("enabled key");
		v = dev.writecmd(CMD_READCB, -1, true);
		System.out.println("cb = " + Integer.toHexString(v) + " should be 0");
		
		dev.writecmd(CMD_WRITECB, CB_DISABLEKEY, false);
		System.out.println("disabled key via cb");
		v = dev.writecmd(CMD_READCB, -1, true);
		System.out.println("cb = " + Integer.toHexString(v) + " should be 10");
		dev.writecmd(CMD_WRITECB, 0, false);
		System.out.println("enabled key via cb");
		v = dev.writecmd(CMD_READCB, -1, true);
		System.out.println("cb = " + Integer.toHexString(v) + " should be 0");
		
		v = dev.writecmd(CMD_IFTESTAUX, -1, true);
		System.out.println("aux test " + v + " should be 0");
		
		dev.writecmd(CMD_WRITEAUXOUT, 0xfe, false);
		System.out.println("wrote aux fe");
		v = dev.readst();
		System.out.println("st " + Integer.toHexString(v) + " should be 21");
		v = dev.readdat();
		System.out.println("dat " + Integer.toHexString(v) + " should be fe");
		v = dev.readst();
		System.out.println("st " + Integer.toHexString(v) + " should be 0");
		
		dev.writedat(0xff);
		v = dev.readdat();
		System.out.println("dat " + Integer.toHexString(v) + " should be fa");
		v = dev.readdat();
		System.out.println("dat " + Integer.toHexString(v) + " should be aa");
		v = dev.readst();
		System.out.println("st " + Integer.toHexString(v) + " should be 0");
		
		// test reset using interrupts
		dev.writecmd(CMD_WRITECB, CB_ENABLEKEYINT, false);
		dev.writedat(0xff);
		p = pa.poll();
		System.out.println("p " + p + " should be keyboard int");
		v = dev.readdat();
		System.out.println("dat " + Integer.toHexString(v) + " should be fa");
		p = pa.poll();
		System.out.println("p " + p + " should be keyboard int");
		v = dev.readdat();
		System.out.println("dat " + Integer.toHexString(v) + " should be aa");
		p = pa.poll();
		System.out.println("p " + p + " should be null");
		v = dev.readst();
		System.out.println("st " + Integer.toHexString(v) + " should be 0");
		
	}
	
	private int readst () {
		return (byte) systemRead(M_CMDSTATUS, 1);
	}
	
	private int readdat () {
		int s = readst();
		if ((s & 1) == 0) throw new RuntimeException("status not set");
		return (byte) systemRead(M_DATA, 1);
	}
	
	private void writedat (int v) {
		int s = readst();
		if ((s & 1) != 0) throw new RuntimeException("buffer full");
		systemWrite(M_DATA, 1, v);
	}
	
	private int writecmd (int cmd, int next, boolean resp) {
		int s, r = -1;
		s = readst();
		if ((s & 1) != 0) throw new RuntimeException("buffer full");
		systemWrite(M_CMDSTATUS, 1, cmd);
		if (next >= 0) {
			writedat(next);
		}
		if (resp) {
			r = readdat();
		}
		return r;
	}
	
	private final Logger log = new Logger("KBC");
	private final int baseAddr;
	
	// there are really bytes represented as ints for convenience
	
	/**
	 * data byte (to be read from data port)
	 */
	private int data;
	
	/** status byte */
	private int status;
	
	/** 
	 * config/command byte (command 20/60)<br>
	 * bit 0: key interrupt enable<br>
	 * bit 1: aux interrupt enable<br>
	 * bit 2: system flag<br>
	 * bit 3: inhibit override<br>
	 * bit 4: keyboard port clock disable<br>
	 * bit 5: aux port clock disable<br>
	 * bit 6: key translation<br>
	 * bit 7: 0<br>
	 */
	private int config;
	
	/** last command issued to command port (if required) */
	private int datacmd;
	
	/** last command issued to device */
	private int devcmd;
	
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
		return offset >= 0 && offset <= M_CMDSTATUS;
	}
	
	@Override
	public int systemRead (int addr, int size) {
		final int offset = addr - baseAddr;
		
		switch (offset) {
			case M_DATA: // 60
				// no more data...
				// bit of a hack to return multiple bytes
				// should this raise irq?
				int v = data & 0xff;
				int r = data >>> 8;
				log.println("read data %x remaining %x", v, r);
				if (r != 0) {
					log.println("gonna push another byte...");
					pushData(r, false);
				} else {
					data = 0;
					status = 0;
				}
				return v;
				
			case M_CMDSTATUS: // 64
				status |= ST_NOTINHIBITED;
				log.println("read status %x: %s", status, statusString(status));
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
			case M_DATA:
				writeData(value);
				return;
				
			case M_CMDSTATUS:
				writeControllerCommand(value);
				return;
				
			default:
				throw new RuntimeException();
		}
	}
	
	private void writeControllerCommand (final int value) {
		log.println("write controller command %x: %s", value, cmdString(value));
		datacmd = 0;
		
		switch (value) {
			case CMD_READCB:
				log.println("read config %x: %s", config, configString(config));
				data = config;
				status = ST_OUTPUTFULL;
				break;
			case CMD_WRITECB:
				// wait for the next byte...
				datacmd = value;
				status = ST_CMDDATA;
				break;
			case CMD_DISABLEAUX:
				config |= CB_DISABLEAUX;
				status = 0;
				break;
			case CMD_ENABLEAUX:
				config &= ~CB_DISABLEAUX;
				status = 0;
				break;
			case CMD_DISABLEKEY:
				config |= CB_DISABLEKEY;
				status = 0;
				break;
			case CMD_ENABLEKEY:
				config &= ~CB_DISABLEKEY;
				status = 0;
				break;
			case CMD_IFTESTAUX:
				// success
				data = 0;
				status = ST_OUTPUTFULL;
				break;
			case CMD_SELFTEST:
				// success
				data = 0x55;
				status = 1;
				break;
			case CMD_WRITEKEYOUT:
				// wait for the next byte...
				datacmd = value;
				status = ST_CMDDATA;
				break;
			case CMD_WRITEAUXOUT:
				// wait for the next byte...
				datacmd = value;
				status = ST_CMDDATA;
				break;
			case CMD_WRITEAUXIN:
				datacmd = value;
				status = ST_CMDDATA;
				break;
			default:
				throw new RuntimeException(String.format("unknown kbc command %x", value));
		}
	}
	
	private void writeData (int value) {
		if ((status & ST_CMDDATA) != 0) {
			writeControllerData(value);
		} else if (devcmd == 0) {
			writeDeviceCommand(value);
		} else {
			writeDeviceData(value);
		}
	}

	private void writeControllerData (int value) {
		log.println("write controller data %x for cmd %x: %s", value, datacmd, cmdString(datacmd));
		
		switch (datacmd) {
			case CMD_WRITECB:
				// write to cfg
				log.println("write config %x: %s (was %x: %s)", 
						value, configString(value), config, configString(config));
				config = value;
				status = 0;
				// XXX should copy system flag to status
				//log.println("config now " + cfgString(commandbyte));
				break;
				
			case CMD_WRITEAUXOUT:
				log.println("write aux out %x", value);
				pushData(value, true);
				break;
				
			default:
				throw new RuntimeException(String.format("unknown kbc data %x for cmd %x", value, datacmd));
		}
		
		datacmd = 0;
	}

	private void writeDeviceData (int value) {
		log.println("write device data %x for device command %x: %s", value, devcmd, kbString(devcmd));
		switch (devcmd) {
			case KB_SETLED:
				// yawn
				break;
			default:
				throw new RuntimeException(String.format("unknown devcmd %x data %x", devcmd, value));
		}
		
		devcmd = 0;
	}

	private void writeDeviceCommand (int value) {
		// device command
		// atkbd.c atkbd_probe()
		log.println("write device command %x: %s", value, kbString(value));
		
		switch (value) {
			case KB_SETLED:
				pushData(0xfa, false);
				devcmd = 0xed;
				break;
				
			case KB_IDENTIFY:
				pushData(0x83abfa, false);
				break;
			case KB_RESET:
				// [   19.044000] atkbd serio0: keyboard reset failed on isa0060/serio0
				// fa = ack, aa = test pass
				pushData(0xaafa, false);
				break;
			default:
				throw new RuntimeException(String.format("unknown kbc device command %x config %s", value, configString(config)));
		}
	}
	
	private void pushData (int value, boolean aux) {
		data = value;
		// output buffer full, aux data available
		status = ST_OUTPUTFULL | (aux ? ST_AUXDATA : 0);
		if ((config & (aux ? CB_ENABLEAUXINT : CB_ENABLEKEYINT)) != 0) {
			log.println("fire kbc irq aux=" + aux);
			addException(new CpuExceptionParams(CpuConstants.EX_INTERRUPT, 
					MaltaUtil.INT_SOUTHBRIDGE_INTR, 
					aux ? MaltaUtil.IRQ_MOUSE : MaltaUtil.IRQ_KEYBOARD));
		} else {
			log.println("not firing kbc irq due to disabled");
		}
	}

	protected void addException (CpuExceptionParams p) {
		Cpu.getInstance().addException(p);
	}
	
	private String configString (int cfg) {
		return InstructionUtil.flagString(getClass(), "CB_", cfg);
	}
	
	private String statusString (int s) {
		return InstructionUtil.flagString(getClass(), "ST_", s);
	}

	private String kbString (int c) {
		return InstructionUtil.lookup(getClass(), "KB_", c);
	}

	private String cmdString (int v) {
		return InstructionUtil.lookup(getClass(), "CMD_", v);
	}
}
