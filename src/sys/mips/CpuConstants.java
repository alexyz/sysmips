package sys.mips;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * mips constants
 */
public final class CpuConstants {
	
	//
	// instructions
	// any instructions added here must also be added to IsnSet!
	//
	
	/** special meta instruction selected by FN */
	public static final int OP_SPECIAL = 0x00;
	/** register-immediate meta instruction (selected by RT) */
	public static final int OP_REGIMM = 0x01;
	/** branch within 256mb region */
	public static final int OP_J = 0x02;
	/** jump and link */
	public static final int OP_JAL = 0x03;
	/** branch on equal */
	public static final int OP_BEQ = 0x04;
	/** branch on not equal */
	public static final int OP_BNE = 0x05;
	/** branch on less than or equal to zero */
	public static final int OP_BLEZ = 0x06;
	/** branch on greater than zero */
	public static final int OP_BGTZ = 0x07;
	/** add immediate with overflow exception */
	public static final int OP_ADDI = 0x08;
	/** add immediate unsigned word */
	public static final int OP_ADDIU = 0x09;
	/** set on less than immediate. compare both as signed. */
	public static final int OP_SLTI = 0x0a;
	/** set on less than immediate unsigned */
	public static final int OP_SLTIU = 0x0b;
	/** and immediate (zero extend) */
	public static final int OP_ANDI = 0x0c;
	/** or immediate. zx */
	public static final int OP_ORI = 0x0d;
	/** exclusive or immediate (zx) */
	public static final int OP_XORI = 0x0e;
	/** load upper immediate */
	public static final int OP_LUI = 0x0f;
	/** coprocessor 0 (system) meta instruction selected by RS then by FN */
	public static final int OP_COP0 = 0x10;
	/** coprocessor 1 (fpu) meta instruction selected by RS then by RT/FN */
	public static final int OP_COP1 = 0x11;
	public static final int OP_COP2 = 0x12;
	/** coprocessor 1 (fpu) extension meta instruction (selected by fn) */
	public static final int OP_COP1X = 0x13;
	/** branch if equal likely, execute delay slot only if taken */
	public static final int OP_BEQL = 0x14;
	public static final int OP_BNEL = 0x15;
	public static final int OP_BLEZL = 0x16;
	public static final int OP_BGTZL = 0x17;
	/** meta instruction selected by fn */
	public static final int OP_SPECIAL2 = 0x1c;
	public static final int OP_JALX = 0x1d;
	public static final int OP_SPECIAL3 = 0x1f;
	/** load int (signed) */
	public static final int OP_LB = 0x20;
	/** load halfword. sign extend */
	public static final int OP_LH = 0x21;
	/** load word left */
	public static final int OP_LWL = 0x22;
	/** load word */
	public static final int OP_LW = 0x23;
	/** load unsigned int */
	public static final int OP_LBU = 0x24;
	/** load halfword unsigned */
	public static final int OP_LHU = 0x25;
	/** load word right */
	public static final int OP_LWR = 0x26;
	/** store int */
	public static final int OP_SB = 0x28;
	/** store halfword */
	public static final int OP_SH = 0x29;
	/** store word left */
	public static final int OP_SWL = 0x2a;
	/** store word */
	public static final int OP_SW = 0x2b;
	/** store word right */
	public static final int OP_SWR = 0x2e;
	public static final int OP_CACHE = 0x2f;
	/** load linked word synchronised */
	public static final int OP_LL = 0x30;
	/** load word from mem to coprocessor */
	public static final int OP_LWC1 = 0x31;
	public static final int OP_LWC2 = 0x32;
	/** prefetch */
	public static final int OP_PREF = 0x33;
	/** load double word to floating point */
	public static final int OP_LDC1 = 0x35;
	public static final int OP_LDC2 = 0x36;
	/** store conditional word */
	public static final int OP_SC = 0x38;
	/** store word from coprocessor to memory */
	public static final int OP_SWC1 = 0x39;
	public static final int OP_SWC2 = 0x3a;
	/** store double word from coprocessor to memory */
	public static final int OP_SDC1 = 0x3d;
	public static final int OP_SDC2 = 0x3e;
	
