package sys.mips;

import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

import sys.elf.*;
import sys.util.Symbols;

import static sys.mips.MemoryUtil.*;
import static sys.mips.CpuConstants.*;
import static sys.mips.InstructionUtil.*;

/**
 * Cpu helper functions
 */
public class CpuUtil {
	
	public static final long NS_IN_S = 1000000000;
	
	/** load elf file into cpu, set entry point, return max address */
	public static Cpu loadElf (final FileChannel chan, final int memsize, final int[] top) throws Exception {
		MappedByteBuffer buf = chan.map(FileChannel.MapMode.READ_ONLY, 0, chan.size());
		ELF32 elf = new ELF32(buf);
		//System.out.println("elf=" + elf);
		elf.print(System.out);
		
		Cpu cpu = new Cpu(memsize, elf.header.data == ELF32Header.ELFDATA2LSB);
		Memory mem = cpu.getMemory();
		Symbols sym = cpu.getSymbols();
		
		top[0] = 0;
		
		for (ELF32Program program : elf.programs) {
			if (program.type == ELF32Program.PT_LOAD) {
				System.out.println("ph=" + program);
				buf.position(program.fileOffset);
				final byte[] data = new byte[program.memorySize];
				buf.get(data, 0, program.fileSize);
				MemoryUtil.storeBytes(mem, program.physicalAddress, data);
				top[0] = program.physicalAddress + program.memorySize;
			}
		}
		
		System.out.println("top=" + Integer.toHexString(top[0]));
		
		// bit of a hack, put the non global symbols in first, then let the global ones overwrite them
		for (ELF32Symbol symbol : elf.symbols) {
			if (symbol.getBind() != ELF32Symbol.STB_GLOBAL) {
				sym.put(symbol.value, symbol.name);
			}
		}
		for (ELF32Symbol symbol : elf.symbols) {
			if (symbol.getBind() == ELF32Symbol.STB_GLOBAL && symbol.size > 0) {
				sym.put(symbol.value, symbol.name);
			}
		}
		
		System.out.println("symbols=" + sym);
		System.out.println("entry=" + sym.getNameOffset(elf.header.entryAddress));
		
		cpu.setPc(elf.header.entryAddress);
		
		return cpu;
	}
	
	/**
	 * store argument and environment vectors and load argument registers ready for a
	 * call to a main() function
	 */
	public static void setMainArgs (final Cpu cpu, final int addr, final List<String> argsList, final List<String> envList) {
		System.out.println("set args=" + argsList + " env=" + envList);
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
		
		System.out.println("argc=" + argsList.size());
		cpu.setRegister(REG_A0, argsList.size());
		System.out.println("argv=" + Integer.toHexString(argvAddr));
		for (int n = 0; n < 10; n++) {
			// argv -> p -> value
			int x = mem.loadWord(argvAddr + n*4);
			System.out.println("argv[" + n + "]=" + Integer.toHexString(x) + " -> " + (x != 0 ? loadString(mem, x) : "<null>"));
			if (x == 0) {
				break;
			}
		}
		cpu.setRegister(REG_A1, argvAddr);
		System.out.println("env=" + Integer.toHexString(envAddr));
		cpu.setRegister(REG_A2, envAddr);
	}
	
	public static String gpRegString (final Cpu cpu, String[] prev) {
		final int[] reg = cpu.getRegisters();
		final Memory mem = cpu.getMemory();
		final Symbols syms = cpu.getSymbols();
		
		final StringBuilder sb = new StringBuilder(256);
		for (int n = 0; n < reg.length; n++) {
			final String value = syms.getNameAddrOffset(reg[n]);
			if (prev == null || prev[n] == null || !prev[n].equals(value)) {
				final String name = gpRegName(n);
				if (sb.length() > 0) {
					sb.append(" ");
				}
				sb.append(name).append("=").append(value);
				if (prev != null) {
					prev[n] = value;
				}
			}
		}
		if (sb.length() > 0) {
			return sb.toString();
		} else {
			return "unchanged";
		}
	}
	
	private static String cpRegString (Cpu cpu) {
		final int[] reg = cpu.getCpRegisters();
		final Memory mem = cpu.getMemory();
		final Symbols syms = cpu.getSymbols();
		
		final StringBuilder sb = new StringBuilder(256);
		sb.append("cycle=").append(cpu.getCycle());
		for (int n = 0; n < reg.length; n++) {
			final int v = reg[n];
			if (v != 0) {
				sb.append(" ").append(cpRegName(n / 8, n % 8)).append("=").append(syms.getNameOffset(v));
			}
		}
		return sb.toString();
	}
	
}
