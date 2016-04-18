package sys.malta;

import sys.util.Symbols;

public interface Device {
	
	/** init symbol table */
	public void init (Symbols sym);
	
	/** return true if read/write at this address is allowed */
	public boolean isMapped (int addr);
	
	/** read at the address */
	public int systemRead (int addr, int size);
	
	/** write at the address */
	public void systemWrite (final int addr, int size, final int value);
}
