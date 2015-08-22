package sys.mips;

import java.io.RandomAccessFile;
import java.util.*;

import sys.elf.*;

import static sys.mips.MemoryUtil.*;
import static sys.mips.MipsConstants.*;
import static sys.mips.IsnUtil.*;

/**
 * Cpu helper functions
 */
public class CpuUtil {
	
	/** load elf file into cpu, set entry point, return max address */
	public static Cpu loadElf (final RandomAccessFile file, final int[] top) throws Exception {
		ELF32 elf = new ELF32(file);
		System.out.println("elf=" + elf);
		
		Cpu cpu = new Cpu(elf.header.data == ELF32Header.ELFDATA2LSB);
		Memory mem = cpu.getMemory();
		Symbols sym = mem.getSymbols();
		
		top[0] = 0;
		
		for (ELF32Program program : elf.programs) {
			if (program.type == ELF32Program.PT_LOAD) {
				System.out.println("ph=" + program);
				file.seek(program.fileOffset);
				final byte[] data = new byte[program.memorySize];
				file.read(data, 0, program.fileSize);
				MemoryUtil.storeBytes(mem, program.physicalAddress, data);
				top[0] = program.physicalAddress + program.memorySize;
			}
		}
		
		System.out.println("top=" + Integer.toHexString(top[0]));
		
		// only need this for user-level...
//		int gp;
//	    for (ELF32Section section : elf.sections) {
//	      if (section.type == ELF32Section.SHT_MIPS_REGINFO) {
//	        file.seek(section.fileOffset + 20);
//	        gp = elf.header.decode(file.readInt());
//	        break;
//	      }
//	    }
		
		for (ELF32Symbol symbol : elf.symbols) {
			if (symbol.getBind() == ELF32Symbol.STB_GLOBAL && symbol.size > 0) {
				sym.put(symbol.value, symbol.name);
			}
		}
		
		System.out.println("symbols=" + sym);
		System.out.println("entry=" + sym.getName(elf.header.entryAddress));
		
		cpu.setPc(elf.header.entryAddress);
		
		return cpu;
	}
	
	/**
	 * store argument and environment vectors and load argument registers ready for a
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
	
	private static String gpRegString (final Cpu cpu) {
		final int pc = cpu.getPc();
		final int[] reg = cpu.getRegisters();
		final Memory mem = cpu.getMemory();
		final Symbols syms = mem.getSymbols();
		
		final StringBuilder sb = new StringBuilder(256);
		sb.append("pc=").append(syms.getName(pc));
		for (int n = 0; n < reg.length; n++) {
			if (reg[n] != 0) {
				sb.append(" ").append(gpRegName(n)).append("=").append(syms.getName(reg[n]));
			}
		}
		return sb.toString();
	}
	
	private static String cpRegString (Cpu cpu) {
		final int[] reg = cpu.getCpRegisters();
		final Memory mem = cpu.getMemory();
		final Symbols syms = mem.getSymbols();
		
		final StringBuilder sb = new StringBuilder(256);
		sb.append("cycle=").append(cpu.getCycle());
		for (int n = 0; n < reg.length; n++) {
			final int v = reg[n];
			if (v != 0) {
				sb.append(" ").append(cpRegName(n / 8, n % 8)).append("=").append(syms.getName(v));
			}
		}
		return sb.toString();
	}
	
}
