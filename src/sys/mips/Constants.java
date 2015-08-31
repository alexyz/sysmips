package sys.mips;

/**
 * mips constants
 */
public final class Constants {
	
	//
	// instructions
	// any instructions added here must also be added to IsnSet!
	//
	
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
	/** load upper immediate */
	public static final byte OP_LUI = 0x0f;
	/** coprocessor 0 (system) meta instruction selected by RS then by FN */
	public static final byte OP_COP0 = 0x10;
	/** coprocessor 1 (fpu) meta instruction selected by RS then by RT/FN */
	public static final byte OP_COP1 = 0x11;
	public static final byte OP_COP2 = 0x12;
	/** coprocessor 1 (fpu) extension meta instruction (selected by fn) */
	public static final byte OP_COP1X = 0x13;
	/** branch if equal likely, execute delay slot only if taken */
	public static final byte OP_BEQL = 0x14;
	public static final byte OP_BNEL = 0x15;
	public static final byte OP_BLEZL = 0x16;
	public static final byte OP_BGTZL = 0x17;
	/** meta instruction selected by fn */
	public static final byte OP_SPECIAL2 = 0x1c;
	public static final byte OP_JALX = 0x1d;
	public static final byte OP_SPECIAL3 = 0x1f;
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
	/** load word right */
	public static final byte OP_LWR = 0x26;
	/** store byte */
	public static final byte OP_SB = 0x28;
	/** store halfword */
	public static final byte OP_SH = 0x29;
	/** store word left */
	public static final byte OP_SWL = 0x2a;
	/** store word */
	public static final byte OP_SW = 0x2b;
	/** store word right */
	public static final byte OP_SWR = 0x2e;
	public static final byte OP_CACHE = 0x2f;
	/** load linked word synchronised */
	public static final byte OP_LL = 0x30;
	/** load word from mem to coprocessor */
	public static final byte OP_LWC1 = 0x31;
	public static final byte OP_LWC2 = 0x32;
	/** prefetch */
	public static final byte OP_PREF = 0x33;
	/** load double word to floating point */
	public static final byte OP_LDC1 = 0x35;
	public static final byte OP_LDC2 = 0x36;
	/** store conditional word */
	public static final byte OP_SC = 0x38;
	/** store word from coprocessor to memory */
	public static final byte OP_SWC1 = 0x39;
	public static final byte OP_SWC2 = 0x3a;
	/** store double word from coprocessor to memory */
	public static final byte OP_SDC1 = 0x3d;
	public static final byte OP_SDC2 = 0x3e;
	
	//
	// SPECIAL instructions
	//
	
	/** shift word left logical. also nop if sa,rd,rt = 0 */
	public static final byte FN_SLL = 0x00;
	public static final byte FN_MOVCI = 0x01;
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
	public static final byte FN_SYNC = 0x0f;
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
	public static final byte FN_SUB = 0x22;
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
	public static final byte FN_TGE = 0x30;
	public static final byte FN_TGEU = 0x31;
	public static final byte FN_TLT = 0x32;
	public static final byte FN_TLTU = 0x33;
	public static final byte FN_TEQ = 0x34;
	/** trap if not equal */
	public static final byte FN_TNE = 0x36;
	
	//
	// REGIMM instructions
	//
	
	/** branch on less than zero */
	public static final byte RT_BLTZ = 0x00;
	/** branch if greater than or equal to zero */
	public static final byte RT_BGEZ = 0x01;
	public static final byte RT_BLTZL = 0x02;
	public static final byte RT_BGEZL = 0x03;
	public static final byte RT_TGEI = 0x08;
	public static final byte RT_TGEIU = 0x09;
	public static final byte RT_TLTI = 0x0a;
	public static final byte RT_TLTIU = 0x0b;
	public static final byte RT_TEQI = 0x0c;
	public static final byte RT_TNEI = 0x0e;
	/** branch on less than zero and link */
	public static final byte RT_BLTZAL = 0x10;
	/** branch on greater than or equal to zero and link */
	public static final byte RT_BGEZAL = 0x11;
	public static final byte RT_BLTZALL = 0x12;
	public static final byte RT_BGEZALL = 0x13;
	public static final byte RT_SYNCI = 0x1f;

	//
	// SPECIAL2 instructions
	//
	
