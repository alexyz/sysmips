package sys.mips;

/**
 * A disassembly name and type.
 */
public class Isn {
	
	public final String name;
	public final String format;
	
	public Isn (String name) {
		this(name, "");
	}
	
	/**
	 * Instruction name, unknown type
	 */
	public Isn (String name, String format) {
		this.name = name;
		this.format = format;
	}
	
	@Override
	public String toString () {
		return "Name[" + name + ", " + format + "]";
	}
	
}