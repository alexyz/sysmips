package sys.mips;

import java.io.RandomAccessFile;
import java.util.*;

import sys.elf.*;

import static sys.mips.MemoryUtil.*;
import static sys.mips.MipsConstants.*;

public class CpuUtil {

	/** load elf file, set entry point, return max address */
	public static int loadElf (Cpu cpu, RandomAccessFile file) throws Exception {
		ELF32 elf = new ELF32(file);
		System.out.println("elf=" + elf);
		int top = 0;
		
		for (ELF32Program program : elf.programs) {
			if (program.type == ELF32Program.PT_LOAD) {
				file.seek(program.fileOffset);
				final byte[] data = new byte[program.memorySize];
				file.read(data, 0, program.fileSize);
				MemoryUtil.storeBytes(cpu.getMemory(), program.physicalAddress, data);
				top = program.physicalAddress + program.memorySize;
			}
		}
		
		for (ELF32Symbol symbol : elf.symbols) {
			if (symbol.getBind() == ELF32Symbol.STB_GLOBAL && symbol.size > 0) {
				cpu.getMemory().getSymbols().put(symbol.value, symbol.name);
			}
		}
		System.out.println("symbols=" + cpu.getMemory().getSymbols());
		
		System.out.println("entry=" + cpu.getMemory().getSymbols().getName(elf.header.entryAddress));
		cpu.setPc(elf.header.entryAddress);
		return top;
	}
	
	/**
	 * store argument and environment vectors and load registers ready for a
	 * call to a main() function
	 */
	public static void setMainArgs (final Cpu cpu, final int addr, final List<String> argsList, final List<String> envList) {
		final Memory mem = cpu.getMemory();
		int p = addr;

		final List<Integer> argv = new ArrayList<>();
		p = storeStrings(mem, p, argv, argsList);
		final List<Integer> env = new ArrayList<>();
		p = storeStrings(mem, p, env, envList);
		final int argvAddr = nextWord(p);
		p = storeWords(mem, argvAddr, argv);
		final int envAddr = nextWord(p);
		p = storeWords(mem, envAddr, env);
		
		cpu.setRegister(REG_A0, argsList.size());
		cpu.setRegister(REG_A1, argvAddr);
		cpu.setRegister(REG_A2, envAddr);
	}
}
