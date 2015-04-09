package sys.linux;

import java.io.*;
import java.util.*;

import sys.elf.*;
import sys.mips.*;

public class Linux {
	
	public static BufferedReader br;
	
	public static void main (String[] args) throws Exception {
		
		br = new BufferedReader(new InputStreamReader(System.in));
		
		Malta malta = new Malta();
		
		ELF32 elf;
		try (RandomAccessFile file = new RandomAccessFile("images/vmlinux", "r")) {
			elf = new ELF32(file);
			System.out.println("elf=" + elf);
			//elf.print(System.out);
			malta.load(elf, file);
		}
		
		System.out.println("memory=" + malta.getCpu().getMemory());
		malta.getCpu().getMemory().print(System.out);
		
		// TODO cmdline of console=ttyS0 initrd=? root=?
		// grub-2.00\grub-core\loader\mips\linux.c
		// state.gpr[1] = entry_addr;
		// state.gpr[4] = linux_argc;
		// state.gpr[5] = target_addr + argv_off;
		// where argv is phdr->p_paddr + phdr->p_memsz + 0x100000
		
		MaltaJFrame frame = new MaltaJFrame();
		frame.setVisible(true);
		
		malta.getSupport().addPropertyChangeListener(frame);
		malta.getCpu().run();
	}
	
}
