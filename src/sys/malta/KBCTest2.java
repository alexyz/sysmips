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
	
	private void run() {

		asEqual("initial status", 0x1c, status());
		
		asTrue("getconfig", cmd(0x20));
		asEqual("initial config", 0x55, read());
		asEqual("clear", 0, status() & 1);
		
		// set config
		
		// self test
		
		// keyout
		// see if it delivers int
		
		// pic masking...
	}

	private int status() {
		return p4.loadByte(CMD) & 0xff;
	}
	
	private boolean cmd(int c) {
		if ((status()&2)!=0) {
			return false;
		} else {
			p4.storeByte(CMD, (byte) c);
			return true;
		}
	}
	
	private int read() {
		if ((status()&1)!=1) {
			return -1;
		} else {
			return p4.loadByte(DATA) & 0xff;
		}
	}
	
	private boolean writecon (int x) {
		if ((status()&6)!=4) {
			return false;
		} else {
			p4.storeByte(DATA, (byte) x);
			return true;
		}
	}
	
	private static void asTrue (String name, boolean ac) {
		asEqual(name, 1, ac ? 1 : 0);
	}
	
	private static void asEqual (String name, int ex, int ac) {
		System.out.println(String.format("%s: %s expected: %x actual: %x", ex==ac ? "OK" : "FAIL", name, ex, ac));
	}
	
	
	
	
	
}
