package sys.mips;

/**
 * Floating point rounder
 */
public abstract class Round {
	
	/**
	 * Round towards zero (truncate)
	 */
	public static final Round ZERO = new Round() {
		@Override
		public final double round (double d) {
			return d > 0.0 ? StrictMath.floor(d) : StrictMath.ceil(d);
		}
	};
	
	/**
	 * Round towards positive infinity
	 */
	public static final Round POSINF = new Round() {
		@Override
		public final double round (double d) {
			return StrictMath.ceil(d);
		}
	};
	
	/**
	 * Round towards negative infinity
	 */
	public static final Round NEGINF = new Round() {
		@Override
		public final double round (double d) {
			return StrictMath.floor(d);
		}
	};
	
	/**
	 * No rounding
	 */
	public static final Round NONE = new Round() {
		@Override
		public final double round (double d) {
			return d;
		}
	};
	
	/**
	 * Round this double to a double
	 */
	public abstract double round (double d);
	
}
