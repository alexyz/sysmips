package sys.test;

import java.io.RandomAccessFile;

import sys.mips.*;

import static sys.mips.Constants.*;

public class Test2 {
	
	private Cpu cpu = new Cpu();
	
	public static void main (String[] args) throws Exception {
		Test2 t = new Test2();
		t.before();
		t.run();
	}
	
	private void before() throws Exception {
		cpu.getMemory().initPage(0x400000);
		cpu.setRegister(REG_SP, 0x4ffff0);
		cpu.getMemory().getSymbols().put(0x4f0000, "STACK");
		try (RandomAccessFile file = new RandomAccessFile("xbin/test", "r")) {
			CpuUtil.loadElf(cpu, file);
		}
	}
	
	private void run() {
		System.out.println("test start");
		
		try {
			cpu.run();
		} catch (CpuException e) {
			System.out.println("exit reason: " + e);
			System.out.println("A0: " + cpu.getRegister(REG_A0));
			System.out.println("F0(d): " + cpu.getFpRegister(0, FpFormat.DOUBLE));
			System.out.println("F2(d): " + cpu.getFpRegister(2, FpFormat.DOUBLE));
		}
		
		System.out.println("test end");
	}
	
}
