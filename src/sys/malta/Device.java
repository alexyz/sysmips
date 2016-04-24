package sys.malta;

import sys.util.Symbols;

/**
 * device interface. all addresses are physical (so you can look them up in the
 * symbol table) and need to be translated to offsets by the device itself.
 */
public interface Device {
	
	/** init symbol table with symbols from this device */
	public void init (final Symbols sym);
	
	/** return true if read/write at this physical address is allowed */
	public boolean isMapped (final int addr);
	
	/** read at the physical address (size must be 1, 2 or 4) */
	public int systemRead (final int addr, final int size);
	
	/** write at the physical address (size must be 1, 2 or 4) */
	public void systemWrite (final int addr, final int size, final int value);
	
}
