package sys.mips;

public class Isn {
	
	public final byte op;
	public final byte op2;
	public final byte op3;
	public final String name;
	public final String asm;
	public final String disasm;
	
	public Isn (String name) {
		this((byte) 0, (byte) 0, (byte) 0, name, null, null);
	}
	
	public Isn (byte op, String name, String asm, String disasm) {
		this(op, (byte) 0, (byte) 0, name, asm, disasm);
	}
	
	public Isn (byte op, byte op2, String name, String asm, String disasm) {
		this(op, op2, (byte) 0, name, asm, disasm);
	}
	
	public Isn (byte op, byte op2, byte op3, String name, String asm, String disasm) {
		this.op = op;
		this.op2 = op2;
		this.op3 = op3;
		this.name = name;
		this.asm = asm;
		this.disasm = disasm;
	}
	
	@Override
	public String toString () {
		return getClass().getSimpleName() + "[" + name + ", " + asm + "]";
	}
	
}
