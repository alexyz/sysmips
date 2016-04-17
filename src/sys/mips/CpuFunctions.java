package sys.mips;

import java.util.Random;

/**
 * mips instruction/register decoding and encoding
 */
public final class CpuFunctions {
	
	public static final long ZX_INT_MASK = 0xffff_ffffL;
	public static final int ZX_SHORT_MASK = 0xffff;
	
	private static final Random RANDOM = new Random();

	/**
	 * zero extend int to long (beware implicit sign extension of byte/short
	 * parameters to int)
	 */
	public static final long zeroExtendInt (final int x) {
		return x & ZX_INT_MASK;
	}
	
	/**
	 * zero extend short to int (beware implicit sign extension of byte
	 * parameters to short)
	 */
	public static final int zeroExtendShort (final short x) {
		return x & ZX_SHORT_MASK;
	}

	/** same as rs */
	public static final int base (final int isn) {
		return rs(isn);
	}
	
	/** calculate branch target */
	public static final int branch (final int isn, final int pc) {
		return pc + (simm(isn) * 4);
	}
	
	/** same as sa */
	public static final int fd (final int isn) {
		return sa(isn);
	}
	
	public static final int fn (final int isn) {
		return isn & 0x3f;
	}
	
	/** fp instruction true flag */
	public static boolean fptf (final int isn) {
		// see BC1F
		return (isn & 0x10000) != 0;
	}
	
	/** fp instruction nullify delay */
	public static boolean fpnd (final int isn) {
		// see BC1FL
		return (isn & 0x20000) != 0;
	}
	
	/** fp instruction condition code flag (0-7) */
	public static final int fpcc (final int isn) {
		// see BC1F
		return (isn >> 18) & 7;
	}
	
	/** same as rd */
	public static final int fs (final int isn) {
		return rd(isn);
	}
	
	/** same as rt */
	public static final int ft (final int isn) {
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
	
	public static final int op (final int isn) {
		return isn >>> 26;
	}
	
	public static final int rd (final int isn) {
		return (isn >>> 11) & 0x1f;
	}
	
	/** same as rs */
	public static final int fmt (final int isn) {
		return rs(isn);
	}
	
	/** same as rs */
	public static final int fr (final int isn) {
		return rs(isn);
	}
	
	/** same as base, fpu fmt and fr */
	public static final int rs (final int isn) {
		return (isn >>> 21) & 0x1f;
	}
	
	/** same as ft */
	public static final int rt (final int isn) {
		return (isn >>> 16) & 0x1f;
	}
	
	public static final int sa (final int isn) {
		return (isn >>> 6) & 0x1f;
	}
	
	/** coprocessor 0 register selection (0-7) */
	public static final int sel (final int isn) {
		return isn & 0x7;
	}
	
	/** signed immediate */
	public static final short simm (final int isn) {
		return (short) isn;
	}
	
	/** syscall or break number */
	public static final int syscall (final int isn) {
		return (isn >>> 6) & 0xfffff;
	}

	public static final float loadSingle (final int[] fpReg, final int i) {
		return Float.intBitsToFloat(fpReg[i]);
	}
	
	public static final void storeSingle (final int[] fpReg, final int i, final float f) {
		fpReg[i] = Float.floatToRawIntBits(f);
	}

	public static final double loadDouble (final int[] fpReg, final int i) {
		if ((i & 1) == 0) {
			final long mask = 0xffffffffL;
			return Double.longBitsToDouble((fpReg[i] & mask) | ((fpReg[i + 1] & mask) << 32));
		} else {
			throw new IllegalArgumentException("unaligned " + i);
		}
	}
	
	public static final void storeDouble (final int[] fpReg, final int i, final double d) {
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
	public static final boolean fccrFcc (final int[] fpControlReg, final int cc) {
		if (cc >= 0 && cc < 8) {
			return (fpControlReg[CpuConstants.FPCR_FCCR] & (1 << cc)) != 0;
			
		} else {
			throw new IllegalArgumentException("invalid fpu cc " + cc);
		}
	}
	
	/** return the virtual page number / 2 of the virtual address (19 bits) */
	public static final int vpn2 (final int vaddr) {
		// 19 bits (1+19+12)
		//return (vaddr >> 12) & 0x7ffff;
		return vaddr >>> 13;
	}
	
	/** return the even/odd index (the last bit of the vpn) */
	public static final int evenodd (final int vaddr) {
		return (vaddr >> 12) & 1;
	}
	
	/** return random tlb entry */
	public static final int random () {
		return RANDOM.nextInt() & 0xf;
	}
	
	private CpuFunctions () {
		//
	}
}