	public static final byte FN2_MADD = 0x00;
	public static final byte FN2_MADDU = 0x01;
	/** multiply word to gpr */
	public static final byte FN2_MUL = 0x02;
	public static final byte FN2_MSUB = 0x04;
	public static final byte FN2_MSUBU = 0x05;
	/** count leading zeros in word */
	public static final byte FN2_CLZ = 0x20;
	public static final byte FN2_MLO = 0x21;
	public static final byte FN2_SDBBP = 0x3f;
	
	//
	// System coprocessor instructions
	//
	
	/** move from coprocessor 0 */
	public static final byte CP_RS_MFC0 = 0x00;
	/** move to coprocessor 0 */
	public static final byte CP_RS_MTC0 = 0x04;
	public static final byte CP_RS_RDPGPR = 0x0a;
	public static final byte CP_RS_MFMC0 = 0x0b;
	public static final byte CP_RS_WRPGPR = 0x0e;
	/** meta instruction, when rs >= CP_RS_CO, isn is selected by fn */
	public static final byte CP_RS_CO = 0x10;
	
	//
	// system coprocessor functions
	//

	public static final byte CP_FN_TLBR = 0x01;
	public static final byte CP_FN_TLBWI = 0x02;
	/** TLB invalidate, optional instruction */
	public static final byte CP_FN_TLBINV = 0x03;
	public static final byte CP_FN_TLBINVF = 0x04;
	public static final byte CP_FN_TLBWR = 0x06;
	public static final byte CP_FN_TLBP = 0x08;
	public static final byte CP_FN_ERET = 0x18;
	public static final byte CP_FN_DERET = 0x1f;
	public static final byte CP_FN_WAIT = 0x20;
	
	//
	// floating point (coprocessor 1) instructions
	//
	
	/** move word from floating point reg to gpr */
	public static final byte FP_RS_MFC1 = 0x00;
	/** move control word from floating point */
	public static final byte FP_RS_CFC1 = 0x02;
	public static final byte FP_RS_MFHC1 = 0x03;
	/** move word to floating point from gpr */
	public static final byte FP_RS_MTC1 = 0x04;
	/** move control word to floating point */
	public static final byte FP_RS_CTC1 = 0x06;
	public static final byte FP_RS_MTHC1 = 0x07;
	/** branch on fp condition sort-of meta instruction (depends on CC and TF) */
	public static final byte FP_RS_BC1 = 0x08;
	public static final byte FP_RS_BC1ANY2 = 0x09;
	public static final byte FP_RS_BC1ANY4 = 0x0a;
	/**
	 * single precision meta instruction, when rs >= FP_RS_S (and < 0x18), isn is
	 * selected by fn
	 */
	public static final byte FP_RS_S = 0x10;
	/** double precision meta instruction */
	public static final byte FP_RS_D = 0x11;
	/** word precision meta instruction */
	public static final byte FP_RS_W = 0x14;
	/** long precision meta instruction */
	public static final byte FP_RS_L = 0x15;
	public static final byte FP_RS_PS = 0x16;
	
	//
	// floating point functions
	//
	
