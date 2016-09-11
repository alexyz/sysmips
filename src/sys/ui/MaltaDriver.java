package sys.ui;

import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;

import sys.mips.Cpu;
import sys.mips.CpuUtil;

public class MaltaDriver {

	public static void main(String[] args) throws Exception {
		final Path path = FileSystems.getDefault().getPath("images/vmlinux-3.2.0-4-4kc-malta");
		final int memsize = 0x2000000;
		final List<String> kargs = Arrays.asList("debug", "initcall_debug", "ignore_loglevel");
		final List<String> kenv = Arrays.asList("memsize", String.valueOf(memsize));
		final Cpu cpu;
		final int[] top = new int[1];
		try (FileChannel chan = FileChannel.open(path, StandardOpenOption.READ)) {
			cpu = CpuUtil.loadElf(chan, memsize, top);
		}
		CpuUtil.setMainArgs(cpu, top[0] + 0x100000, kargs, kenv);
		cpu.getMemory().print(System.out);
//		System.out.println("press enter");
//		while (System.in.read() != '\n') {
//			//
//		}
		cpu.run();
	}
	
}
