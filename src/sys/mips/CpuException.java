package sys.mips;

public class CpuException extends RuntimeException {
	
	public enum Type {
		Breakpoint, SystemCall, Trap;
	}
	
	public Type type;
	
	public CpuException (Type type) {
		this(type, null);
	}
	
	public CpuException (Type type, String msg) {
		super(type + (msg != null ? ": " + msg : ""));
		this.type = type;
	}
	
}
