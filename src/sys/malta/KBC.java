package sys.malta;

import java.util.*;

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
	public static final int M_DATA = 0;
	/** keyboard command (write), status (read) */
	public static final int M_CMDSTATUS = 4;
	
	/** read command byte */
	private static final int CMD_READCB = 0x20;
	/** write command byte */
	private static final int CMD_WRITECB = 0x60;
	/** disable aux interface */
	private static final int CMD_DISABLEAUX = 0xa7;
	/** enable aux interface */
	private static final int CMD_ENABLEAUX = 0xa8;
	/** aux interface test */
	private static final int CMD_IFTESTAUX = 0xa9;
	/** self test */
	private static final int CMD_SELFTEST = 0xaa;
	/** keyboard interface test */
	private static final int CMD_IFTESTKEY = 0xab;
	/** disable keyboard interface */
	private static final int CMD_DISABLEKEY = 0xad;
	/** enable keyboard interface */
	private static final int CMD_ENABLEKEY = 0xae;
	/** write keyboard output buffer */
	private static final int CMD_WRITEKEY = 0xd2;
	/** write aux output buffer */
	private static final int CMD_WRITEAUX = 0xd3;
	
	/** bit 0: output buffer full */
	private static final int ST_OUTPUTFULL = 0x1;
	/** bit 1: input buffer full */
	private static final int ST_INPUTFULL = 0x2;
	/** bit 2: system */
	private static final int ST_SYSTEM = 0x4;
	/** bit 3: 0=data 1=command */
	private static final int ST_CMDDATA = 0x8;
	/** bit 4: inhibit keyboard */
	private static final int ST_INHIBIT = 0x10;
	/** bit 5: aux data available */
	private static final int ST_AUXDATA = 0x20;
	/** bit 6: timeout error */
	private static final int ST_TIMEOUT = 0x40;
	/** bit 7: parity error */
	private static final int ST_PARITY = 0x80;
	
	/** bit 0: key interrupt enable */
	private static final int CB_ENABLEKEYINT = 0x1;
	/** bit 1: aux interrupt enable */
	private static final int CB_ENABLEAUXINT = 0x2;
	/** bit 4: keyboard port clock disable */
	private static final int CB_DISABLEKEY = 0x10;
	/** bit 5: aux port clock disable */
	private static final int CB_DISABLEAUX = 0x20;
	
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
		
		dev.writecmd(CMD_WRITEAUX, 0xfe, false);
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
				if (r != 0) {
					log.println("gonna push another byte...");
					pushData(r, false);
				} else {
					data = 0;
					status = 0;
				}
				log.println("read data %x remaining %x", v, data);
				return v;
				
			case M_CMDSTATUS: // 64
				//log.println("read status %x", status);
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
				writeCommand(value);
				return;
				
			default:
				throw new RuntimeException();
		}
	}
	
	private void writeCommand (final int value) {
		//log.println("write command %x", value);
		datacmd = 0;
		
		switch (value) {
			case CMD_READCB:
				data = config;
				status |= ST_OUTPUTFULL;
				return;
			case CMD_WRITECB:
				// wait for the next byte...
				datacmd = value;
				status = 0;
				return;
			case CMD_DISABLEAUX:
				config |= CB_DISABLEAUX;
				status = 0;
				return;
			case CMD_ENABLEAUX:
				config &= ~CB_DISABLEAUX;
				status = 0;
				return;
			case CMD_DISABLEKEY:
				config |= CB_DISABLEKEY;
				status = 0;
				return;
			case CMD_ENABLEKEY:
				config &= ~CB_DISABLEKEY;
				status = 0;
				return;
			case CMD_IFTESTAUX:
				// success
				data = 0;
				status = ST_OUTPUTFULL;
				return;
			case CMD_SELFTEST:
				// success
				data = 0x55;
				status = 1;
				return;
			case CMD_WRITEKEY:
				// wait for the next byte...
				datacmd = value;
				status = 0;
				return;
			case CMD_WRITEAUX:
				// wait for the next byte...
				datacmd = value;
				status = 0;
				return;
			default:
				throw new RuntimeException(String.format("unknown keycmd %x", value));
		}
	}
	
	private void writeData (int value) {
		//log.println("write data %x (cmd %x)", value, datacmd);
		
		switch (datacmd) {
			case 0:
				writeDevice(value);
				break;
				
			case CMD_WRITECB:
				// write to cfg
				//log.println("keydata: write config %x (was %x)", value, commandbyte);
				config = value;
				status = 0;
				datacmd = 0;
				// XXX should copy system flag to status
				//log.println("config now " + cfgString(commandbyte));
				break;
				
			case CMD_WRITEAUX:
				//log.println("keydata: write aux out %x", value);
				pushData(value, true);
				datacmd = 0;
				break;
				
			default:
				throw new RuntimeException(String.format("unknown keydata %x for cmd %x", value, datacmd));
		}
	}

	private void writeDevice (int value) {
		// device command
		// atkbd.c atkbd_probe()
		boolean key = (config & CB_DISABLEKEY) == 0;
		boolean aux = (config & CB_DISABLEAUX) == 0;
		log.println("write device command key=" + key + " aux=" + aux);
		switch (value) {
			case 0xf2:
				log.println("identify");
				pushData(0x83abfa, false);
//						data = 0x83abfa;
//						status = ST_OUTPUTFULL;
				break;
			case 0xff:
				// [   19.044000] atkbd serio0: keyboard reset failed on isa0060/serio0
				log.println("reset");
				// fa = ack, aa = test pass
				pushData(0xaafa, false);
//						data = 0xaafa;
//						status = ST_OUTPUTFULL;
				break;
			default:
				throw new RuntimeException(String.format("unknown keyboard command %x config %s", value, cfgString(config)));
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
	
	private static List<String> cfgString (int cfg) {
		List<String> l = new ArrayList<>();
		if ((cfg & 0x1) != 0) l.add("0:keyint");
		if ((cfg & 0x2) != 0) l.add("1:auxint");
		if ((cfg & 0x4) != 0) l.add("2:system");
		if ((cfg & 0x8) != 0) l.add("3:ignorekeylock");
		if ((cfg & 0x10) != 0) l.add("4:keydisable");
		if ((cfg & 0x20) != 0) l.add("5:auxdisable");
		if ((cfg & 0x40) != 0) l.add("6:translate");
		if ((cfg & 0x80) != 0) l.add("7:reserved");
		return l;
	}
	
}
