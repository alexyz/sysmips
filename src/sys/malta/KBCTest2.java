package sys.malta;

/**
 * improved keyboard tests
 */
public class KBCTest2 {
	
	private static final int DATA = 0x60;
	private static final int CMD = 0x64;

	public static void main (String[] args) {
		
		//readstatus (should be 1c=432.)
		//readconfig (should be 55=6420.)
		KBCTest2 t = new KBCTest2();
		t.run();
	}
	
	private final PIIX4 p4 = new PIIX4(null, 0);
	private static final int IS = 0x1c;
	
	private void run() {

		// initial pic imr,isr
		
		asEqual("0 initial status", IS, status());
		
		// get config
		asTrue("1 get config cmd", cmd(0x20));
		asEqual("1 get config data", 0x55, read());
		asEqual("1 status", IS, status()); // vb says 0x1c = sys,cmd,keylock
		
		// set config 0x45
		asTrue("2 write config cmd", cmd(0x60));
		asTrue("2 write config data", writecon(0x45));
		asTrue("2 get config cmd", cmd(0x20));
		asEqual("2 read config data", 0x45, read());
		asEqual("2 status", IS, status());
		
		// keyout when not masked
		// handler: imr=0. isr=1. irr=1.? 
		// handler: read byte, send eoi 
		// imr=0. isr=0. irr=0.? 

		// keyout when masked
		// sleep 1 tick
		// imr=1. isr=0. irr=1.
		// unmask?
		// handler as before?
		
		//asTrue("2 write keyout cmd", cmd(0xd2));
		//asTrue("2 write config data", writecon(0xee));
		
		
		// self test
		
	}
	
	private int status() {
		return p4.loadByte(CMD) & 0xff;
	}
	
	private boolean cmd(int c) {
		if (status()==IS) {
			p4.storeByte(CMD, (byte) c);
			return true;
		} else {
			System.out.println("status=" + KBCUtil.statusString(status()));
			return false;
		}
	}
	
	private int read() {
		if (status()==(IS|1)) {
			return p4.loadByte(DATA) & 0xff;
		} else {
			System.out.println("status=" + KBCUtil.statusString(status()));
			return -1;
		}
	}
	
	private boolean writecon (int x) {
		if (status()==IS) { // ob clear, ib clear, sys, cmd
			p4.storeByte(DATA, (byte) x);
			return true;
		} else {
			System.out.println("status=" + KBCUtil.statusString(status()));
			return false;
		}
	}
	
	private static void asTrue (String name, boolean ac) {
		as(name, ac, String.valueOf(true), String.valueOf(ac));
	}
	
	private static void asEqual (String name, int ex, int ac) {
		as(name, ex==ac, Integer.toHexString(ex), Integer.toHexString(ac));
	}
	
	private static void as (String name, boolean value, String ex, String ac) {
		System.out.println(String.format("%s: %s expected: %s actual: %s", value ? "OK" : "FAIL", name, ex, ac));
	}
	
	
	
}
