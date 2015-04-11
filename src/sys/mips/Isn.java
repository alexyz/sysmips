package sys.mips;

import static sys.mips.MipsConstants.*;

public class Isn {
	
	public static Isn newOp (int op, String name, String asm, String disasm) {
		return new Isn(op, 0, 0, 0, name, asm, disasm);
	}
	
	public static Isn newRegimm (int rt, String name, String asm, String disasm) {
		return new Isn(OP_REGIMM, 0, rt, 0, name, asm, disasm);
	}
	
	public static Isn newFn (int fn, String name, String asm, String disasm) {
		return new Isn(OP_SPECIAL, 0, 0, fn, name, asm, disasm);
	}
	
	public static Isn newFn2 (int fn, String name, String asm, String disasm) {
		return new Isn(OP_SPECIAL2, 0, 0, fn, name, asm, disasm);
	}
	
	public static Isn newCop0 (int rs, String name, String asm, String disasm) {
		return new Isn(OP_COP0, rs, 0, 0, name, asm, disasm);
	}
	
	public static Isn newCop0Fn (int fn, String name, String asm, String disasm) {
		return new Isn(OP_COP0, CP_RS_CO, 0, fn, name, asm, disasm);
	}
	
	public static Isn newCop1 (int rs, String name, String asm, String disasm) {
		return new Isn(OP_COP1, rs, 0, 0, name, asm, disasm);
	}
	
	public static Isn newCop1Fn (int rs, int fn, String name, String asm, String disasm) {
		// only add these for S, though they apply to D, W and L as well
		return new Isn(OP_COP1, rs, 0, fn, name, asm, disasm);
	}
	
	/** the code at bit position 26 */
	public final int op;
	/** the code at bit position 21 */
	public final int rs;
	/** the code at bit position 16 */
	public final int rt;
	/** the code at bit position 0 */
	public final int fn;
	public final String name;
	public final String type;
	public final String disasm;
	
	public Isn (String name) {
		this(0, 0, 0, 0, name, null, null);
	}
	
	private Isn (int op, int rs, int rt, int fn, String name, String asm, String disasm) {
		this.op = op;
		this.rs = rs;
		this.rt = rt;
		this.fn = fn;
		this.name = name;
		this.type = asm;
		this.disasm = disasm;
	}
	
	@Override
	public String toString () {
		return getClass().getSimpleName() + "[" + name + " " + type + "]";
	}
	
}
