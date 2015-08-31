package sys.mips;

import static sys.mips.Constants.*;
import static sys.mips.Decoder.*;

/**
 * abstraction layer for single/double fpu registers
 */
public abstract class FpFormat {
	
	public static final FpFormat SINGLE = new FpFormat() {
		@Override
		public void store (int[] fpReg, int reg, double d) {
			storeSingle(fpReg, reg, (float) d);
		}
		
		@Override
		public double load (int[] fpReg, int reg) {
			return loadSingle(fpReg, reg);
		}
	};
	
	public static final FpFormat DOUBLE = new FpFormat() {
		
		@Override
		public void store (int[] fpReg, int reg, double d) {
			storeDouble(fpReg, reg, d);
		}
		
		@Override
		public double load (int[] fpReg, int reg) {
			return loadDouble(fpReg, reg);
		}
	};

	/** get access for fmt (same as rs) */
	public static FpFormat getInstance (int fmt) {
		switch (fmt) {
			case FP_RS_S: return SINGLE;
			case FP_RS_D: return DOUBLE;
			default: throw new RuntimeException("invalid fmt " + fmt);
		}
	}
	
	/** load value from floating point register */
	public abstract double load (int[] fpReg, int reg);
	
	/** store value to floating point register */
	public abstract void store (int[] fpReg, int reg, double d);
	
}
