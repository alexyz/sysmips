package sys.mips;

import static sys.mips.MipsConstants.*;
import static sys.mips.Isn.*;

/**
 * all instructions
 */
public class IsnSet {
	
	public static final IsnSet SET = new IsnSet();
	
	// assembly types
	private static final String J = "";
	private static final String I = "";
	/** rd, rs, rt */
	public static final String F_R = "";
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
	
	private static void set (Isn[] isns, int i, Isn isn) {
		if (isns[i] != null) {
			throw new RuntimeException(isn.toString());
		}
		isns[i] = isn;
	}
	
	//
	// instance stuff
	//
	
	public final Isn[] op = new Isn[64];
	public final Isn[] fn = new Isn[64];
	public final Isn[] fn2 = new Isn[64];
	public final Isn[] regimm = new Isn[32];
	public final Isn[] system = new Isn[32];
	public final Isn[] systemFn = new Isn[64];
	public final Isn[] fpu = new Isn[32];
	public final Isn[] fpuFn = new Isn[64];
	
	private IsnSet () {
		add(newOp(OP_J, "j", J, SF_JUMP));
		add(newOp(OP_JAL, "jal", J, SF_JUMP));
		add(newOp(OP_BEQ, "beq", I, SF_CONDBRA));
		add(newOp(OP_BNE, "bne", I, SF_CONDBRA));
		add(newOp(OP_BLEZ, "blez", I, SF_ZCONDBRA));
		add(newOp(OP_BGTZ, "bgtz", I, SF_ZCONDBRA));
		add(newOp(OP_ADDIU, "addiu", I, SF_OPIMM));
		add(newOp(OP_SLTI, "slti", I, SF_OPIMM));
		add(newOp(OP_SLTIU, "sltiu", I, SF_OPIMM));
		add(newOp(OP_ANDI, "andi", I, SF_OPIMM));
		add(newOp(OP_ORI, "ori", I, SF_OPIMM));
		add(newOp(OP_XORI, "xori", I, SF_OPIMM));
		add(newOp(OP_LUI, "lui", I, "{rt} <- {imm}"));
		add(newOp(OP_LL, "ll", I, SF_LOAD));
		add(newOp(OP_LB, "lb", I, SF_LOAD));
		add(newOp(OP_LH, "lh", I, SF_LOAD));
		add(newOp(OP_LWL, "lwl", I, SF_LOAD));
		add(newOp(OP_LW, "lw", I, SF_LOAD));
		add(newOp(OP_LBU, "lbu", I, SF_LOAD));
		add(newOp(OP_LHU, "lhu", I, SF_LOAD));
		add(newOp(OP_LWR, "lwr", I, SF_LOAD));
		add(newOp(OP_SB, "sb", I, SF_STORE));
		add(newOp(OP_SH, "sh", I, SF_STORE));
		add(newOp(OP_SWL, "swl", I, SF_STORE));
		add(newOp(OP_SW, "sw", I, SF_STORE));
		add(newOp(OP_SC, "sc", I, SF_STORE));
		add(newOp(OP_SWR, "swr", I, SF_STORE));
		add(newOp(OP_LWC1, "lwc1", I, ""));
		add(newOp(OP_SWC1, "swc1", I, ""));
		
		add(newRegimm(RT_BLTZ, "bltz", "", SF_ZCONDBRA));
		add(newRegimm(RT_BGEZ, "bgez", "", SF_ZCONDBRA));
		add(newRegimm(RT_BLTZAL, "bltzal", "", SF_ZCONDBRA));
		add(newRegimm(RT_BGEZAL, "bgezal", "", SF_ZCONDBRA));
		
		add(newFn(FN_SLL, "sll", F_R, SF_SHIFT));
		add(newFn(FN_SRL, "srl", F_R, SF_SHIFT));
		add(newFn(FN_SRA, "sra", F_R, SF_SHIFT));
		add(newFn(FN_SLLV, "sllv", F_R, SF_SHIFTREG));
		add(newFn(FN_SRLV, "srlv", F_R, SF_SHIFTREG));
		add(newFn(FN_SRAV, "srav", F_R, SF_SHIFTREG));
		add(newFn(FN_JR, "jr", F_R, "{rs} -> {regrs}"));
		add(newFn(FN_JALR, "jalr", F_R, "{rd} <- link, {rs} => {regrs}"));
		add(newFn(FN_MOVZ, "movz", F_R, SF_CONDZMOV));
		add(newFn(FN_MOVN, "movn", F_R, SF_CONDZMOV));
		add(newFn(FN_SYSCALL, "syscall", S, "{syscall}"));
		add(newFn(FN_BREAK, "break", S, "{syscall}"));
		add(newFn(FN_MFHI, "mfhi", F_R, "{rd} <- hi : {hi}"));
		add(newFn(FN_MFLO, "mflo", F_R, "{rd} <- lo : {lo}"));
		add(newFn(FN_MTHI, "mthi", F_R, "hi <- {rs} : {regrs}"));
		add(newFn(FN_MTLO, "mtlo", F_R, "lo <- {rs} : {regrs}"));
		add(newFn(FN_MULT, "mult", F_R, SF_HLOP));
		add(newFn(FN_MULTU, "multu", F_R, SF_HLOP));
		add(newFn(FN_DIV, "div", F_R, SF_HLOP));
		add(newFn(FN_DIVU, "divu", F_R, SF_HLOP));
		add(newFn(FN_ADD, "add", F_R, SF_OP));
		add(newFn(FN_ADDU, "addu", F_R, SF_OP));
		add(newFn(FN_SUBU, "subu", F_R, SF_OP));
		add(newFn(FN_AND, "and", F_R, SF_OP));
		add(newFn(FN_OR, "or", F_R, SF_OP));
		add(newFn(FN_XOR, "xor", F_R, SF_OP));
		add(newFn(FN_NOR, "nor", F_R, SF_OP));
		add(newFn(FN_SLT, "slt", F_R, SF_OP));
		add(newFn(FN_SLTU, "sltu", F_R, SF_OP));
		add(newFn(FN_TNE, "tne", F_R, SF_COND));
		
		add(newFn2(FN2_MUL, "mul", F_R, SF_OP));
		
		add(newCop0(CP_RS_MFC0, "mfc0", RTRDSEL, "{rt} <- {cprd}"));
		add(newCop0(CP_RS_MFH, "mfh", RD, ""));
		add(newCop0(CP_RS_MTC0, "mtc0", RTRDSEL, "{cprd} <- {rt}"));
		add(newCop0(CP_RS_MTHC0, "mthc0", RTRDSEL, ""));
		add(newCop0(CP_RS_RDPGPR, "rdpgpr", RDRT, ""));
		add(newCop0(CP_RS_WRPGPR, "wrpgpr", RDRT, ""));
		
		add(newCop0Fn(CP_FN_TLBINV, "tlbinv", "", N));
		add(newCop0Fn(CP_FN_TLBINVF, "tlbinvf", "", N));
		add(newCop0Fn(CP_FN_TLBP, "tlbp", "", N));
		add(newCop0Fn(CP_FN_TLBR, "tlbr", "", N));
		add(newCop0Fn(CP_FN_TLBWI, "tlbwi", "", N));
		add(newCop0Fn(CP_FN_TLBWR, "tlbiwr", "", N));
		
		add(newCop1(FP_RS_MFC1, "mfc1", "", RTFS));
		add(newCop1(FP_RS_CFC1, "cfc1", "", RTFS));
		add(newCop1(FP_RS_MTC1, "mtc1", "", RTFS));
		add(newCop1(FP_RS_CTC1, "ctc1", "", RTFS));
		add(newCop1(FP_RS_BC1, "bc1", "", CCOFFSET));
		
		add(newCop1Fn(FP_FN_ADD, "add.fmt", FDFSFT, "add.{fpfmt}"));
		add(newCop1Fn(FP_FN_SUB, "sub.fmt", FDFSFT, "sub.{fpfmt}"));
		add(newCop1Fn(FP_FN_MUL, "mul.fmt", FDFSFT, "mul.{fpfmt}"));
		add(newCop1Fn(FP_FN_DIV, "div.fmt", FDFSFT, "div.{fpfmt}"));
		add(newCop1Fn(FP_FN_MOV, "mov.fmt", FDFSFT, "mov.{fpfmt}"));
		add(newCop1Fn(FP_FN_NEG, "neg.fmt", FDFSFT, "neg.{fpfmt}"));
		add(newCop1Fn(FP_FN_CVT_S, "cvts.fmt", FDFSFT, "cvts.{fpfmt}"));
		add(newCop1Fn(FP_FN_CVT_D, "cvtd.fmt", FDFSFT, "cvtd.{fpfmt}"));
		add(newCop1Fn(FP_FN_CVT_W, "cvtw.fmt", FDFSFT, "cvtw.{fpfmt}"));
		add(newCop1Fn(FP_FN_C_ULT, "c.ult.fmt", CCFSFT, "ult.{fpc}"));
		add(newCop1Fn(FP_FN_C_EQ,  "c.eq.f", CCFSFT, "eq.{fptf}"));
		add(newCop1Fn(FP_FN_C_LT,  "c.lt.f", CCFSFT, "lt.{fptf}"));
		add(newCop1Fn(FP_FN_C_LE,  "c.le.f", CCFSFT, "le.{fptf}"));
	}
	
	private void add (Isn isn) {
		switch (isn.op) {
			case OP_SPECIAL:
				set(fn, isn.fn, isn);
				break;
				
			case OP_REGIMM:
				set(regimm, isn.rt, isn);
				break;
				
			case OP_COP0:
				if (isn.rs < CP_RS_CO) {
					set(system, isn.rs, isn);
				} else {
					set(systemFn, isn.fn, isn);
				}
				break;
				
			case OP_COP1:
				if (isn.rs < FP_RS_S) {
					set(fpu, isn.rs, isn);
				} else {
					set(fpuFn, isn.fn, isn);
				}
				break;
				
			case OP_SPECIAL2:
				set(fn2, isn.fn, isn);
				break;
				
			default:
				set(op, isn.op, isn);
		}
	}
	
}
