package sys.mips;

/** internal exception */
public class CpuException extends RuntimeException {
	
	public final CpuExceptionParams ep;

	public CpuException (CpuExceptionParams ep) {
		this.ep = ep;
	}
	
	@Override
	public String toString () {
		return "CpuException[" + ep + "]";
	}
}