	//
	// SPECIAL instructions
	//
	
	/** shift word left logical. also nop if sa,rd,rt = 0 */
	public static final int FN_SLL = 0x00;
	public static final int FN_MOVCI = 0x01;
	/** shift word right logical */
	public static final int FN_SRL = 0x02;
	/** shift word right arithmetic (preserve sign) */
	public static final int FN_SRA = 0x03;
	/** shift word left logical variable */
	public static final int FN_SLLV = 0x04;
	/**  shift word right logical variable */
	public static final int FN_SRLV = 0x06;
	/** shift word right arithmetic variable */
	public static final int FN_SRAV = 0x07;
	/** jump register (function call return if rs=31) */
	public static final int FN_JR = 0x08;
	/** jump and link register */
	public static final int FN_JALR = 0x09;
	public static final int FN_MOVZ = 0x0a;
	public static final int FN_MOVN = 0x0b;
	public static final int FN_SYSCALL = 0x0c;
	public static final int FN_BREAK = 0x0d;
	public static final int FN_SYNC = 0x0f;
	/** move from hi register */
	public static final int FN_MFHI = 0x10;
	/** move to hi register */
	public static final int FN_MTHI = 0x11;
	/** move from lo register */
	public static final int FN_MFLO = 0x12;
	public static final int FN_MTLO = 0x13;
	/** multiply 32 bit signed integers */
	public static final int FN_MULT = 0x18;
	/** mul unsigned integers */
	public static final int FN_MULTU = 0x19;
	/** divide 32 bit signed integers */
	public static final int FN_DIV = 0x1a;
	/** divide unsigned word */
	public static final int FN_DIVU = 0x1b;
	/** add with overflow exception */
	public static final int FN_ADD = 0x20;
	/** add unsigned word */
	public static final int FN_ADDU = 0x21;
	public static final int FN_SUB = 0x22;
	/** subtract unsigned word */
	public static final int FN_SUBU = 0x23;
	/** bitwise logical and */
	public static final int FN_AND = 0x24;
	/** bitwise logical or */
	public static final int FN_OR = 0x25;
	/** exclusive or */
	public static final int FN_XOR = 0x26;
	/** not or */
	public static final int FN_NOR = 0x27;
	/** set on less than signed */
	public static final int FN_SLT = 0x2a;
	/** set on less than unsigned */
	public static final int FN_SLTU = 0x2b;
	public static final int FN_TGE = 0x30;
	public static final int FN_TGEU = 0x31;
	public static final int FN_TLT = 0x32;
	public static final int FN_TLTU = 0x33;
	public static final int FN_TEQ = 0x34;
	/** trap if not equal */
	public static final int FN_TNE = 0x36;
	
	//
	// REGIMM instructions
	//
	
	/** branch on less than zero */
	public static final int RT_BLTZ = 0x00;
	/** branch if greater than or equal to zero */
	public static final int RT_BGEZ = 0x01;
	public static final int RT_BLTZL = 0x02;
	public static final int RT_BGEZL = 0x03;
	public static final int RT_TGEI = 0x08;
	public static final int RT_TGEIU = 0x09;
	public static final int RT_TLTI = 0x0a;
	public static final int RT_TLTIU = 0x0b;
	public static final int RT_TEQI = 0x0c;
	public static final int RT_TNEI = 0x0e;
	/** branch on less than zero and link */
	public static final int RT_BLTZAL = 0x10;
	/** branch on greater than or equal to zero and link */
	public static final int RT_BGEZAL = 0x11;
	public static final int RT_BLTZALL = 0x12;
	public static final int RT_BGEZALL = 0x13;
	public static final int RT_SYNCI = 0x1f;

	//
	// SPECIAL2 instructions
	//
	
	/** signed multiply and add to hilo */
	public static final int FN2_MADD = 0x00;
	public static final int FN2_MADDU = 0x01;
	/** multiply word to gpr */
	public static final int FN2_MUL = 0x02;
	public static final int FN2_MSUB = 0x04;
	public static final int FN2_MSUBU = 0x05;
	/** count leading zeros in word */
	public static final int FN2_CLZ = 0x20;
	public static final int FN2_CLO = 0x21;
	public static final int FN2_SDBBP = 0x3f;
	
