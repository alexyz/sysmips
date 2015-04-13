package sys.test;

import java.io.RandomAccessFile;

import sys.elf.*;
import sys.mips.*;

import static sys.mips.MipsConstants.*;

public class Test2 {
	
	private Cpu cpu = new Cpu();
	
	public static void main (String[] args) throws Exception {
		Test2 t = new Test2();
		t.before();
		t.run();
	}
	
	private void before() throws Exception {
		cpu.getMemory().initPage(0x400000);
		cpu.setRegister(REG_SP, 0x4e0000);
		cpu.setRegister(REG_A0, 0x4f0000);
		cpu.getMemory().getSymbols().put(0x4d0000, "STACK");
		cpu.getMemory().getSymbols().put(0x4f0000, "RESULTS");
		try (RandomAccessFile file = new RandomAccessFile("xbin/a.out", "r")) {
			CpuLoader.loadElf(cpu, file);
		}
	}
	
	private void run() {
		System.out.println("test start");
		try {
			cpu.run();
		} catch (CpuException e) {
			System.out.println("exit reason: " + e);
		}
		
		// TODO load data from vector...
		
		System.out.println("test end");
	}
	
}
