package sys.mips;

public final class MipsConstants {
	
	/** special meta instruction selected by FN */
	public static final byte OP_SPECIAL = 0x00;
	/** register-immediate meta instruction (selected by RT) */
	public static final byte OP_REGIMM = 0x01;
	/** branch within 256mb region */
	public static final byte OP_J = 0x02;
	/** jump and link */
	public static final byte OP_JAL = 0x03;
	
	/** branch on equal */
	public static final byte OP_BEQ = 0x04;
	/** branch on not equal */
	public static final byte OP_BNE = 0x05;
	/** branch on less than or equal to zero */
	public static final byte OP_BLEZ = 0x06;
	/** branch on greater than zero */
	public static final byte OP_BGTZ = 0x07;
	/** add immediate with overflow exception */
	public static final byte OP_ADDI = 0x08;
	/** add immediate unsigned word */
	public static final byte OP_ADDIU = 0x09;
	/** set on less than immediate. compare both as signed. */
	public static final byte OP_SLTI = 0x0a;
	/** set on less than immediate unsigned */
	public static final byte OP_SLTIU = 0x0b;
	/** and immediate (zero extend) */
	public static final byte OP_ANDI = 0x0c;
	/** or immediate. zx */
	public static final byte OP_ORI = 0x0d;
	/** exclusive or immediate (zx) */
	public static final byte OP_XORI = 0x0e;
	public static final byte OP_LUI = 0x0f;
	/** coprocessor 0 meta instruction selected by RS then by FN */
	public static final byte OP_COP0 = 0x10;
	/** coprocessor 1 meta instruction selected by RS then by FN */
	public static final byte OP_COP1 = 0x11;
	/** meta instruction selected by fn */
	public static final byte OP_SPECIAL2 = 0x1c;
	/** load byte (signed) */
	public static final byte OP_LB = 0x20;
	/** load halfword. sign extend */
	public static final byte OP_LH = 0x21;
	/** load word left */
	public static final byte OP_LWL = 0x22;
	/** load word */
	public static final byte OP_LW = 0x23;
	/** load unsigned byte */
	public static final byte OP_LBU = 0x24;
	/** load halfword unsigned */
	public static final byte OP_LHU = 0x25;
	public static final byte OP_LWR = 0x26;
	/** store byte */
	public static final byte OP_SB = 0x28;
	public static final byte OP_SH = 0x29;
	/** store word left */
	public static final byte OP_SWL = 0x2a;
	/** store word */
	public static final byte OP_SW = 0x2b;
	/** store word right */
	public static final byte OP_SWR = 0x2e;
	/** load linked word synchronised */
	public static final byte OP_LL = 0x30;
	/** load word from mem to coprocessor */
	public static final byte OP_LWC1 = 0x31;
	/** store conditional word */
	public static final byte OP_SC = 0x38;
	
	/** store word from coprocessor to memory */
	public static final byte OP_SWC1 = 0x39;
	
	/** shift word left logical. also nop if sa,rd,rt = 0 */
	public static final byte FN_SLL = 0x00;
	/** shift word right logical */
	public static final byte FN_SRL = 0x02;
	/** shift word right arithmetic (preserve sign) */
	public static final byte FN_SRA = 0x03;
	/** shift word left logical variable */
	public static final byte FN_SLLV = 0x04;
	/**  shift word right logical variable */
	public static final byte FN_SRLV = 0x06;
	/** shift word right arithmetic variable */
	public static final byte FN_SRAV = 0x07;
	/** jump register (function call return if rs=31) */
	public static final byte FN_JR = 0x08;
	/** jump and link register */
	public static final byte FN_JALR = 0x09;
	public static final byte FN_MOVZ = 0x0a;
	public static final byte FN_MOVN = 0x0b;
	public static final byte FN_SYSCALL = 0x0c;
	public static final byte FN_BREAK = 0x0d;
	/** move from hi register */
	public static final byte FN_MFHI = 0x10;
	/** move to hi register */
	public static final byte FN_MTHI = 0x11;
	/** move from lo register */
	public static final byte FN_MFLO = 0x12;
	public static final byte FN_MTLO = 0x13;
	/** multiply 32 bit signed integers */
	public static final byte FN_MULT = 0x18;
	/** mul unsigned integers */
	public static final byte FN_MULTU = 0x19;
	/** divide 32 bit signed integers */
	public static final byte FN_DIV = 0x1a;
	/** divide unsigned word */
	public static final byte FN_DIVU = 0x1b;
	/** add with overflow exception */
	public static final byte FN_ADD = 0x20;
	/** add unsigned word */
	public static final byte FN_ADDU = 0x21;
	/** subtract unsigned word */
	public static final byte FN_SUBU = 0x23;
	/** bitwise logical and */
	public static final byte FN_AND = 0x24;
	/** bitwise logical or */
	public static final byte FN_OR = 0x25;
	/** exclusive or */
	public static final byte FN_XOR = 0x26;
	