	//
	// System coprocessor instructions
	//
	
	/** move from coprocessor 0 */
	public static final int CP_RS_MFC0 = 0x00;
	/** move to coprocessor 0 */
	public static final int CP_RS_MTC0 = 0x04;
	public static final int CP_RS_RDPGPR = 0x0a;
	public static final int CP_RS_MFMC0 = 0x0b;
	public static final int CP_RS_WRPGPR = 0x0e;
	/** meta instruction, when rs >= CP_RS_CO, isn is selected by fn */
	public static final int CP_RS_CO = 0x10;
	
	//
	// system coprocessor functions
	//

	public static final int CP_FN_TLBR = 0x01;
	/** write indexed tlb entry */
	public static final int CP_FN_TLBWI = 0x02;
	/** TLB invalidate, optional instruction */
	public static final int CP_FN_TLBINV = 0x03;
	public static final int CP_FN_TLBINVF = 0x04;
	/** write random tlb entry */
	public static final int CP_FN_TLBWR = 0x06;
	/** tlb probe */
	public static final int CP_FN_TLBP = 0x08;
	/** exception return */
	public static final int CP_FN_ERET = 0x18;
	public static final int CP_FN_DERET = 0x1f;
	public static final int CP_FN_WAIT = 0x20;
	
	//
	// floating point (coprocessor 1) instructions
	//
	
	/** move word from floating point reg to gpr */
	public static final int FP_RS_MFC1 = 0x00;
	/** move control word from floating point */
	public static final int FP_RS_CFC1 = 0x02;
	public static final int FP_RS_MFHC1 = 0x03;
	/** move word to floating point from gpr */
	public static final int FP_RS_MTC1 = 0x04;
	/** move control word to floating point */
	public static final int FP_RS_CTC1 = 0x06;
	public static final int FP_RS_MTHC1 = 0x07;
	/** branch on fp condition sort-of meta instruction (depends on CC and TF) */
	public static final int FP_RS_BC1 = 0x08;
	public static final int FP_RS_BC1ANY2 = 0x09;
	public static final int FP_RS_BC1ANY4 = 0x0a;
	/**
	 * single precision meta instruction, when rs >= FP_RS_S (and < 0x18), isn is
	 * selected by fn
	 */
	public static final int FP_RS_S = 0x10;
	/** double precision meta instruction */
	public static final int FP_RS_D = 0x11;
	/** word precision meta instruction */
	public static final int FP_RS_W = 0x14;
	/** long precision meta instruction */
	public static final int FP_RS_L = 0x15;
	public static final int FP_RS_PS = 0x16;
	
	//
	// floating point functions
	//
	
