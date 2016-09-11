package sys.elf;

import java.nio.ByteBuffer;

/**
 * An ELF 32bit section header
 */
public class ELF32Section {
	
	public static final int SECTION_SIZE = 40;
	
	/**
	 * Section types
	 */
	public static final byte SHT_PROGBITS = 1;
	public static final byte SHT_SYMTAB = 2;
	public static final byte SHT_STRTAB = 3;
	public static final byte SHT_RELA = 4;
	public static final byte SHT_DYNAMIC = 6;
	public static final byte SHT_NOTE = 7;
	public static final byte SHT_NOBITS = 8;
	public static final byte SHT_REL = 9;
	
	/** a mips elf32_reginfo table */
	public static final int SHT_MIPS_REGINFO = 0x70000006;
	
	/** section should be writeable at run time */
	public static final int SHF_WRITE = 1;
	/** section occupies memory at run time */
	public static final int SHF_ALLOC = 2;
	/** section contains code */
	public static final int SHF_EXECINSTR = 4;
	
	/** index of name in string table */
	public final int nameIndex;
	/** type, e.g. symbol table, reginfo, or relocation entries */
	public final int type;
	/** section should be allocated/writeable/executable */
	public final int flags;
	/** address this section should be loaded at */
	public final int address;
	/** offset in file of this section */
	public final int fileOffset;
	/** size of this section in the file */
	public final int fileSize;
	public final int linkedSection;
	public final int info;
	public final int addressAlign;
	/** size of each record in the section */
	public final int entrySize;
	
	public String name;
	
	/**
	 * Read section header from given DataInput
	 */
	public ELF32Section (ELF32Header header, ByteBuffer buf) {
		nameIndex = header.decode(buf.getInt());
		type = header.decode(buf.getInt());
		flags = header.decode(buf.getInt());
		address = header.decode(buf.getInt());
		fileOffset = header.decode(buf.getInt());
		fileSize = header.decode(buf.getInt());
		linkedSection = header.decode(buf.getInt());
		info = header.decode(buf.getInt());
		addressAlign = header.decode(buf.getInt());
		entrySize = header.decode(buf.getInt());
	}
	
	private String typeString () {
		switch (type) {
			case SHT_RELA:
				return "rela";
			case SHT_DYNAMIC:
				return "dynamic";
			case SHT_NOTE:
				return "note";
			case SHT_PROGBITS:
				return "progbits";
			case SHT_SYMTAB:
				return "symtab";
			case SHT_STRTAB:
				return "strtab";
			case SHT_NOBITS:
				return "nobits";
			case SHT_REL:
				return "rel";
			case SHT_MIPS_REGINFO:
				return "reginfo";
			default:
				return Integer.toHexString(type);
		}
	}
	
	private String flagString () {
		StringBuilder sb = new StringBuilder();
		sb.append(Integer.toHexString(flags));
		if ((flags & SHF_WRITE) != 0) {
			sb.append(" write");
		}
		if ((flags & SHF_EXECINSTR) != 0) {
			sb.append(" exec");
		}
		if ((flags & SHF_ALLOC) != 0) {
			sb.append(" alloc");
		}
		return sb.toString();
	}
	
	@Override
	public String toString () {
		String s = "ELF32Section[%s type=%s flags=%s addr=x%x offset=x%x size=%d link=x%x info=x%x align=x%x entsize=%d]";
		return String.format(s, name, typeString(), flagString(), address, fileOffset, fileSize, linkedSection, info, addressAlign, entrySize);
	}
	
}
