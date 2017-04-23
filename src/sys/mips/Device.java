package sys.mips;

import sys.util.Symbols;

/**
 * device interface. all addresses are physical (so you can look them up in the
 * symbol table) and need to be translated to offsets by the device itself.
 */
public abstract class Device {
	
	protected final int baseAddr;
	
	public Device (int baseAddr) {
		this.baseAddr = baseAddr;
	}
	
	public int offset (int addr) {
		return addr - baseAddr;
	}
	
	/** init symbol table with symbols from this device */
	public void init (final Symbols sym) {
		//
	}
	
	/** return true if read/write at this physical address is allowed */
	public boolean isMapped (final int addr) {
		throw rx(addr);
	}
	
	/** read at the physical address */
	public byte loadByte (final int addr) {
		throw rx(addr);
	}
	
	/** read at the physical address */
	public short loadHalfWord (final int addr) {
		throw rx(addr);
	}
	
	/** read at the physical address */
	public int loadWord (final int addr) {
		throw rx(addr);
	}
	
	/** write at the physical address */
	public void storeByte (final int addr, final byte value) {
		throw rx(addr);
	}
	
	/** write at the physical address */
	public void storeHalfWord (final int addr, final short value) {
		throw rx(addr);
	}
	
	/** write at the physical address */
	public void storeWord (final int addr, final int value) {
		throw rx(addr);
	}
	
	private RuntimeException rx (int a) {
		return new RuntimeException(String.format("unimplemented: %s %x", getClass().getSimpleName(), a));
	}
}
