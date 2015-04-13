package sys.test;

import java.util.Random;

import sys.mips.*;

public class Test {
	private static final int BREAK = 0b001101;
	
	private static int rd (final int isn) {
		return (isn >>> 11) & 0x1f;
	}
	
	private Cpu cpu = new Cpu();
	private Memory mem = cpu.getMemory();
	private Random random = new Random();
	private int[] reg = cpu.getRegisters();
	private CpuException.Type exType;
	
	public static void main (String[] args) {
		Test t = new Test();
		t.run();
	}
	
	private void run() {
		System.out.println("test start");
		mem.initPage(0);
		testSll();
		testAddu();
		System.out.println("test end");
	}

	private void testSll () {
		System.out.println("sll");
		for (int n = 0; n < 100; n++) {
			int rt = random.nextInt();
			int sa = random.nextInt(32);
			int isn = exec (0, 0, rt, 0, sa, 0);
			assertEqual(reg[rd(isn)], rt << sa, "sll");
			assertEqual(exType, CpuException.Type.Breakpoint, "break");
		}
	}
	
	private void testAddu () {
		System.out.println("addu");
		for (int n = 0; n < 100; n++) {
			int rs = random.nextInt();
			int rt = random.nextInt();
			int rd = random.nextInt();
			int isn = exec (0, rs, rt, rd, 0, 0b100001);
			assertEqual(reg[rd(isn)], rs + rt, "addu");
			assertEqual(exType, CpuException.Type.Breakpoint, "break");
		}
	}
	
	private int exec (int op, int rs, int rt, int rd, int sa, int fn) {
		int r = random.nextInt(20) + 1;
		int a = random.nextInt(0x3f000) << 2;
		reg[r] = rs;
		reg[r+1] = rt;
		reg[r+2] = rd;
		int isn = (op << 26) | (r << 21) | ((r+1) << 16) | ((r+2) << 11) | (sa << 6) | fn;
		mem.storeWord(a, isn);
		mem.storeWord(a+4, 0);
		mem.storeWord(a+8, BREAK);
		cpu.setPc(a);
		try {
			exType = null;
			cpu.run();
		} catch (CpuException e) {
			exType = e.type;
		}
		return isn;
	}
	
	void assertEqual(int a, int b, String msg) {
		if (a != b) {
			cpu.getLogger().print(System.out);
			throw new RuntimeException("assertion failure: " + a + " = " + b + ": " + msg);
		}
	}
	
	void assertEqual(Object a, Object b, String msg) {
		if (a != b) {
			cpu.getLogger().print(System.out);
			throw new RuntimeException("assertion failure: " + a + " = " + b + ": " + msg);
		}
	}
}
