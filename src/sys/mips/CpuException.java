package sys.mips;

/** internal exception */
public class CpuException extends RuntimeException {
	
	public final int extype;
	public final int vaddr;

	public CpuException (int extype, int vaddr) {
		this.extype = extype;
		this.vaddr = vaddr;
	}
	
	@Override
	public String toString () {
		return "CpuException[" + extype + ", " + Integer.toHexString(vaddr) + "]";
	}
}
