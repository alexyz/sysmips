package sys.mips;

import static sys.mips.MipsConstants.*;

/**
 * all instructions
 */
public class IsnSet {
	
	public static final IsnSet SET = new IsnSet();
	
	// assembly types
	private static final String J = "";
	private static final String I = "";
	private static final String R = "";
	private static final String S = "";
	private static final String RTRDSEL = "";
	private static final String RD = "";
	private static final String RDRT = "";
	private static final String RTFS = "";
	private static final String CCOFFSET= "";
	private static final String N = "";
	private static final String CCFSFT = "";
	private static final String FDFSFT = "";
	private static final String RSOFFSET = "";
	
	// interactive disassembly types
	private static final String SF_LOAD = "{rt} <- [{base}+{offset}]: {membaseoffset} <- {baseoffset}";
	private static final String SF_STORE = "[{base}+{offset}] <- {rt}: [{baseoffset}] <- {regrt}";
	private static final String SF_JUMP = "{jump}";
	private static final String SF_CONDBRA = "{rs} ~ {rt}: {regrs} ~ {regrt} => {branch}";
	private static final String SF_OPIMM = "{rt} <- {rs} * {imm}";
	private static final String SF_OP = "{rd} <- {rs} * {rt}";
	private static final String SF_COND = "{rs} ~ {rt}";
	private static final String SF_SHIFT = "{rd} <- {rt} * {sa}";
	private static final String SF_SHIFTREG = "{rd} <- {rt} * {rs}";
	private static final String SF_ZCONDBRA = "{rs} ~ 0: {regrs} => {branch}";
	private static final String SF_CONDZMOV = "{rd} <- {rs} if {regrt} ~ 0";
	private static final String SF_HLOP = "hi:lo <- {rs} * {rt}";
	
	//
	// instance stuff
	//
	
	public final Isn[] op = new Isn[64];
	public final Isn[] special = new Isn[64];
	public final Isn[] special2 = new Isn[64];
	public final Isn[] regimm = new Isn[32];
	public final Isn[] system = new Isn[32];
	public final Isn[] systemFn = new Isn[64];
	public final Isn[] floatingPoint = new Isn[32];
	public final Isn[] floatingPointFn = new Isn[64];
	
