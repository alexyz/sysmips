package sys.malta;

import sys.util.Symbols;

public interface Device {
	
	public int systemRead (int addr, int size);
	
	public void systemWrite (final int addr, final int value, int size);
	
	public void init (Symbols sym, int offset);
}
