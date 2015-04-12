package sys.mips;

import static sys.mips.MipsConstants.*;
import static sys.mips.Isn.*;

import java.util.*;

/**
 * all instructions
 */
public class IsnSet {
	
	public static final IsnSet ISNSET = new IsnSet();
	
	//
	// assembly types (not just I, J and R...)
	//
	
	/** number, offset */
	public static final String T_I2_N = "I2N";
	/** rt, rs, imm */
	public static final String T_I3 = "I3";
	/** rs, offset */
	public static final String T_IB2 = "IB2";
	/** rs, rt, offset */
	public static final String T_IB3 = "IB3";
	/** target */
	public static final String T_J1 = "J";
	/** [syscall] */
	public static final String T_S = "S";
	/** rd, rs, rt */
	public static final String T_R3 = "R3";
	/** rd, rt, sa */
	public static final String T_R3S = "R3S";
	/** fd, fs, ft */
	public static final String T_FR3 = "FR3";
	/** fd, fs */
	public static final String T_FR2 = "FR2";
	
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
			throw new RuntimeException("duplicate " + isn.toString());
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
	public final Isn[] fpuFnSingle = new Isn[64];
	public final Isn[] fpuFnDouble = new Isn[64];
	public final Isn[] fpuFnWord = new Isn[64];
	public final Isn[] fpuFnLong = new Isn[64];
	/** all instructions by name */
	public final SortedMap<String, Isn> names = new TreeMap<>();
	