	public static final byte FP_FN_ADD = 0x00;
	public static final byte FP_FN_SUB = 0x01;
	public static final byte FP_FN_MUL = 0x02;
	public static final byte FP_FN_DIV = 0x03;
	public static final byte FP_FN_SQRT = 0x04;
	public static final byte FP_FN_ABS = 0x05;
	public static final byte FP_FN_MOV = 0x06;
	public static final byte FP_FN_NEG = 0x07;
	public static final byte FP_FN_ROUND_L = 0x08;
	public static final byte FP_FN_TRUNC_L = 0x09;
	public static final byte FP_FN_CEIL_L = 0x0a;
	public static final byte FP_FN_FLOOR_L = 0x0b;
	public static final byte FP_FN_ROUND_W = 0x0c;
	public static final byte FP_FN_TRUNC_W = 0x0d;
	public static final byte FP_FN_CEIL_W = 0x0e;
	public static final byte FP_FN_FLOOR_W = 0x0f;
	public static final byte FP_FN_MOVCF = 0x11;
	public static final byte FP_FN_MOVZ = 0x12;
	public static final byte FP_FN_MOVN = 0x13;
	public static final byte FP_FN_RECIP = 0x15;
	public static final byte FP_FN_RSQRT = 0x16;
	public static final byte FP_FN_RECIP2 = 0x1c;
	public static final byte FP_FN_RECIP1 = 0x1d;
	public static final byte FP_FN_RSQRT1 = 0x1e;
	public static final byte FP_FN_RSQRT2 = 0x1f;
	/** convert to single */
	public static final byte FP_FN_CVT_S = 0x20;
	/** convert to double */
	public static final byte FP_FN_CVT_D = 0x21;
	/** convert to word */
	public static final byte FP_FN_CVT_W = 0x24;
	public static final byte FP_FN_CVT_L = 0x25;
	public static final byte FP_FN_CVT_PS = 0x26;
	public static final byte FP_FN_C_F = 0x30;
	public static final byte FP_FN_C_UN = 0x31;
	/** compare for equal */
	public static final byte FP_FN_C_EQ = 0x32;
	public static final byte FP_FN_C_UEQ = 0x33;
	public static final byte FP_FN_C_OLT = 0x34;
	/** unordered or less than */
	public static final byte FP_FN_C_ULT = 0x35;
	public static final byte FP_FN_C_OLE = 0x36;
	public static final byte FP_FN_C_ULE = 0x37;
	public static final byte FP_FN_C_SF = 0x38;
	public static final byte FP_FN_C_NGLE = 0x39;
	public static final byte FP_FN_C_SEQ = 0x3a;
	public static final byte FP_FN_C_NGL = 0x3b;
	/** compare for less than */
	public static final byte FP_FN_C_LT = 0x3c;
	public static final byte FP_FN_C_NGE = 0x3d;
	/** less then or equal */
	public static final byte FP_FN_C_LE = 0x3e;
	public static final byte FP_FN_C_NGT = 0x3f;
	
	//
	// floating point extension instructions
	//
	
	/** multiply add */
	public static final byte FP_FNX_MADDS = 0x20;
	
	//
	// general registers
	//

	/** first argument register */
	public static final int REG_ZERO = 0;
	public static final int REG_AT = 1;
	public static final int REG_V0 = 2;
	public static final int REG_V1 = 3;
	public static final int REG_A0 = 4;
	public static final int REG_A1 = 5;
	public static final int REG_A2 = 6;
	public static final int REG_A3 = 7;
	/** stack pointer register */
	public static final int REG_SP = 29;
	
	//
	// floating point control registers and constants
	//

	/** floating point control implementation register */
	public static final int FPCR_FIR = 0;
	/** floating point condition codes register */
	public static final int FPCR_FCCR = 25;
	/** fp control and status register */
	public static final int FPCR_FCSR = 31;
	
	/** coproc round to nearest */
	public static final int FCSR_RM_RN = 0x0;
	/** coproc round towards zero */
	public static final int FCSR_RM_RZ = 0x1;
	/** coproc round towards plus infinity */
	public static final int FCSR_RM_RP = 0x2;
	/** coproc round towards minus infinity */
	public static final int FCSR_RM_RM = 0x3;
	
	//
	// system coprocessor control registers (as register+selection*32)
	//
	
	/** system coprocessor TLB access index register */
	public static final int CPR_INDEX = 0;
	/** system coprocessor TLB write random register (changes every clock cycle) */
	public static final int CPR_RANDOM = 1;
	/** system coprocessor TLB entry even page register */
	public static final int CPR_ENTRYLO0 = 2;
	/** system coprocessor TLB entry odd page register */
	public static final int CPR_ENTRYLO1 = 3;
	/** system coprocessor context register (pointer to OS page table entry array) */
	public static final int CPR_CONTEXT = 4;
	/** system coprocessor page mask register (page size for TLB entry) */
	public static final int CPR_PAGEMASK = 5;
	/** system coprocessor wired register (boundary between wired and random TLB entries) */
	public static final int CPR_WIRED = 6;
	/** system coprocessor bad virtual address register */
	public static final int CPR_BADVADDR = 8;
	/** system coprocessor count register (increments every other clock cycle) */
	public static final int CPR_COUNT = 9;
	/** system coprocessor entry hi register (TLB virtual address match) */
	public static final int CPR_ENTRYHI = 10;
	/** system coprocessor compare register (timer interrupt) */
	public static final int CPR_COMPARE = 11;
	/** system coprocessor status register */
	public static final int CPR_STATUS = 12;
	/** system coprocessor cause of most recent exception register */
	public static final int CPR_CAUSE = 13;
	/** system coprocessor exception program counter register */
	public static final int CPR_EPC = 14;
	/** system coprocessor processor id register */
	public static final int CPR_PRID = 15;
	/** system coprocessor config register */
	public static final int CPR_CONFIG = 16;
	/** system coprocessor config1 register (16,1) */
	public static final int CPR_CONFIG1 = 16 + (1*32);
	/** system coprocessor load linked physical address register */
	public static final int CPR_LLADDR = 17;
	/** system coprocessor watchpoint debug lo register */
	public static final int CPR_WATCHLO = 18;
	/** system coprocessor watchpoint debug hi register */
	public static final int CPR_WATCHHI = 19;
	/** system coprocessor debug exception register */
	public static final int CPR_DEBUG = 23;
	/** system coprocessor debug exception program counter register */
	public static final int CPR_DEPC = 24;
	/** system coprocessor i/d cache testing register */
	public static final int CPR_ERRCTL = 26;
	/** system coprocessor cache tag array register */
	public static final int CPR_TAGLO = 28;
	/** system coprocessor cache data array register (28,1) */
	public static final int CPR_DATALO = 28 + (1*32);
	/** system coprocessor error exception program counter register */
	public static final int CPR_ERROREPC = 30;
	/** system coprocessor debug save register */
	public static final int CPR_DESAVE = 31;
	
