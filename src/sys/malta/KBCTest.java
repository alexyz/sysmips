package sys.malta;

import java.util.*;
import java.util.function.Function;

import sys.mips.*;

import static sys.malta.KBCUtil.*;

public class KBCTest {

	public static void main (String[] args) {
		KBCTest t = new KBCTest();
		t.run();
	}
	
	private KBC dev;
	
	public void run() {
		Deque<CpuExceptionParams> pa = new ArrayDeque<>();
		
		dev = new KBC(0) {
			@Override
			protected void addException (CpuExceptionParams p) {
				pa.add(p);
			}
		};
		
		
		asEq("test", 0x55, cmdresp(CMD_SELFTEST));
		asEq("test2", 0, cmdresp(CMD_READCB));
		
		cmd(CMD_DISABLEAUX);
		asEq("disable aux", 0x20, cmdresp(CMD_READCB));
		cmd(CMD_ENABLEAUX);
		asEq("enable aux", 0, cmdresp(CMD_READCB));
		
		cmd(CMD_DISABLEKEY);
		asEq("disable key", 0x10, cmdresp(CMD_READCB));
		cmd(CMD_ENABLEKEY);
		asEq("enable key", 0, cmdresp(CMD_READCB));
		
		cmd(CMD_WRITECB, CB_DISABLEKEY);
		asEq("disable key via cb", 0x10, cmdresp(CMD_READCB));
		cmd(CMD_WRITECB, 0);
		asEq("enable key via cb", 0, cmdresp(CMD_READCB));
		
		asEq("aux test", 0, cmdresp(CMD_IFTESTAUX));
		
		cmd(CMD_WRITEAUXOUT, 0xfe); // send fe to aux output buffer
		asEq("write aux st", 0x21, readst());
		asEq("write aux dat", 0xfe, readdat());
		asEq("write aux st2", 0, readst());
		
		writedat(0xff);
		asEq("aux test dat", 0xfa, readdat());
		asEq("aux test dat", 0xaa, readdat());
		asEq("aux test st", 0, readst());
		
		// test reset using interrupts
		cmd(CMD_WRITECB, CB_ENABLEKEYINT);
		writedat(0xff);
		asf("enablekeyint1", e -> e != null && e.irq == MaltaUtil.IRQ_KEYBOARD, pa.poll());
		asEq("enablekeyint2", 0xfa, readdat());
		asf("enablekeyint3", e -> e != null && e.irq == MaltaUtil.IRQ_KEYBOARD, pa.poll());
		asEq("enablekeyint4", 0xaa, readdat());
		asf("enablekeyint5", e -> e == null, pa.poll());
		asEq("enablekeyint6", 0, readst());
		
	}
	
	private static void asEq (String name, int ex, int ac) {
		if (ex==ac) {
			System.out.println(String.format("OK: %s actual: %x", name, ac));
		} else {
			System.out.println(String.format("FAIL: %s expected: %x actual: %x", name, ex, ac));
			System.exit(-1);
		}
	}
	
	private static <T> void asf (String name, Function<T,Boolean> ec, T ac) {
		if (ec.apply(ac).booleanValue()) {
			System.out.println(String.format("OK: %s actual: %s", name, ac));
		} else {
			System.out.println(String.format("FAIL: %s actual: %s", name, ac));
			System.exit(-1);
		}
	}
	
	private int readst () {
		return dev.loadByte(M_CMDSTATUS) & 0xff;
	}
	
	private int readdat () {
		int s = readst();
		if ((s & 1) == 0) throw new RuntimeException("status not set");
		return dev.loadByte(M_DATA) & 0xff;
	}
	
	private void writedat (int v) {
		int s = readst();
		if ((s & 1) != 0) throw new RuntimeException("buffer full");
		dev.storeByte(M_DATA, (byte) v);
	}
	
	private void cmd (int cmd) {
		cmddataresp(cmd, -1, false);
	}
	
	private void cmd (int cmd, int dat) {
		cmddataresp(cmd, dat, false);
	}
	
	private int cmdresp (int cmd) {
		return cmddataresp(cmd, -1, true);
	}
	
	private int cmddataresp (int cmd, int dat, boolean resp) {
		int s = readst();
		if ((s & 1) != 0) throw new RuntimeException("buffer full");
		dev.storeByte(M_CMDSTATUS, (byte) cmd);
		if (dat >= 0) {
			writedat(dat);
		}
		int r = -1;
		if (resp) {
			r = readdat();
		}
		return r;
	}
}