	public static final int FP_FN_ADD = 0x00;
	public static final int FP_FN_SUB = 0x01;
	public static final int FP_FN_MUL = 0x02;
	public static final int FP_FN_DIV = 0x03;
	public static final int FP_FN_SQRT = 0x04;
	public static final int FP_FN_ABS = 0x05;
	public static final int FP_FN_MOV = 0x06;
	public static final int FP_FN_NEG = 0x07;
	public static final int FP_FN_ROUND_L = 0x08;
	public static final int FP_FN_TRUNC_L = 0x09;
	public static final int FP_FN_CEIL_L = 0x0a;
	public static final int FP_FN_FLOOR_L = 0x0b;
	public static final int FP_FN_ROUND_W = 0x0c;
	public static final int FP_FN_TRUNC_W = 0x0d;
	public static final int FP_FN_CEIL_W = 0x0e;
	public static final int FP_FN_FLOOR_W = 0x0f;
	public static final int FP_FN_MOVCF = 0x11;
	public static final int FP_FN_MOVZ = 0x12;
	public static final int FP_FN_MOVN = 0x13;
	public static final int FP_FN_RECIP = 0x15;
	public static final int FP_FN_RSQRT = 0x16;
	public static final int FP_FN_RECIP2 = 0x1c;
	public static final int FP_FN_RECIP1 = 0x1d;
	public static final int FP_FN_RSQRT1 = 0x1e;
	public static final int FP_FN_RSQRT2 = 0x1f;
	/** convert to single */
	public static final int FP_FN_CVT_S = 0x20;
	/** convert to double */
	public static final int FP_FN_CVT_D = 0x21;
	/** convert to word */
	public static final int FP_FN_CVT_W = 0x24;
	public static final int FP_FN_CVT_L = 0x25;
	public static final int FP_FN_CVT_PS = 0x26;
	public static final int FP_FN_C_F = 0x30;
	public static final int FP_FN_C_UN = 0x31;
	/** compare for equal */
	public static final int FP_FN_C_EQ = 0x32;
	public static final int FP_FN_C_UEQ = 0x33;
	public static final int FP_FN_C_OLT = 0x34;
	/** unordered or less than */
	public static final int FP_FN_C_ULT = 0x35;
	public static final int FP_FN_C_OLE = 0x36;
	public static final int FP_FN_C_ULE = 0x37;
	public static final int FP_FN_C_SF = 0x38;
	public static final int FP_FN_C_NGLE = 0x39;
	public static final int FP_FN_C_SEQ = 0x3a;
	public static final int FP_FN_C_NGL = 0x3b;
	/** compare for less than */
	public static final int FP_FN_C_LT = 0x3c;
	public static final int FP_FN_C_NGE = 0x3d;
	/** less then or equal */
	public static final int FP_FN_C_LE = 0x3e;
	public static final int FP_FN_C_NGT = 0x3f;
	
	//
	// floating point extension instructions
	//
	
	/** multiply add */
	public static final int FP_FNX_MADDS = 0x20;
	
	//
	// general registers
	//

	public static final int REG_ZERO = 0;
	public static final int REG_AT = 1;
	public static final int REG_V0 = 2;
	public static final int REG_V1 = 3;
	/** first argument register */
	public static final int REG_A0 = 4;
	public static final int REG_A1 = 5;
	public static final int REG_A2 = 6;
	public static final int REG_A3 = 7;
	/** kernel temp 0 */
	public static final int REG_K0 = 26;
	/** kernel temp 1 */
	public static final int REG_K1 = 27;
	public static final int REG_GP = 28;
	/** stack pointer register */
	public static final int REG_SP = 29;
	public static final int REG_RA = 31;
	
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
	
	public static final CpRegConstant CPR_INDEX_INDEX = new CpRegConstant(CPR_INDEX, 0, 4);
	public static final CpRegConstant CPR_INDEX_PROBEFAIL = new CpRegConstant(CPR_INDEX, 31, 1);
	
	public static final CpRegConstant CPR_RANDOM_RANDOM = new CpRegConstant(CPR_RANDOM, 0, 4);
	
	public static final CpRegConstant CPR_ENTRYLO0_GLOBAL = new CpRegConstant(CPR_ENTRYLO0, 0, 1);
	public static final CpRegConstant CPR_ENTRYLO0_VALID = new CpRegConstant(CPR_ENTRYLO0, 1, 1);
	public static final CpRegConstant CPR_ENTRYLO0_DIRTY = new CpRegConstant(CPR_ENTRYLO0, 2, 1);
	public static final CpRegConstant CPR_ENTRYLO0_COHERENCY = new CpRegConstant(CPR_ENTRYLO0, 3, 3);
	public static final CpRegConstant CPR_ENTRYLO0_PFN = new CpRegConstant(CPR_ENTRYLO0, 6, 20);
	
	public static final CpRegConstant CPR_ENTRYLO1_GLOBAL = new CpRegConstant(CPR_ENTRYLO1, 0, 1);
	public static final CpRegConstant CPR_ENTRYLO1_VALID = new CpRegConstant(CPR_ENTRYLO1, 1, 1);
	public static final CpRegConstant CPR_ENTRYLO1_DIRTY = new CpRegConstant(CPR_ENTRYLO1, 2, 1);
	public static final CpRegConstant CPR_ENTRYLO1_COHERENCY = new CpRegConstant(CPR_ENTRYLO1, 3, 3);
	public static final CpRegConstant CPR_ENTRYLO1_PFN = new CpRegConstant(CPR_ENTRYLO1, 6, 20);

