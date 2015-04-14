package sys.mips;

import static sys.mips.MipsConstants.*;

public abstract class Access {
	
	/** get access for fmt (same as rs) */
	public static Access access (int fmt) {
		switch (fmt) {
			case FP_RS_S: return SINGLE;
			case FP_RS_D: return DOUBLE;
			case FP_RS_W: return WORD;
			default: throw new RuntimeException("invalid fmt " + fmt);
		}
	}
	
	public static final Access SINGLE = new Access() {
		@Override
		public void set (int[] fpReg, int reg, double d) {
			setSingle(fpReg, reg, (float) d);
		}
		
		@Override
		public double get (int[] fpReg, int reg) {
			return getSingle(fpReg, reg);
		}
	};
	
	public static final Access DOUBLE = new Access() {
		
		@Override
		public void set (int[] fpReg, int reg, double d) {
			setDouble(fpReg, reg, d);
		}
		
		@Override
		public double get (int[] fpReg, int reg) {
			return getDouble(fpReg, reg);
		}
	};
	
	public static final Access WORD = new Access() {
		
		@Override
		public void set (int[] fpReg, int reg, double d) {
			fpReg[reg] = (int) d;
		}
		
		@Override
		public double get (int[] fpReg, int reg) {
			return fpReg[reg];
		}
	};
	
	public abstract double get (int[] fpReg, int reg);
	
	public abstract void set (int[] fpReg, int reg, double d);
	
}
