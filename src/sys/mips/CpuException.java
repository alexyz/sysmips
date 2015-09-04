package sys.mips;

/** internal exception */
public class CpuException extends RuntimeException {
	
	public final int extype;
	public final int vaddr;
	public final boolean tlbRefill;

	public CpuException (int extype, int vaddr, boolean tlbRefill) {
		this.extype = extype;
		this.vaddr = vaddr;
		this.tlbRefill = tlbRefill;
	}
	
	@Override
	public String toString () {
		return "CpuException[" + IsnUtil.exceptionName(extype) + ", " + Integer.toHexString(vaddr) + ", " + tlbRefill + "]";
	}
}
