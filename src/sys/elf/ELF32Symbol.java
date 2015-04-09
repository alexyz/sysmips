package sys.elf;

import java.io.*;

/**
 * An elf symbol object
 */
public class ELF32Symbol implements Comparable<ELF32Symbol> {
	
	/** symbol table types */
	public static final int STT_NOTYPE = 0;
	public static final int STT_OBJECT = 1;
	public static final int STT_FUNC = 2;
	public static final int STT_SECTION = 3;
	public static final int STT_FILE = 4;
	
	/** symbol binding */
	public static final int STB_LOCAL = 0;
	public static final int STB_GLOBAL = 1;
	public static final int STB_WEAK = 2;
	
	/** symbol name and section name */
	public final String name;
	public final int nameIndex;
	public final int nameSize;
	/** address of object that symbol refers to */
	public final int valueAddress;
	/** symbol type (can be misleading) */
	public final byte info;
	public final byte other;
	/** refers to section */
	public final short section;
	
	/**
	 * Load a symbol from the given DataInput with reference to the given string
	 * table.
	 */
	public ELF32Symbol (DataInput file, byte[] strings) throws IOException {
		nameIndex = file.readInt();
		valueAddress = file.readInt();
		nameSize = file.readInt();
		info = file.readByte();
		other = file.readByte();
		section = file.readShort();
		name = ELF32.readString(strings, nameIndex);
	}
	
	@Override
	public int compareTo (final ELF32Symbol other) {
		final long a1 = valueAddress & 0xffffffffL;
		final long a2 = other.valueAddress & 0xffffffffL;
		int c = a1 > a2 ? 1 : a1 == a2 ? 0 : -1;
		if (c == 0) {
			c = info - other.info;
		}
		return c;
	}
	
	public int getType() {
		return info & 0xf;
	}
	
	public int getBind() {
		return info >>> 4;
	}
	
	/**
	 * Return the type of this symbol
	 */
	private String typeString () {
		final int type = getType();
		switch (type) {
			case STT_NOTYPE:
				return "notype";
			case STT_OBJECT:
				return "object";
			case STT_FUNC:
				return "function";
			case STT_SECTION:
				return "section";
			case STT_FILE:
				return "file";
			default:
				return Integer.toHexString(type);
		}
	}
	
	/**
	 * Return the binding for this symbol
	 */
	private String bindString () {
		final int bind = getBind();
		switch (bind) {
			case STB_LOCAL:
				return "local";
			case STB_GLOBAL:
				return "global";
			case STB_WEAK:
				return "weak";
			default:
				return Integer.toHexString(bind);
		}
	}
	
	private String other () {
		switch (other) {
			case 0:
				return "def";
			case 2:
				return "hide";
			default:
				return Integer.toString(other);
		}
	}
	
	/**
	 * Print a full description of this symbol
	 */
	@Override
	public String toString () {
		return String.format("ELF32Symbol[x%x %s bind=%s type=%s other=%s]", valueAddress, name, bindString(), typeString(), other());
	}
	
}
