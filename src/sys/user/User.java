package sys.user;

import java.io.*;
import java.util.*;

import sys.mips.*;

/**
 * run a linux user mode program
 */
public class User {
	
	public static void main (String[] a) throws Exception {
		Cpu cpu = new LinuxUserCpu();
		cpu.getMemory().initPage(0x400000);
		cpu.getMemory().initPage(0x800000);
		File file = new File("images/hw-mipsel");
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			int top = CpuUtil.loadElf(cpu, raf);
		}
		List<String> args = Arrays.asList("hw");
		List<String> env = Arrays.asList();
		CpuUtil.setMainArgs(cpu, 0x8f0000, args, env);
		cpu.setRegister(MipsConstants.REG_SP, 0x8e0000);
		cpu.run();
	}
	
}
