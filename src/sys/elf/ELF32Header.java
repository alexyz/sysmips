package sys.elf;

import java.io.*;

/**
 * ELF header file
 */
public class ELF32Header {
	
	public static final int HEADER_SIZE = 52;
	
	/** these appear at the start of every elf file */
	public static final int ELF_MAGIC = 0x7f454c46;
	public static final int ET_NONE = 0;
	public static final int ET_REL = 1;
	/** executable file */
	public static final int ET_EXEC = 2;
	/** shared object file */
	public static final int ET_DYN = 3;
	public static final int ET_CORE = 4;
	public static final int ET_LOPROC = 0xff00;
	public static final int ET_HIPROC = 0xffff;
	public static final int EM_MIPS = 8;
	public static final int SHN_UNDEF = 0;
	public static final int EV_CURRENT = 1;
	
	/** type (2 = executable) */
	public final short type;
	/** machine type */
	public final short machine;
	public final int version;
	/** entry point of program */
	public final int entryAddress;
	/** offset of first program header table in file */
	public final int programHeaderOffset;
	/** offset of first section header table in file */
	public final int sectionHeaderOffset;
	/** machine specific flags */
	public final int flags;
	public final short headerSize;
	public final short programHeaderSize;
	/** number of program header table entries */
	public final short programHeaders;
	public final short sectionHeaderSize;
	/** number of section header table entries */
	public final short sectionHeaders;
	/** index number of the section header describing the string table */
	public final short stringTableSection;
	
	/** load elf header from file */
	public ELF32Header (DataInput f) throws IOException {
		int e_ident = f.readInt();
		if (e_ident != ELF_MAGIC) {
			throw new IOException("Not an ELF file");
		}
		f.skipBytes(12);
		type = f.readShort();
		machine = f.readShort();
		version = f.readInt();
		entryAddress = f.readInt();
		programHeaderOffset = f.readInt();
		sectionHeaderOffset = f.readInt();
		flags = f.readInt();
		headerSize = f.readShort();
		programHeaderSize = f.readShort();
		programHeaders = f.readShort();
		sectionHeaderSize = f.readShort();
		sectionHeaders = f.readShort();
		stringTableSection = f.readShort();
	}
	
	public String typeString () {
		switch (type) {
			case ET_CORE:
				return "core";
			case ET_DYN:
				return "shared";
			case ET_EXEC:
				return "executable";
			case ET_REL:
				return "relocatable";
			default:
				return Integer.toString(type);
		}
	}
	
	public String machineString () {
		switch (machine) {
			case EM_MIPS:
				return "mips";
			default:
				return Integer.toString(machine);
		}
	}
	
	@Override
	public String toString () {
		return "ELF32Header [type=" + typeString() + ", machine=" + machineString() + ", version=" + version + ", entryAddress=0x"
				+ Integer.toHexString(entryAddress) + ", programHeaderOffset=" + programHeaderOffset + ", sectionHeaderOffset=" + sectionHeaderOffset
				+ ", flags=" + flags + ", headerSize=" + headerSize + ", programHeaderSize=" + programHeaderSize + ", programHeaders=" + programHeaders
				+ ", sectionHeaderSize=" + sectionHeaderSize + ", sectionHeaders=" + sectionHeaders + ", stringTableSection=" + stringTableSection + "]";
	}
	
}
