package sys.mips;

import static sys.mips.MipsConstants.*;

public abstract class Access {
	
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
