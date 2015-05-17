package sys.mips;

import java.io.File;
import java.io.RandomAccessFile;

public class User {
	
	public static void main (String[] args) throws Exception {
		Cpu cpu = new Cpu();
		cpu.getMemory().initPage(0x400000);
		File file = new File("images/hw-mipsel");
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		int top = CpuUtil.loadElf(cpu, raf);
		//CpuUtil.setMainArgs(malta.getCpu(), top + 0x100000, args, env);
	}
	
}
