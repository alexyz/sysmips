package sys.mips;

public class Isn {
	
	/** the code at bit position 26 */
	public final int op;
	/** the code at bit position 21 */
	public final int rs;
	/** the code at bit position 16 */
	public final int rt;
	/** the code at bit position 0 */
	public final int fn;
	/** isn name, unique for all instructions */
	public final String name;
	public final String format;
	
	public Isn (String name) {
		this(0, 0, 0, 0, name, "");
	}
	
	public Isn (int op, int rs, int rt, int fn, String name, String format) {
		this.op = op;
		this.rs = rs;
		this.rt = rt;
		this.fn = fn;
		this.name = name;
		this.format = format;
	}
	
	@Override
	public String toString () {
		return getClass().getSimpleName() + "[" + name + "]";
	}
	
}