	private IsnSet () {
		add(newOp(OP_J, "j", T_J1, SF_JUMP));
		add(newOp(OP_JAL, "jal", T_J1, SF_JUMP));
		add(newOp(OP_BEQ, "beq", T_IB3, SF_CONDBRA));
		add(newOp(OP_BNE, "bne", T_IB3, SF_CONDBRA));
		add(newOp(OP_BLEZ, "blez", T_IB2, SF_ZCONDBRA));
		add(newOp(OP_BGTZ, "bgtz", T_IB2, SF_ZCONDBRA));
		add(newOp(OP_ADDI, "addi", T_I3, SF_OPIMM));
		add(newOp(OP_ADDIU, "addiu", T_I3, SF_OPIMM));
		add(newOp(OP_SLTI, "slti", T_I3, SF_OPIMM));
		add(newOp(OP_SLTIU, "sltiu", T_I3, SF_OPIMM));
		add(newOp(OP_ANDI, "andi", T_I3, SF_OPIMM));
		add(newOp(OP_ORI, "ori", T_I3, SF_OPIMM));
		add(newOp(OP_XORI, "xori", T_I3, SF_OPIMM));
		add(newOp(OP_LUI, "lui", T_I3, "{rt} <- {imm}"));
		add(newOp(OP_BEQL, "beql", T_I3, ""));
		add(newOp(OP_LB, "lb", T_I3, SF_LOAD));
		add(newOp(OP_LH, "lh", T_I3, SF_LOAD));
		add(newOp(OP_LWL, "lwl", T_I3, SF_LOAD));
		add(newOp(OP_LW, "lw", T_I3, SF_LOAD));
		add(newOp(OP_LBU, "lbu", T_I3, SF_LOAD));
		add(newOp(OP_LHU, "lhu", T_I3, SF_LOAD));
		add(newOp(OP_LWR, "lwr", T_I3, SF_LOAD));
		add(newOp(OP_SB, "sb", T_I3, SF_STORE));
		add(newOp(OP_SH, "sh", T_I3, SF_STORE));
		add(newOp(OP_SWL, "swl", T_I3, SF_STORE));
		add(newOp(OP_SW, "sw", T_I3, SF_STORE));
		add(newOp(OP_SC, "sc", T_I3, SF_STORE));
		add(newOp(OP_SWR, "swr", T_I3, SF_STORE));
		add(newOp(OP_LL, "ll", T_I3, SF_LOAD));
		add(newOp(OP_LWC1, "lwc1", T_I3, ""));
		add(newOp(OP_SWC1, "swc1", T_I3, ""));
		
		add(newRegimm(RT_BLTZ, "bltz", T_IB2, SF_ZCONDBRA));
		add(newRegimm(RT_BGEZ, "bgez", T_IB2, SF_ZCONDBRA));
		add(newRegimm(RT_BLTZAL, "bltzal", "", SF_ZCONDBRA));
		add(newRegimm(RT_BGEZAL, "bgezal", T_IB2, SF_ZCONDBRA));
		
		add(newFn(FN_SLL, "sll", T_R3S, SF_SHIFT));
		add(newFn(FN_SRL, "srl", T_R3S, SF_SHIFT));
		add(newFn(FN_SRA, "sra", T_R3, SF_SHIFT));
		add(newFn(FN_SLLV, "sllv", T_R3, SF_SHIFTREG));
		add(newFn(FN_SRLV, "srlv", T_R3, SF_SHIFTREG));
		add(newFn(FN_SRAV, "srav", T_R3, SF_SHIFTREG));
		add(newFn(FN_JR, "jr", T_R3, "{rs} -> {regrs}"));
		add(newFn(FN_JALR, "jalr", T_R3, "{rd} <- link, {rs} => {regrs}"));
		add(newFn(FN_MOVZ, "movz", T_R3, SF_CONDZMOV));
		add(newFn(FN_MOVN, "movn", T_R3, SF_CONDZMOV));
		add(newFn(FN_SYSCALL, "syscall", T_S, "{syscall}"));
		add(newFn(FN_BREAK, "break", T_S, "{syscall}"));
		add(newFn(FN_MFHI, "mfhi", T_R3, "{rd} <- hi : {hi}"));
		add(newFn(FN_MFLO, "mflo", T_R3, "{rd} <- lo : {lo}"));
		add(newFn(FN_MTHI, "mthi", T_R3, "hi <- {rs} : {regrs}"));
		add(newFn(FN_MTLO, "mtlo", T_R3, "lo <- {rs} : {regrs}"));
		add(newFn(FN_MULT, "mult", T_R3, SF_HLOP));
		add(newFn(FN_MULTU, "multu", T_R3, SF_HLOP));
		add(newFn(FN_DIV, "div", T_R3, SF_HLOP));
		add(newFn(FN_DIVU, "divu", T_R3, SF_HLOP));
		add(newFn(FN_ADD, "add", T_R3, SF_OP));
		add(newFn(FN_ADDU, "addu", T_R3, SF_OP));
		add(newFn(FN_SUBU, "subu", T_R3, SF_OP));
		add(newFn(FN_AND, "and", T_R3, SF_OP));
		add(newFn(FN_OR, "or", T_R3, SF_OP));
		add(newFn(FN_XOR, "xor", T_R3, SF_OP));
		add(newFn(FN_NOR, "nor", T_R3, SF_OP));
		add(newFn(FN_SLT, "slt", T_R3, SF_OP));
		add(newFn(FN_SLTU, "sltu", T_R3, SF_OP));
		add(newFn(FN_TNE, "tne", T_R3, SF_COND));
		
		add(newFn2(FN2_MUL, "mul", T_R3, SF_OP));
		
		add(newCop0(CP_RS_MFC0, "mfc0", "", "{rt} <- {cprd}"));
		add(newCop0(CP_RS_MFH, "mfh", "", ""));
		add(newCop0(CP_RS_MTC0, "mtc0", "", "{cprd} <- {rt}"));
		add(newCop0(CP_RS_MTHC0, "mthc0", "", ""));
		add(newCop0(CP_RS_RDPGPR, "rdpgpr", "", ""));
		add(newCop0(CP_RS_WRPGPR, "wrpgpr", "", ""));
		
		add(newCop0Fn(CP_FN_TLBINV, "tlbinv", "", ""));
		add(newCop0Fn(CP_FN_TLBINVF, "tlbinvf", "", ""));
		add(newCop0Fn(CP_FN_TLBP, "tlbp", "", ""));
		add(newCop0Fn(CP_FN_TLBR, "tlbr", "", ""));
		add(newCop0Fn(CP_FN_TLBWI, "tlbwi", "", ""));
		add(newCop0Fn(CP_FN_TLBWR, "tlbiwr", "", ""));
		
		add(newCop1(FP_RS_MFC1, "mfc1", "", ""));
		add(newCop1(FP_RS_CFC1, "cfc1", "", ""));
		add(newCop1(FP_RS_MTC1, "mtc1", "", ""));
		add(newCop1(FP_RS_CTC1, "ctc1", "", ""));
		add(newCop1(FP_RS_BC1, "bc1", T_I2_N, ""));
		
		for (int rs : new int[] { FP_RS_S, FP_RS_D, FP_RS_W, FP_RS_L }) {
			String f = fpFormatName(rs);
			// not all of these apply to all formats...
			add(newCop1Fn(rs, FP_FN_ABS, "abs." + f, T_FR2, "abs.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_ADD, "add." + f, T_FR3, "add.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_SUB, "sub." + f, T_FR3, "sub.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_MUL, "mul." + f, T_FR3, "mul.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_DIV, "div." + f, T_FR3, "div.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_MOV, "mov." + f, T_FR2, "mov.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_NEG, "neg." + f, T_FR2, "neg.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_CVT_S, "cvts." + f, T_FR3, "cvts.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_CVT_D, "cvtd." + f, T_FR3, "cvtd.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_CVT_W, "cvtw." + f, T_FR3, "cvtw.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_C_ULT, "c.ult." + f, "", "ult.{fpc}"));
			add(newCop1Fn(rs, FP_FN_C_EQ,  "c.eq." + f, "", "eq.{fptf}"));
			add(newCop1Fn(rs, FP_FN_C_LT,  "c.lt." + f, "", "lt.{fptf}"));
			add(newCop1Fn(rs, FP_FN_C_LE,  "c.le." + f, "", "le.{fptf}"));
		}
	}
	
	private void add (Isn isn) {
		if (names.put(isn.name, isn) != null) {
			throw new RuntimeException("duplicate name " + isn);
		}
		
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
				switch (isn.rs) {
					case FP_RS_S:
						set(fpuFnSingle, isn.fn, isn);
						break;
					case FP_RS_D:
						set(fpuFnDouble, isn.fn, isn);
						break;
					case FP_RS_W:
						set(fpuFnWord, isn.fn, isn);
						break;
					case FP_RS_L:
						set(fpuFnLong, isn.fn, isn);
						break;
					default: 
						set(fpu, isn.rs, isn);
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
