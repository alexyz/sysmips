package sys.malta;

import sys.util.Symbols;

public interface Device {
	
	/** init at the physical address the device exists at */
	public void init (Symbols sym, int offset);
	
	/** return true if read/write at this logical address is allowed */
	public boolean isMapped (int addr);
	
	/** read at the logical address */
	public int systemRead (int addr, int size);
	
	/** write at the logical address */
	public void systemWrite (final int addr, final int value, int size);
}
