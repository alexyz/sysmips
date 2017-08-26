package sys.malta;

import sys.mips.InstructionUtil;

public class KBCUtil {
	
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
	/** pulse output line (f0 to ff) */
	public static final int CMD_PULSE = 0xf0;
	
	/** bit 0: output buffer full */
	public static final int ST_OUTPUTFULL = 0x1;
	/** bit 1: input buffer full */
	public static final int ST_INPUTFULL = 0x2;
	/** bit 2: system */
	public static final int ST_SYSTEM = 0x4;
	/** bit 3: 0=data 1=command */
	public static final int ST_CMDDATA = 0x8;
	/** bit 4: inhibit keyboard (linux calls this keylock?) */
	public static final int ST_NOKEYLOCK = 0x10;
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
	
	/** system, command, nokeylock, buffers clear **/
	public static int initialStatus () {
		// 0x1c
		return ST_NOKEYLOCK|ST_SYSTEM|ST_CMDDATA;
	}
	
	public static int initialConfig () {
		// 0x55
		return CB_KEYTRANS|CB_DISABLEKEY|CB_SYSTEM|CB_ENABLEKEYINT;
	}
	
	public static String configString (int cfg) {
		return InstructionUtil.flagString(KBCUtil.class, "CB_", cfg);
	}
	
	public static String statusString (int s) {
		return InstructionUtil.flagString(KBCUtil.class, "ST_", s);
	}

	public static String kbString (int c) {
		return InstructionUtil.lookup(KBCUtil.class, "KB_", c);
	}

	public static String cmdString (int v) {
		return InstructionUtil.lookup(KBCUtil.class, "CMD_", v);
	}
	
	private KBCUtil() {
		//
	}
}
