package sys.test;

import java.io.RandomAccessFile;

import sys.elf.*;
import sys.mips.*;

public class Test2 {
	
	private Cpu cpu = new Cpu();
	
	public static void main (String[] args) throws Exception {
		Test2 t = new Test2();
		t.before();
		t.run();
	}
	
	private void before() throws Exception {
		cpu.getMemory().init(0x400000);
		cpu.getRegisters()[29] = 0x4ffff0;
		cpu.getMemory().init(0x500000);
		cpu.getRegisters()[4] = 0x500000;
		try (RandomAccessFile file = new RandomAccessFile("xbin/a.out", "r")) {
			load(file);
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
	
	private void load (RandomAccessFile file) throws Exception {
		ELF32 elf = new ELF32(file);
		System.out.println("elf=" + elf);
		for (ELF32Program program : elf.programs) {
			if (program.type == ELF32Program.PT_LOAD) {
				file.seek(program.fileOffset);
				final byte[] data = new byte[program.memorySize];
				file.read(data, 0, program.fileSize);
				cpu.getMemory().storeBytes(program.virtualAddress, data);
			}
		}
		
		for (ELF32Symbol symbol : elf.symbols) {
			if (symbol.getBind() == ELF32Symbol.STB_GLOBAL) {
				cpu.getMemory().getSymbols().put(symbol.valueAddress, symbol.name);
			}
		}
		
		System.out.println("entry=" + elf.header.entryAddress);
		cpu.setPc(elf.header.entryAddress);
	}
}
