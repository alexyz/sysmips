package sys.mips;

/** internal exception */
public class CpuException extends RuntimeException {
	
	public final EP ep;

	public CpuException (EP ep) {
		this.ep = ep;
	}
	
	@Override
	public String toString () {
		return "CpuException[" + ep + "]";
	}
}
