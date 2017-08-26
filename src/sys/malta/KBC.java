package sys.malta;

import sys.mips.*;
import sys.util.Logger;

import static sys.malta.KBCUtil.*;

/**
 * 8042 keyboard controller as documented in various contradictory sources
 * @see linux/drivers/input/keyboard/atkbd.c
 * @see linux/include/linux/i8042.h
 */
public class KBC extends Device {
	
	private final Logger log = new Logger("KBC");
	
	// there are really bytes represented as ints for convenience
	
	/**
	 * data byte (to be read from data port)
	 */
	private int data;
	
	/** 
	 * status byte<br>
	 * 0: output buffer full (can read from data port)<br>
	 * 1: input buffer full (cannot write to cmd/data port)<br>
	 * 2: system (1)<br>
	 * 3: command/data (1 for controller)<br>
	 * 4: unknown (keylock?)<br>
	 * 5: unknown (receive timeout?)<br>
	 * 6: unknown (timeout?)<br>
	 * 7: unknown (parity?)<br>
	 */
	private int status;
	
	/** 
	 * config/command byte (command 20/60)<br>
	 * bit 0: key interrupt enable<br>
	 * bit 1: aux interrupt enable<br>
	 * bit 2: system flag<br>
	 * bit 3: zero/inhibit override<br>
	 * bit 4: key clock disable<br>
	 * bit 5: aux clock disable<br>
	 * bit 6: key translation<br>
	 * bit 7: zero<br>
	 */
	private int config;
	
	/** last command issued to command port (if required) */
	private int datacmd;
	
	/** last command issued to device */
	private int devcmd;
	
	public KBC (Device parent, int baseAddr) {
		super(parent, baseAddr);
		status = initialStatus();
		config = initialConfig();
	}
	
	@Override
	public void init () {
		getCpu().getSymbols().init(KBCUtil.class, "M_", null, baseAddr, 1);
	}
	
	@Override
	public boolean isMapped (int addr) {
		final int offset = addr - baseAddr;
		return offset >= 0 && offset <= M_CMDSTATUS;
	}
	
	@Override
	public byte loadByte (final int addr) {
		final int offset = addr - baseAddr;
		
		switch (offset) {
			case M_DATA: // 60
				return readData();
				
			case M_CMDSTATUS: // 64
				//status |= ST_NOTINHIBITED;
				log.println("read status %x: %s", status, statusString(status));
				return (byte) status;
				
			default:
				throw new RuntimeException();
		}
	}

	@Override
	public void storeByte (final int addr, final byte value) {
		final int offset = addr - baseAddr;
		
		switch (offset) {
			case M_DATA:
				writeData(value & 0xff);
				return;
				
			case M_CMDSTATUS:
				writeControllerCommand(value & 0xff);
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
				status = ST_OUTPUTFULL|initialStatus();
				break;
			case CMD_WRITECB:
				// wait for the next byte...
				datacmd = value;
				status = initialStatus();
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
//				if (value > CMD_PULSE) {
//					log.println("pulse %x", value & 0xf);
//				} else {
					throw new RuntimeException(String.format("unknown controller command: %x", value));
//				}
		}
	}

	private byte readData () {
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
			status = initialStatus();
		}
		return (byte) v;
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
				status = initialStatus();
				break;
				
			case CMD_WRITEAUXOUT:
				log.println("write aux out %x", value);
				pushData(value, true);
				break;
				
			case CMD_WRITEAUXIN:
				// XXX write mouse command
				// ff = mouse reset
				// pretend mouse not connected?
				log.println("write aux in %x", value);
//				break;
				
			default:
				throw new RuntimeException(String.format("unknown controller data: cmd %s data %x", cmdString(datacmd), value));
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
				throw new RuntimeException(String.format("unknown device data: cmd %x data %x", devcmd, value));
		}
		
		devcmd = 0;
	}

	private void writeDeviceCommand (int value) {
		// device command
		// atkbd.c atkbd_probe()
		log.println("write device command %x: %s", value, kbString(value));
		devcmd = 0;
		
		switch (value) {
			case KB_SETLED:
				pushData(0xfa, false);
				devcmd = KB_SETLED;
				break;
			case KB_IDENTIFY:
				pushData(0x83abfa, false);
				break;
			case KB_RESET:
				// [   19.044000] atkbd serio0: keyboard reset failed on isa0060/serio0
				// fa = ack, aa = test pass
				pushData(0xaafa, false);
				break;
			case KB_ENABLESCAN:
				pushData(0xfa, false);
				break;
			default:
				throw new RuntimeException(String.format("unknown device command %s config %s", kbString(value), configString(config)));
		}
	}
	
	private void pushData (int value, boolean aux) {
		data = value;
		// output buffer full, aux data available
		status = ST_OUTPUTFULL | (aux ? ST_AUXDATA : 0);
		if ((config & (aux ? CB_ENABLEAUXINT : CB_ENABLEKEYINT)) != 0) {
			log.println("1fire kbc irq aux=" + aux);
			fire(aux ? MaltaUtil.IRQ_MOUSE : MaltaUtil.IRQ_KEYBOARD);
		} else {
			log.println("not firing kbc irq due to disabled");
		}
	}
	
	@Override
	public void fire (int irq) {
		// XXX for some reason this isn't visible in log
		log.println("2fire irq " + irq);
		super.fire(irq);
	}
}
