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
	 * status byte<br>
	 * bit 0: output buffer full<br>
	 * bit 1: input buffer full<br>
	 * bit 2: system or muxerr<br>
	 * bit 3: command/data<br>
	 * bit 4: keyboard not inhibited/inhibited (keylock)<br>
	 * bit 5: transmit timeout or aux data available<br>
	 * bit 6: receive timeout or timeout<br>
	 * bit 7: parity error<br>
	 */
	private int status;
	
	/** 
	 * config byte (ibm reference calls this command byte)<br>
	 * bit 0: key interrupt enable<br>
	 * bit 1: aux interrupt enable<br>
	 * bit 2: system flag<br>
	 * bit 3: 0<br>
	 * bit 4: keyboard port clock disable<br>
	 * bit 5: aux port clock disable<br>
	 * bit 6: key translation<br>
	 * bit 7: 0<br>
	 */
	private int config;
	
	/** last command issued to command port (if required) */
	private int cmd;
//	private int nextcmd;
	
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
		final int data = this.data;
		
		switch (offset) {
			case M_KBC_DATA: // 60
				log.println("read keydata %x", data);
				// no more data...
				status = 0;
//				if (nextcmd != 0) {
//					log.println("next cmd %x", nextcmd);
//					command(nextcmd);
//					nextcmd = 0;
//				}
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
				log.println("config now " + cfgString(config));
				return;
			case 0xa8:
				log.println("command %x: enable aux", value);
				config &= ~0x20 & 0xff;
				status = 0;
				log.println("config now " + cfgString(config));
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
		log.println("keydata %x (cmd %x)", value, cmd);
		
		switch (cmd) {
			case 0:
				switch (value) {
					case 0xff:
						log.println("keydata: reset");
						data = 0xfa;
						status = 0x1;
						// FIXME should this write two bytes back?
//						nextcmd = 0xaa;
						break;
					default:
						throw new RuntimeException(String.format("unknown system command %x", value));
				}
				break;
				
			case 0x60:
				// write to cfg
				log.println("keydata: write config %x (was %x)", value, config);
				config = value;
				status = 0;
				cmd = 0;
				// XXX should copy system flag to status
				log.println("config now " + cfgString(config));
				return;
				
			case 0xd3:
				log.println("keydata: write aux out %x", value);
				data = value;
				status = 0x21; // output buffer full, aux data available
				cmd = 0;
				if ((config & 0x2) != 0) {
					log.println("fire aux irq!!");
					final CpuExceptionParams ep = new CpuExceptionParams(CpuConstants.EX_INTERRUPT, MaltaUtil.INT_SOUTHBRIDGE_INTR, MaltaUtil.IRQ_MOUSE);
					Cpu.getInstance().addException(ep);
				} else {
					log.println("not firing aux irq due to disabled");
				}
				return;
				
			default:
				throw new RuntimeException(String.format("unknown keydata %x for cmd %x", value, cmd));
		}
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
