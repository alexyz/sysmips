package sys.linux;

import java.io.*;
import java.util.*;

import sys.elf.*;
import sys.mips.*;

public class Linux {
	
	public static BufferedReader br;
	
	public static void main (String[] args) throws Exception {
		
		br = new BufferedReader(new InputStreamReader(System.in));
		
		ELF32 elf;
		Memory mem;
		
		try (RandomAccessFile file = new RandomAccessFile("images/vmlinux", "r")) {
			elf = new ELF32(file);
			System.out.println("elf=" + elf);
			//elf.print(System.out);
			
			mem = new Memory();
			System.out.println("mem=" + mem);
			for (int ph = 0; ph < elf.header.programHeaders; ph++) {
				ELF32Program program = elf.programs[ph];
				if (program.type == ELF32Program.PT_LOAD) {
					file.seek(program.fileOffset);
					final byte[] data = new byte[program.memorySize];
					file.read(data, 0, program.fileSize);
					mem.storeBytes(program.virtualAddress, data);
				}
			}
		}
		
		SortedMap<Long,String> symMap = new TreeMap<>();
		for (ELF32Symbol symbol : elf.symbols) {
			if (symbol.getBind() == ELF32Symbol.STB_GLOBAL) {
				symMap.put(symbol.valueAddress & 0xffffffffL, symbol.name);
			}
		}
		Symbols syms = new Symbols(symMap);
		// TODO add malta symbols
		System.out.println("syms=" + syms);
		
		// TODO cmdline of console=ttyS0 initrd=? root=?
		// grub-2.00\grub-core\loader\mips\linux.c
		// state.gpr[1] = entry_addr;
		// state.gpr[4] = linux_argc;
		// state.gpr[5] = target_addr + argv_off;
		// where argv is phdr->p_paddr + phdr->p_memsz + 0x100000
		Cpu cpu = new Cpu(mem, syms);
		cpu.setPc(elf.header.entryAddress);
		
		Malta malta = new Malta(cpu);
		mem.setMalta(malta);
		
		MaltaJFrame frame = new MaltaJFrame(malta);
		frame.setVisible(true);
		
		cpu.run();
	}
	
}
