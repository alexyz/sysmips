package sys.mips;

public interface Device {
	
	public int systemRead (int addr, int size);
	
	public void systemWrite (final int addr, int size, final int value);
	
	public void init (Symbols sym, int offset);
}