	private IsnSet () {
		add(new Isn(OP_J, "j", J, SF_JUMP));
		add(new Isn(OP_JAL, "jal", J, SF_JUMP));
		add(new Isn(OP_BEQ, "beq", I, SF_CONDBRA));
		add(new Isn(OP_BNE, "bne", I, SF_CONDBRA));
		add(new Isn(OP_BLEZ, "blez", I, SF_ZCONDBRA));
		add(new Isn(OP_BGTZ, "bgtz", I, SF_ZCONDBRA));
		add(new Isn(OP_ADDIU, "addiu", I, SF_OPIMM));
		add(new Isn(OP_SLTI, "slti", I, SF_OPIMM));
		add(new Isn(OP_SLTIU, "sltiu", I, SF_OPIMM));
		add(new Isn(OP_ANDI, "andi", I, SF_OPIMM));
		add(new Isn(OP_ORI, "ori", I, SF_OPIMM));
		add(new Isn(OP_XORI, "xori", I, SF_OPIMM));
		add(new Isn(OP_LUI, "lui", I, "{rt} <- {imm}"));
		add(new Isn(OP_LL, "ll", I, SF_LOAD));
		add(new Isn(OP_LB, "lb", I, SF_LOAD));
		add(new Isn(OP_LH, "lh", I, SF_LOAD));
		add(new Isn(OP_LWL, "lwl", I, SF_LOAD));
		add(new Isn(OP_LW, "lw", I, SF_LOAD));
		add(new Isn(OP_LBU, "lbu", I, SF_LOAD));
		add(new Isn(OP_LHU, "lhu", I, SF_LOAD));
		add(new Isn(OP_LWR, "lwr", I, SF_LOAD));
		add(new Isn(OP_SB, "sb", I, SF_STORE));
		add(new Isn(OP_SH, "sh", I, SF_STORE));
		add(new Isn(OP_SWL, "swl", I, SF_STORE));
		add(new Isn(OP_SW, "sw", I, SF_STORE));
		add(new Isn(OP_SC, "sc", I, SF_STORE));
		add(new Isn(OP_SWR, "swr", I, SF_STORE));
		add(new Isn(OP_LWC1, "lwc1", I, ""));
		add(new Isn(OP_SWC1, "swc1", I, ""));
		
		add(new Isn(OP_REGIMM, RT_BLTZ, "bltz", "", SF_ZCONDBRA));
		add(new Isn(OP_REGIMM, RT_BGEZ, "bgez", "", SF_ZCONDBRA));
		add(new Isn(OP_REGIMM, RT_BLTZAL, "bltzal", "", SF_ZCONDBRA));
		add(new Isn(OP_REGIMM, RT_BGEZAL, "bgezal", "", SF_ZCONDBRA));
		
		add(new Isn(OP_SPECIAL, FN_SLL, "sll", R, SF_SHIFT));
		add(new Isn(OP_SPECIAL, FN_SRL, "srl", R, SF_SHIFT));
		add(new Isn(OP_SPECIAL, FN_SRA, "sra", R, SF_SHIFT));
		add(new Isn(OP_SPECIAL, FN_SLLV, "sllv", R, SF_SHIFTREG));
		add(new Isn(OP_SPECIAL, FN_SRLV, "srlv", R, SF_SHIFTREG));
		add(new Isn(OP_SPECIAL, FN_SRAV, "srav", R, SF_SHIFTREG));
		add(new Isn(OP_SPECIAL, FN_JR, "jr", R, "{rs} -> {regrs}"));
		add(new Isn(OP_SPECIAL, FN_JALR, "jalr", R, "{rd} <- link, {rs} => {regrs}"));
		add(new Isn(OP_SPECIAL, FN_MOVZ, "movz", R, SF_CONDZMOV));
		add(new Isn(OP_SPECIAL, FN_MOVN, "movn", R, SF_CONDZMOV));
		add(new Isn(OP_SPECIAL, FN_SYSCALL, "syscall", S, "{syscall}"));
		add(new Isn(OP_SPECIAL, FN_BREAK, "break", S, "{syscall}"));
		add(new Isn(OP_SPECIAL, FN_MFHI, "mfhi", R, "{rd} <- hi : {hi}"));
		add(new Isn(OP_SPECIAL, FN_MFLO, "mflo", R, "{rd} <- lo : {lo}"));
		add(new Isn(OP_SPECIAL, FN_MTHI, "mthi", R, "hi <- {rs} : {regrs}"));
		add(new Isn(OP_SPECIAL, FN_MTLO, "mtlo", R, "lo <- {rs} : {regrs}"));
		add(new Isn(OP_SPECIAL, FN_MULT, "mult", R, SF_HLOP));
		add(new Isn(OP_SPECIAL, FN_MULTU, "multu", R, SF_HLOP));
		add(new Isn(OP_SPECIAL, FN_DIV, "div", R, SF_HLOP));
		add(new Isn(OP_SPECIAL, FN_DIVU, "divu", R, SF_HLOP));
		add(new Isn(OP_SPECIAL, FN_ADDU, "addu", R, SF_OP));
		add(new Isn(OP_SPECIAL, FN_SUBU, "subu", R, SF_OP));
		add(new Isn(OP_SPECIAL, FN_AND, "and", R, SF_OP));
		add(new Isn(OP_SPECIAL, FN_OR, "or", R, SF_OP));
		add(new Isn(OP_SPECIAL, FN_XOR, "xor", R, SF_OP));
		add(new Isn(OP_SPECIAL, FN_NOR, "nor", R, SF_OP));
		add(new Isn(OP_SPECIAL, FN_SLT, "slt", R, SF_OP));
		add(new Isn(OP_SPECIAL, FN_SLTU, "sltu", R, SF_OP));
		add(new Isn(OP_SPECIAL, FN_TNE, "tne", R, SF_COND));
		
		add(new Isn(OP_SPECIAL2, FN2_MUL, "mul", R, SF_OP));
		
		add(new Isn(OP_COP0, CP_RS_MFC0, "mfc0", RTRDSEL, "{rt} <- {cprd}"));
		add(new Isn(OP_COP0, CP_RS_MFH, "mfh", RD, ""));
		add(new Isn(OP_COP0, CP_RS_MTC0, "mtc0", RTRDSEL, "{cprd} <- {rt}"));
		add(new Isn(OP_COP0, CP_RS_MTHC0, "mthc0", RTRDSEL, ""));
		add(new Isn(OP_COP0, CP_RS_RDPGPR, "rdpgpr", RDRT, ""));
		add(new Isn(OP_COP0, CP_RS_WRPGPR, "wrpgpr", RDRT, ""));
		
		add(new Isn(OP_COP0, CP_RS_CO, CP_FN_TLBINV, "tlbinv", "", N));
		add(new Isn(OP_COP0, CP_RS_CO, CP_FN_TLBINVF, "tlbinvf", "", N));
		add(new Isn(OP_COP0, CP_RS_CO, CP_FN_TLBP, "tlbp", "", N));
		add(new Isn(OP_COP0, CP_RS_CO, CP_FN_TLBR, "tlbr", "", N));
		add(new Isn(OP_COP0, CP_RS_CO, CP_FN_TLBWI, "tlbwi", "", N));
		add(new Isn(OP_COP0, CP_RS_CO, CP_FN_TLBWR, "tlbiwr", "", N));
		
		add(new Isn(OP_COP1, FP_RS_MFC1, "mfc1", "", RTFS));
		add(new Isn(OP_COP1, FP_RS_CFC1, "cfc1", "", RTFS));
		add(new Isn(OP_COP1, FP_RS_MTC1, "mtc1", "", RTFS));
		add(new Isn(OP_COP1, FP_RS_CTC1, "ctc1", "", RTFS));
		add(new Isn(OP_COP1, FP_RS_BC1, "bc1", "", CCOFFSET));
		
		add(new Isn(OP_COP1, FP_RS_S, FP_FN_ADD_D, "add.fmt", FDFSFT, "add.{fpfmt}"));
		add(new Isn(OP_COP1, FP_RS_S, FP_FN_SUB_D, "sub.fmt", FDFSFT, "sub.{fpfmt}"));
		add(new Isn(OP_COP1, FP_RS_S, FP_FN_MUL_D, "mul.fmt", FDFSFT, "mul.{fpfmt}"));
		add(new Isn(OP_COP1, FP_RS_S, FP_FN_DIV_D, "div.fmt", FDFSFT, "div.{fpfmt}"));
		add(new Isn(OP_COP1, FP_RS_S, FP_FN_MOV_D, "mov.fmt", FDFSFT, "mov.{fpfmt}"));
		add(new Isn(OP_COP1, FP_RS_S, FP_FN_NEG_D, "neg.fmt", FDFSFT, "neg.{fpfmt}"));
		add(new Isn(OP_COP1, FP_RS_S, FP_FN_CVT_S, "cvts.fmt", FDFSFT, "cvts.{fpfmt}"));
		add(new Isn(OP_COP1, FP_RS_S, FP_FN_CVT_D, "cvtd.fmt", FDFSFT, "cvtd.{fpfmt}"));
		add(new Isn(OP_COP1, FP_RS_S, FP_FN_CVT_W, "cvtw.fmt", FDFSFT, "cvtw.{fpfmt}"));
		add(new Isn(OP_COP1, FP_RS_S, FP_FN_C_ULT, "c.ult.fmt", CCFSFT, "ult.{fpc}"));
		add(new Isn(OP_COP1, FP_RS_S, FP_FN_C_EQ,  "c.eq.f", CCFSFT, "eq.{fptf}"));
		add(new Isn(OP_COP1, FP_RS_S, FP_FN_C_LT,  "c.lt.f", CCFSFT, "lt.{fptf}"));
		add(new Isn(OP_COP1, FP_RS_S, FP_FN_C_LE,  "c.le.f", CCFSFT, "le.{fptf}"));
	}
	
	private void add (Isn isn) {
		switch (isn.op) {
			case OP_SPECIAL:
				special[isn.op2] = isn;
				break;
				
			case OP_REGIMM:
				regimm[isn.op2] = isn;
				break;
				
			case OP_COP0:
				System.out.println("adding cp " + isn.op2);
				if (isn.op2 < CP_RS_CO) {
					System.out.println("adding cprs " + isn.op3);
					system[isn.op2] = isn;
				} else {
					systemFn[isn.op3] = isn;
				}
				break;
				
			case OP_COP1:
				if (isn.op2 < FP_RS_S) {
					floatingPoint[isn.op2] = isn;
				} else {
					floatingPointFn[isn.op3] = isn;
				}
				break;
				
			case OP_SPECIAL2:
				special2[isn.op2] = isn;
				break;
				
			default:
				op[isn.op] = isn;
		}
	}
	
}
