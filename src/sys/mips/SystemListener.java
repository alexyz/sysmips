package sys.mips;

public interface SystemListener {
	/** called when data is read from system area */
	public void systemRead(int addr, int value);
	/** called when data is written to system area */
	public void systemWrite(int addr, int value);
}
