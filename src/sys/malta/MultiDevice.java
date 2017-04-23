package sys.malta;

import java.util.*;

import sys.mips.Device;
import sys.util.Symbols;

/**
 * a device that contains several other devices
 */
public class MultiDevice extends Device {
	
	protected final List<Device> devices = new ArrayList<>();
	
	public MultiDevice (int baseAddr) {
		super(baseAddr);
	}
	
	@Override
	public void init (final Symbols sym) {
		for (Device d : devices) {
			d.init(sym);
		}
	}
	
	@Override
	public boolean isMapped (final int addr) {
		return getMapped(addr) != null;
	}

	@Override
	public byte loadByte (final int addr) {
		Device d = getMapped(addr);
		return d != null ? d.loadByte(addr) : super.loadByte(addr);
	}
	
	@Override
	public void storeByte (final int addr, final byte value) {
		Device d = getMapped(addr);
		if (d != null) {
			d.storeByte(addr, value);
		} else {
			super.storeByte(addr, value);
		}
	}
	
	@Override
	public short loadHalfWord (final int addr) {
		Device d = getMapped(addr);
		return d != null ? d.loadHalfWord(addr) : super.loadHalfWord(addr);
	}
	
	@Override
	public void storeHalfWord (final int addr, final short value) {
		Device d = getMapped(addr);
		if (d != null) {
			d.storeHalfWord(addr, value);
		} else {
			super.storeHalfWord(addr, value);
		}
	}
	
	@Override
	public int loadWord (final int addr) {
		Device d = getMapped(addr);
		return d != null ? d.loadWord(addr) : super.loadWord(addr);
	}
	
	@Override
	public void storeWord (final int addr, final int value) {
		Device d = getMapped(addr);
		if (d != null) {
			d.storeWord(addr, value);
		} else {
			super.storeWord(addr, value);
		}
	}
	
	private Device getMapped (final int addr) {
		for (Device d : devices) {
			if (d.isMapped(addr)) {
				return d;
			}
		}
		return null;
	}
}
