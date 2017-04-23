package sys.malta;

import sys.mips.Device;

public class MaltaRev extends Device {
	
	public MaltaRev (final int baseAddr) {
		super(baseAddr);
	}
	
	@Override
	public boolean isMapped (int addr) {
		return offset(addr) == 0;
	}
	
	@Override
	public int loadWord (final int addr) {
		return 1;
	}
}
