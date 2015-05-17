package sys.mips;

import static sys.mips.MipsConstants.*;

/**
 * Floating point rounder
 */
public abstract class FpRound {
	
	/**
	 * Round towards zero (truncate)
	 */
	public static final FpRound ZERO = new FpRound() {
		@Override
		public final double round (double d) {
			return d > 0.0 ? StrictMath.floor(d) : StrictMath.ceil(d);
		}
	};
	
	/**
	 * Round towards positive infinity
	 */
	public static final FpRound POSINF = new FpRound() {
		@Override
		public final double round (double d) {
			return StrictMath.ceil(d);
		}
	};
	
	/**
	 * Round towards negative infinity
	 */
	public static final FpRound NEGINF = new FpRound() {
		@Override
		public final double round (double d) {
			return StrictMath.floor(d);
		}
	};
	
	/**
	 * No rounding
	 */
	public static final FpRound NONE = new FpRound() {
		@Override
		public final double round (double d) {
			return d;
		}
	};
	
	/**
	 * get the appropriate rounding mode for the value of the fp condition and status register
	 */
	public static FpRound getInstance (int fcsr) {
		switch (fcsr & 0x3) {
			case FCSR_RM_RN:
				return FpRound.NONE;
			case FCSR_RM_RZ:
				return FpRound.ZERO;
			case FCSR_RM_RP:
				return FpRound.POSINF;
			case FCSR_RM_RM:
				return FpRound.NEGINF;
			default: 
				throw new RuntimeException();
		}
	}
	
	/**
	 * apply the rounding
	 */
	public abstract double round (double d);
	
}