	/** not or */
	public static final byte FN_NOR = 0x27;
	/** set on less than signed */
	public static final byte FN_SLT = 0x2a;
	/** set on less than unsigned */
	public static final byte FN_SLTU = 0x2b;
	/** trap if not equal */
	public static final byte FN_TNE = 0x36;
	/** multiply word to gpr */
	public static final byte FN2_MUL = 0x02;
	/** branch on less than zero */
	public static final byte RT_BLTZ = 0x00;
	/** branch if greater than or equal to zero */
	public static final byte RT_BGEZ = 0x01;
	/** branch on less than zero and link */
	public static final byte RT_BLTZAL = 0x10;
	/** branch on greater than or equal to zero and link */
	public static final byte RT_BGEZAL = 0x11;
	/** move from coprocessor 0 */
	public static final byte CP_RS_MFC0 = 0x00;
	public static final byte CP_RS_MFH = 0x02;
	/** move to coprocessor 0 */
	public static final byte CP_RS_MTC0 = 0x04;
	/** move to high coprocessor 0 */
	public static final byte CP_RS_MTHC0 = 0x06;
	public static final byte CP_RS_RDPGPR = 0x0a;
	public static final byte CP_RS_WRPGPR = 0x0e;
	/** meta instruction, when rs >= CP_RS_CO, isn is selected by fn */
	public static final byte CP_RS_CO = 0x10;
	
	/** move word from floating point reg to gpr */
	public static final byte FP_RS_MFC1 = 0x00;
	/** move control word from floating point */
	public static final byte FP_RS_CFC1 = 0x02;
	
	/** move word to floating point from gpr */
	public static final byte FP_RS_MTC1 = 0x04;
	/** move control word to floating point */
	public static final byte FP_RS_CTC1 = 0x06;
	/** branch on fp condition */
	public static final byte FP_RS_BC1 = 0x08;
	/**
	 * single precision meta instruction, when rs >= FP_RS_S (and < 0x18), isn is
	 * selected by fn
	 */
	public static final byte FP_RS_S = 0x10;
	/** double precision meta instruction */
	public static final byte FP_RS_D = 0x11;
	/** word precision instruction */
	public static final byte FP_RS_W = 0x14;
	public static final byte CP_FN_TLBR = 0x01;
	public static final byte CP_FN_TLBWI = 0x02;
	
	public static final byte CP_FN_TLBINV = 0x03;
	
	public static final byte CP_FN_TLBINVF = 0x04;
	
	public static final byte CP_FN_TLBWR = 0x06;
	
	public static final byte CP_FN_TLBP = 0x08;
	
	public static final byte FP_FN_ADD = 0x00;
	
	public static final byte FP_FN_SUB = 0x01;
	
	public static final byte FP_FN_MUL = 0x02;
	
	public static final byte FP_FN_DIV = 0x03;
	
	public static final byte FP_FN_ABS_D = 0x05;
	
	public static final byte FP_FN_MOV = 0x06;
	
	public static final byte FP_FN_NEG = 0x07;
	
	/** convert to single */
	public static final byte FP_FN_CVT_S = 0x20;
	
	/** convert to double */
	public static final byte FP_FN_CVT_D = 0x21;
	
	/** convert to word */
	public static final byte FP_FN_CVT_W = 0x24;
	
	/** compare for equal */
	public static final byte FP_FN_C_EQ = 0x32;
	
	/** unordered or less than */
	public static final byte FP_FN_C_ULT = 0x35;
	
	/** compare for less than */
	public static final byte FP_FN_C_LT = 0x3c;
	
	/** less then or equal */
	public static final byte FP_FN_C_LE = 0x3e;
	
	/** coproc round to nearest */
	public static final int FCSR_RM_RN = 0x0;
	
	/** coproc round towards zero */
	public static final int FCSR_RM_RZ = 0x1;
	
	/** coproc round towards plus infinity */
	public static final int FCSR_RM_RP = 0x2;
	
	/** coproc round towards minus infinity */
	public static final int FCSR_RM_RM = 0x3;
	
	public static final int HI_GPR = 32;
	
	public static final int LO_GPR = 33;
	
	public static final int STATUS_CPR = 12;
	
	public static final int STATUS_SEL = 0;
	
	public static final int PRID_CPR = 15;
	
	public static final int PRID_SEL = 0;
	
	/** fp control register index */
	public static final int FIR_FCR = 0;
	
	/** fp control and status register index */
	public static final int FCSR_FCR = 31;
	
	public static int base (final int isn) {
		return rs(isn);
	}
	
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
	public static boolean fpTrue (final int isn) {
		// see BC1F
		return (isn & 0x10000) != 0;
	}
	
	/** same as rd */
	public static int fs (final int isn) {
		return rd(isn);
	}
	
	/** same as rt */
	public static int ft (final int isn) {
		return rt(isn);
	}
	
	public static double getDouble (int[] fpReg, int in) {
		final long mask = 0xffffffffL;
		return Double.longBitsToDouble((fpReg[in] & mask) | ((fpReg[in + 1] & mask) << 32));
	}
	
	public static float getSingle (int[] fpReg, int i) {
		return Float.intBitsToFloat(fpReg[i]);
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
	
	public static int rs (final int isn) {
		return (isn >>> 21) & 0x1f;
	}
	
	public static int rt (final int isn) {
		return (isn >>> 16) & 0x1f;
	}
	
	public static int sa (final int isn) {
		return (isn >>> 6) & 0x1f;
	}
	
	public static int sel (final int isn) {
		return isn & 0x7;
	}
	
	public static void setDouble (final int[] fpReg, final int i, final double d) {
		final long dl = Double.doubleToRawLongBits(d);
		fpReg[i] = (int) dl;
		fpReg[i + 1] = (int) (dl >>> 32);
	}
	
	public static void setSingle (final int[] fpReg, final int i, final float f) {
		fpReg[i] = Float.floatToRawIntBits(f);
	}
	
	/** sign extended immediate */
	public static final int simm (final int isn) {
		return (short) isn;
	}
	
	/** syscall or break number */
	public static final int syscall (final int isn) {
		return (isn >>> 6) & 0xfffff;
	}
	
	private MipsConstants () {
		//
	}
	
}
