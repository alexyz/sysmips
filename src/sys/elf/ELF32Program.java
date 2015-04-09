package sys.elf;

import java.io.*;

/**
 * An ELF program header
 */
public class ELF32Program {
	
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
	/**
	 * register usage for shared object, mips psabi page 86
	 */
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
	
	/**
	 * Load program header from given DataInput
	 */
	public ELF32Program (DataInput f) throws IOException {
		type = f.readInt();
		fileOffset = f.readInt();
		virtualAddress = f.readInt();
		physicalAddress = f.readInt();
		fileSize = f.readInt();
		memorySize = f.readInt();
		flags = f.readInt();
		align = f.readInt();
	}
	
	/**
	 * Return string of the type of this program header
	 */
	public String type () {
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
			case PT_MIPS_REGINFO:
				return "reginfo";
			default:
				return Integer.toHexString(type);
		}
	}
	
	/**
	 * Print a full description of this header
	 */
	@Override
	public String toString () {
		String f = "ELF32Program[type: %s offset: %s vaddr: x%x paddr: x%x filesz: %d memsize: %d flags: x%x align: %d]";
		return String.format(f, type(), fileOffset, virtualAddress, physicalAddress, fileSize, memorySize, flags, align);
	}
	
}
