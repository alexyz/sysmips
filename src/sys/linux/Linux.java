package sys.linux;

import java.io.*;
import java.util.*;

import sys.mips.*;
import static sys.mips.MipsConstants.*;
import static sys.mips.MemoryUtil.*;

public class Linux {
	
	public static void main (String[] args) throws Exception {
		
		final Malta malta = new Malta();
		final Cpu cpu = malta.getCpu();
		final Memory mem = cpu.getMemory();
		
		try (RandomAccessFile file = new RandomAccessFile(args[0], "r")) {
			final int top = CpuLoader.loadElf(cpu, file);
			// command line of console=ttyS0 initrd=? root=?
			// TODO environment keys: ethaddr, modetty0, memsize (defaults to 32MB)
			
			final List<String> argsList = Arrays.asList("console=ttyS0");
			final List<Integer> argv = new ArrayList<>();
			final List<String> envList = Arrays.asList("key", "value");
			final List<Integer> env = new ArrayList<>();

			int p = top + 0x100000;
			p = storeStrings(mem, p, argv, argsList);
			p = storeStrings(mem, p, env, envList);
			final int argvAddr = nextWord(p);
			p = storeWords(mem, argvAddr, argv);
			final int envAddr = nextWord(p);
			p = storeWords(mem, envAddr, env);
			
			cpu.setRegister(REG_A0, argsList.size());
			cpu.setRegister(REG_A1, argvAddr);
			cpu.setRegister(REG_A2, envAddr);
		}
		
		System.out.println("memory=" + mem);
		mem.print(System.out);
		
		MaltaJFrame frame = new MaltaJFrame();
		frame.setVisible(true);
		malta.getSupport().addPropertyChangeListener(frame);
		try {
			cpu.run();
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
		Thread.sleep(60000);
		System.exit(1);
	}
	
}
