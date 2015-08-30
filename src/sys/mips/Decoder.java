package sys.mips;

/**
 * mips instruction/register decoding and encoding
 */
public class Decoder {

	/** same as rs */
	public static int base (final int isn) {
		return rs(isn);
	}
	
	/** branch target */
	public static int branch (final int isn, final int pc) {
		return pc + (simm(isn) * 4);
	}
	
	/** same as sa */
	public static int fd (final int isn) {
		return sa(isn);
	}
	
	public static int fn (final int isn) {
		return isn & 0x3f;
	}
	
	/** fp instruction true flag */
	public static boolean fptf (final int isn) {
		// see BC1F
		return (isn & 0x10000) != 0;
	}
	
	/** fp instruction condition code flag (0-7) */
	public static int fpcc (final int isn) {
		// see BC1F
		return (isn >> 18) & 7;
	}
	
	/** same as rd */
	public static int fs (final int isn) {
		return rd(isn);
	}
	
	/** same as rt */
	public static int ft (final int isn) {
		return rt(isn);
	}
	
	/** unsigned immediate */
	public static final int imm (final int isn) {
		return isn & 0xffff;
	}
	
	/** jump target */
	public static final int jump (final int isn, final int pc) {
		return (pc & 0xf0000000) | ((isn & 0x3FFFFFF) << 2);
	}
	
	public static int op (final int isn) {
		return isn >>> 26;
	}
	
	public static int rd (final int isn) {
		return (isn >>> 11) & 0x1f;
	}
	
	/** same as rs */
	public static int fmt (final int isn) {
		return rs(isn);
	}
	
	/** same as rs */
	public static int fr (final int isn) {
		return rs(isn);
	}
	
	/** same as base, fpu fmt and fr */
	public static int rs (final int isn) {
		return (isn >>> 21) & 0x1f;
	}
	
	/** same as ft */
	public static int rt (final int isn) {
		return (isn >>> 16) & 0x1f;
	}
	
	public static int sa (final int isn) {
		return (isn >>> 6) & 0x1f;
	}
	
	/** coprocessor 0 register selection (0-7) */
	public static int sel (final int isn) {
		return isn & 0x7;
	}
	
	/** sign extended immediate */
	public static final int simm (final int isn) {
		return (short) isn;
	}
	
	/** syscall or break number */
	public static final int syscall (final int isn) {
		return (isn >>> 6) & 0xfffff;
	}

	public static float loadSingle (final int[] fpReg, final int i) {
		return Float.intBitsToFloat(fpReg[i]);
	}
	
	public static void storeSingle (final int[] fpReg, final int i, final float f) {
		fpReg[i] = Float.floatToRawIntBits(f);
	}

	public static double loadDouble (final int[] fpReg, final int i) {
		if ((i & 1) == 0) {
			final long mask = 0xffffffffL;
			return Double.longBitsToDouble((fpReg[i] & mask) | ((fpReg[i + 1] & mask) << 32));
		} else {
			throw new IllegalArgumentException("unaligned " + i);
		}
	}
	
	public static void storeDouble (final int[] fpReg, final int i, final double d) {
		if ((i & 1) == 0) {
			final long dl = Double.doubleToRawLongBits(d);
			// the spec says...
			fpReg[i] = (int) dl;
			fpReg[i + 1] = (int) (dl >>> 32);
		} else {
			throw new IllegalArgumentException("unaligned " + i);
		}
	}

	/** floating point condition code register condition */
	public static boolean fccrFcc (final int[] fpControlReg, final int cc) {
		if (cc >= 0 && cc < 8) {
			return (fpControlReg[MipsConstants.FPCR_FCCR] & (1 << cc)) != 0;
			
		} else {
			throw new IllegalArgumentException("invalid fpu cc " + cc);
		}
	}
	
}