	/** context register bad virtual page number (19 bits) */
	public static final CpRegConstant CPR_CONTEXT_BADVPN2 = new CpRegConstant(CPR_CONTEXT, 4, 19);
	public static final CpRegConstant CPR_CONTEXT_PTEBASE = new CpRegConstant(CPR_CONTEXT, 23, 9);
	
	public static final CpRegConstant CPR_PAGEMASK_MASK = new CpRegConstant(CPR_PAGEMASK, 13, 12);
	
	public static final CpRegConstant CPR_WIRED_WIRED = new CpRegConstant(CPR_WIRED, 0, 4);
	
	public static final CpRegConstant CPR_BADVADDR_BADVADDR = new CpRegConstant(CPR_BADVADDR, 0, 32);

	public static final CpRegConstant CPR_ENTRYHI_ASID = new CpRegConstant(CPR_ENTRYHI, 0, 8);
	public static final CpRegConstant CPR_ENTRYHI_VPN2 = new CpRegConstant(CPR_ENTRYHI, 13, 19);
	
	/** status register interrupt enable (1 bit) */
	public static final CpRegConstant CPR_STATUS_IE = new CpRegConstant(CPR_STATUS, 0, 1);
	/** status register exception level (1 bit) */
	public static final CpRegConstant CPR_STATUS_EXL = new CpRegConstant(CPR_STATUS, 1, 1);
	/** status register reset error level (1 bit) */
	public static final CpRegConstant CPR_STATUS_ERL = new CpRegConstant(CPR_STATUS, 2, 1);
	/** status register user mode (1 bit) */
	public static final CpRegConstant CPR_STATUS_UM = new CpRegConstant(CPR_STATUS, 4, 1);
	/** status register interrupt mask (8 bits) */
	public static final CpRegConstant CPR_STATUS_IM = new CpRegConstant(CPR_STATUS, 8, 8);
	/** status register bootstrap exception vectors (1 bit) */
	public static final CpRegConstant CPR_STATUS_BEV = new CpRegConstant(CPR_STATUS, 22, 1);
	/** status register access to system coprocessor (1 bit) */
	public static final CpRegConstant CPR_STATUS_CU0 = new CpRegConstant(CPR_STATUS, 28, 1);
	/** status register access to floating point coprocessor (1 bit) */
	public static final CpRegConstant CPR_STATUS_CU1 = new CpRegConstant(CPR_STATUS, 29, 1);
	
	/** cause register exception code */
	public static final CpRegConstant CPR_CAUSE_EXCODE = new CpRegConstant(CPR_CAUSE, 2, 5);
	/** cause register interrupt pending mask */
	public static final CpRegConstant CPR_CAUSE_IP = new CpRegConstant(CPR_CAUSE, 8, 8);
	/** cause register use general or special exception vector (1 bit) */
	public static final CpRegConstant CPR_CAUSE_IV = new CpRegConstant(CPR_CAUSE, 23, 1);
	/** cause branch delay slot (1 bit) */
	public static final CpRegConstant CPR_CAUSE_BD = new CpRegConstant(CPR_CAUSE, 31, 1);
	
	public static final CpRegConstant CPR_EPC_VALUE = new CpRegConstant(CPR_EPC, 0, 32);
	
	public static final CpRegConstant CPR_PRID_REV = new CpRegConstant(CPR_PRID, 0, 8);
	public static final CpRegConstant CPR_PRID_PROCID = new CpRegConstant(CPR_PRID, 8, 8);
	public static final CpRegConstant CPR_PRID_COMPANYID = new CpRegConstant(CPR_PRID, 16, 8);
	
	
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
	
	//
	// functions
	//
	
	/** return the cpr index for the register and selection */
	public static int cprIndex (int rd, int sel) {
		return rd + sel * 32;
	}

	private CpuConstants () {
		//
	}
	
}