	//
	// system coprocessor register bitmasks
	//
	
	/** interrupt enable */
	public static final int CPR_STATUS_IE = 1 << 0;
	/** exception level */
	public static final int CPR_STATUS_EXL = 1 << 1;
	/** reset error level */
	public static final int CPR_STATUS_ERL = 1 << 2;
	/** user mode */
	public static final int CPR_STATUS_UM = 1 << 4;
	/** status register interrupt mask shift left (8 bits max) */
	public static final int CPR_STATUS_IM_SHL = 8;
	/** status register interrupt mask (8 bits) */
	public static final int CPR_STATUS_IM = 0xff << CPR_STATUS_IM_SHL;
	/** status register bootstrap exception vectors */
	public static final int CPR_STATUS_BEV = 1 << 22;
	/** status register access to system coprocessor */
	public static final int CPR_STATUS_CU0 = 1 << 28;
	/** status register access to floating point coprocessor */
	public static final int CPR_STATUS_CU1 = 1 << 29;
	
	/** cause register exception code shift left (5 bits max) */
	public static final int CPR_CAUSE_EXCODE_SHL = 2;
	/** cause register exception code */
	public static final int CPR_CAUSE_EXCODE = 0x1f << CPR_CAUSE_EXCODE_SHL;
	/** cause register interrupt pending shift left (8 bits max) */
	public static final int CPR_CAUSE_IP_SHL = 8;
	/** cause register interrupt pending mask */
	public static final int CPR_CAUSE_IP = 0xff << CPR_CAUSE_IP_SHL;
	/** cause general or special exception vector */
	public static final int CPR_CAUSE_IV = (1 << 23);
	/** cause branch delay slot */
	public static final int CPR_CAUSE_BD = (1 << 31);
	
	//
	// exception vectors
	//
	
	/** reset address is in boot rom... */
	public static final int EXV_RESET = 0xbfc0_0000;
	public static final int EXV_TLBREFILL = 0x8000_0000;
	public static final int EXV_EXCEPTION = 0x8000_0180;
	public static final int EXV_INTERRUPT = 0x8000_0200;
	
	//
	// exception types (NOT interrupt types!)
	//
	
	public static final int EX_INTERRUPT = 0;
	public static final int EX_TLB_MODIFICATION = 1;
	public static final int EX_TLB_LOAD = 2;
	public static final int EX_TLB_STORE = 3;
	public static final int EX_ADDR_ERROR_LOAD = 4;
	public static final int EX_ADDR_ERROR_STORE = 5;
	public static final int EX_ISN_BUS_ERROR = 6;
	public static final int EX_DATA_BUS_ERROR = 7;
	public static final int EX_SYSCALL = 8;
	public static final int EX_BREAKPOINT = 9;
	public static final int EX_RESERVED_ISN = 10;
	public static final int EX_COPROC_UNUSABLE = 11;
	public static final int EX_OVERFLOW = 12;
	public static final int EX_TRAP = 13;
	public static final int EX_WATCH = 23;
	public static final int EX_MCHECK = 24;
	
	/** return the cpr index for the register and selection */
	public static int cpr (int rd, int sel) {
		return rd + sel * 32;
	}
	
	private Constants () {
		//
	}
	
}
