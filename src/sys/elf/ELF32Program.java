package sys.elf;

import java.nio.ByteBuffer;

/**
 * An ELF program header
 */
public class ELF32Program {
	
	public static final int PROGRAM_SIZE = 32;
	
	public static final int PT_NULL = 0;
	/** a block to load */
	public static final int PT_LOAD = 1;
	public static final int PT_DYNAMIC = 2;
	/** Path to interpreter (dynamic linker, ld-linux) */
	public static final int PT_INTERP = 3;
	public static final int PT_NOTE = 4;
	public static final int PT_SHLIB = 5;
	public static final int PT_PHDR = 6;
	public static final int PT_LOPROC = 0x70000000;
	public static final int PT_HIPROC = 0x7fffffff;
	/** register usage for shared object */
	public static final int PT_MIPS_REGINFO = 0x70000000;
	
	/** Load if equal to PT_LOAD */
	public final int type;
	/** Offset of section in file */
	public final int fileOffset;
	/** Where to load in memory */
	public final int virtualAddress;
	public final int physicalAddress;
	/** Length in file of section */
	public final int fileSize;
	/** Length in memory (greater than or equal to filesz) */
	public final int memorySize;
	/** 1 = exec, 2 = write, 4 = read */
	public final int flags;
	public final int align;
	
	public ELF32Program (ELF32Header header, ByteBuffer buf) {
		type = header.decode(buf.getInt());
		fileOffset = header.decode(buf.getInt());
		virtualAddress = header.decode(buf.getInt());
		physicalAddress = header.decode(buf.getInt());
		fileSize = header.decode(buf.getInt());
		memorySize = header.decode(buf.getInt());
		flags = header.decode(buf.getInt());
		align = header.decode(buf.getInt());
	}
	
	public String typeString () {
		switch (type) {
			case PT_NULL:
				return "null";
			case PT_LOAD:
				return "load";
			case PT_DYNAMIC:
				return "dynamic";
			case PT_INTERP:
				return "interp";
			case PT_NOTE:
				return "note";
			case PT_SHLIB:
				return "shlib";
			case PT_PHDR:
				return "phdr";
			default:
				return Integer.toHexString(type);
		}
	}
	
	@Override
	public String toString () {
		return "ELF32Program [type=" + typeString() + ", fileOffset=" + fileOffset + ", virtualAddress=0x" + Integer.toHexString(virtualAddress)
				+ ", physicalAddress=0x" + Integer.toHexString(physicalAddress) + ", fileSize=" + fileSize + ", memorySize=" + memorySize + ", flags=" + flags
				+ ", align=" + align + "]";
	}
	
}
