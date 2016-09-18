package sys.elf;

import java.nio.ByteBuffer;

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
	/** little endian */
	public static final int ELFDATA2LSB = 1;
	/** big endian */
	public static final int ELFDATA2MSB = 2;
	
	public final byte class_;
	public final byte data;
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
	public ELF32Header (ByteBuffer buf) {
		byte[] i = new byte[16];
		buf.get(i);
		if (!(i[0] == 0x7f && i[1] == 'E' && i[2] == 'L' && i[3] == 'F')) {
			throw new RuntimeException("Not an ELF file");
		}
		class_ = i[4];
		data = i[5];
		type = decode(buf.getShort());
		machine = decode(buf.getShort());
		version = decode(buf.getInt());
		entryAddress = decode(buf.getInt());
		programHeaderOffset = decode(buf.getInt());
		sectionHeaderOffset = decode(buf.getInt());
		flags = decode(buf.getInt());
		headerSize = decode(buf.getShort());
		programHeaderSize = decode(buf.getShort());
		programHeaders = decode(buf.getShort());
		sectionHeaderSize = decode(buf.getShort());
		sectionHeaders = decode(buf.getShort());
		stringTableSection = decode(buf.getShort());
	}
	
	public short decode (short s) {
		switch (data) {
			case 1:
				return Short.reverseBytes(s);
			case 2:
				return s;
			default:
				throw new RuntimeException();
		}
	}
	
	public int decode (int i) {
		switch (data) {
			case 1:
				return Integer.reverseBytes(i);
			case 2:
				return i;
			default:
				throw new RuntimeException();
		}
	}
	
	public String classString () {
		switch (class_) {
			case 1:
				return "32bit";
			case 2:
				return "64bit";
			default:
				return Integer.toHexString(class_);
		}
	}
	
	public String dataString () {
		switch (data) {
			case ELFDATA2LSB:
				return "littleEndian";
			case ELFDATA2MSB:
				return "bigEndian";
			default:
				return Integer.toHexString(data);
		}
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
				return Integer.toHexString(type);
		}
	}
	
	public String machineString () {
		switch (machine) {
			case EM_MIPS:
				return "mips";
			default:
				return Integer.toHexString(machine);
		}
	}
	
	@Override
	public String toString () {
		return "ELF32Header [class=" + classString() + " data=" + dataString() + " type=" + typeString() + ", machine=" + machineString() + ", version="
				+ version + ", entryAddress=0x" + Integer.toHexString(entryAddress) + ", programHeaderOffset=" + programHeaderOffset + ", sectionHeaderOffset="
				+ sectionHeaderOffset + ", flags=" + flags + ", headerSize=" + headerSize + ", programHeaderSize=" + programHeaderSize + ", programHeaders="
				+ programHeaders + ", sectionHeaderSize=" + sectionHeaderSize + ", sectionHeaders=" + sectionHeaders + ", stringTableSection="
				+ stringTableSection + "]";
	}
	
}
