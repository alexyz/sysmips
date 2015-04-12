package sys.mips;

import static sys.mips.MipsConstants.*;
import static sys.mips.Isn.*;

import java.util.*;

/**
 * all instructions
 */
public class IsnSet {
	
	public static final IsnSet ISNSET = new IsnSet();
	
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

	private static final Isn NOP = new Isn("nop");
	
	public static Isn getIsn(int isn) {
		final int op = op(isn);
		final int rs = rs(isn);
		final int rt = rt(isn);
		final int rd = rd(isn);
		final int fn = fn(isn);
		
		final Isn isnObj;
		switch (op) {
			case OP_SPECIAL:
				if (fn == FN_SLL && rd == 0) {
					isnObj = NOP;
				} else {
					isnObj = ISNSET.fn[fn];
				}
				break;
			case OP_REGIMM:
				isnObj = ISNSET.regimm[rt];
				break;
			case OP_COP0:
				if (rs < CP_RS_CO) {
					isnObj = ISNSET.system[rs];
				} else {
					isnObj = ISNSET.systemFn[fn];
				}
				break;
			case OP_COP1:
				if (rs < FP_RS_S) {
					isnObj = ISNSET.fpu[rs];
				} else {
					// XXX not always s...
					isnObj = ISNSET.fpuFnSingle[fn];
				}
				break;
			case OP_SPECIAL2:
				isnObj = ISNSET.fn2[fn];
				break;
			default:
				isnObj = ISNSET.op[op];
		}
		
		return isnObj;
	}
	
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
		add(newOp(OP_J, "j", SF_JUMP));
		add(newOp(OP_JAL, "jal", SF_JUMP));
		add(newOp(OP_BEQ, "beq", SF_CONDBRA));
		add(newOp(OP_BNE, "bne", SF_CONDBRA));
		add(newOp(OP_BLEZ, "blez", SF_ZCONDBRA));
		add(newOp(OP_BGTZ, "bgtz", SF_ZCONDBRA));
		add(newOp(OP_ADDI, "addi", SF_OPIMM));
		add(newOp(OP_ADDIU, "addiu", SF_OPIMM));
		add(newOp(OP_SLTI, "slti", SF_OPIMM));
		add(newOp(OP_SLTIU, "sltiu", SF_OPIMM));
		add(newOp(OP_ANDI, "andi", SF_OPIMM));
		add(newOp(OP_ORI, "ori", SF_OPIMM));
		add(newOp(OP_XORI, "xori", SF_OPIMM));
		add(newOp(OP_LUI, "lui", "{rt} <- {imm}"));
		add(newOp(OP_BEQL, "beql", ""));
		add(newOp(OP_LB, "lb", SF_LOAD));
		add(newOp(OP_LH, "lh", SF_LOAD));
		add(newOp(OP_LWL, "lwl", SF_LOAD));
		add(newOp(OP_LW, "lw", SF_LOAD));
		add(newOp(OP_LBU, "lbu", SF_LOAD));
		add(newOp(OP_LHU, "lhu", SF_LOAD));
		add(newOp(OP_LWR, "lwr", SF_LOAD));
		add(newOp(OP_SB, "sb", SF_STORE));
		add(newOp(OP_SH, "sh", SF_STORE));
		add(newOp(OP_SWL, "swl", SF_STORE));
		add(newOp(OP_SW, "sw", SF_STORE));
		add(newOp(OP_SC, "sc", SF_STORE));
		add(newOp(OP_SWR, "swr", SF_STORE));
		add(newOp(OP_LL, "ll", SF_LOAD));
		add(newOp(OP_LWC1, "lwc1", "{ft} <- [{base}+{offset}]: {membaseoffset} <- [{baseoffset}]"));
		add(newOp(OP_SWC1, "swc1", ""));
		
		add(newRegimm(RT_BLTZ, "bltz", SF_ZCONDBRA));
		add(newRegimm(RT_BGEZ, "bgez", SF_ZCONDBRA));
		add(newRegimm(RT_BLTZAL, "bltzal", SF_ZCONDBRA));
		add(newRegimm(RT_BGEZAL, "bgezal", SF_ZCONDBRA));
		
		add(newFn(FN_SLL, "sll", SF_SHIFT));
		add(newFn(FN_SRL, "srl", SF_SHIFT));
		add(newFn(FN_SRA, "sra", SF_SHIFT));
		add(newFn(FN_SLLV, "sllv", SF_SHIFTREG));
		add(newFn(FN_SRLV, "srlv", SF_SHIFTREG));
		add(newFn(FN_SRAV, "srav", SF_SHIFTREG));
		add(newFn(FN_JR, "jr", "{rs} -> {regrs}"));
		add(newFn(FN_JALR, "jalr", "{rd} <- link, {rs} => {regrs}"));
		add(newFn(FN_MOVZ, "movz", SF_CONDZMOV));
		add(newFn(FN_MOVN, "movn", SF_CONDZMOV));
		add(newFn(FN_SYSCALL, "syscall", "{syscall}"));
		add(newFn(FN_BREAK, "break", "{syscall}"));
		add(newFn(FN_MFHI, "mfhi", "{rd} <- hi : {hi}"));
		add(newFn(FN_MFLO, "mflo", "{rd} <- lo : {lo}"));
		add(newFn(FN_MTHI, "mthi", "hi <- {rs} : {regrs}"));
		add(newFn(FN_MTLO, "mtlo", "lo <- {rs} : {regrs}"));
		add(newFn(FN_MULT, "mult", SF_HLOP));
		add(newFn(FN_MULTU, "multu", SF_HLOP));
		add(newFn(FN_DIV, "div", SF_HLOP));
		add(newFn(FN_DIVU, "divu", SF_HLOP));
		add(newFn(FN_ADD, "add", SF_OP));
		add(newFn(FN_ADDU, "addu", SF_OP));
		add(newFn(FN_SUBU, "subu", SF_OP));
		add(newFn(FN_AND, "and", SF_OP));
		add(newFn(FN_OR, "or", SF_OP));
		add(newFn(FN_XOR, "xor", SF_OP));
		add(newFn(FN_NOR, "nor", SF_OP));
		add(newFn(FN_SLT, "slt", SF_OP));
		add(newFn(FN_SLTU, "sltu", SF_OP));
		add(newFn(FN_TNE, "tne", SF_COND));
		
		add(newFn2(FN2_MUL, "mul", SF_OP));
		
		add(newCop0(CP_RS_MFC0, "mfc0", "{rt} <- {cprd}"));
		add(newCop0(CP_RS_MFH, "mfh", ""));
		add(newCop0(CP_RS_MTC0, "mtc0", "{cprd} <- {rt}"));
		add(newCop0(CP_RS_MTHC0, "mthc0", ""));
		add(newCop0(CP_RS_RDPGPR, "rdpgpr", ""));
		add(newCop0(CP_RS_WRPGPR, "wrpgpr", ""));
		
		add(newCop0Fn(CP_FN_TLBINV, "tlbinv", ""));
		add(newCop0Fn(CP_FN_TLBINVF, "tlbinvf", ""));
		add(newCop0Fn(CP_FN_TLBP, "tlbp", ""));
		add(newCop0Fn(CP_FN_TLBR, "tlbr", ""));
		add(newCop0Fn(CP_FN_TLBWI, "tlbwi", ""));
		add(newCop0Fn(CP_FN_TLBWR, "tlbiwr", ""));
		
		add(newCop1(FP_RS_MFC1, "mfc1", ""));
		add(newCop1(FP_RS_CFC1, "cfc1", ""));
		add(newCop1(FP_RS_MTC1, "mtc1", ""));
		add(newCop1(FP_RS_CTC1, "ctc1", ""));
		add(newCop1(FP_RS_BC1, "bc1", ""));
		
		for (int rs : new int[] { FP_RS_S, FP_RS_D, FP_RS_W, FP_RS_L }) {
			String f = fpFormatName(rs);
			// not all of these apply to all formats...
			add(newCop1Fn(rs, FP_FN_ABS, "abs." + f, "abs.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_ADD, "add." + f, "add.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_SUB, "sub." + f, "sub.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_MUL, "mul." + f, "mul.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_DIV, "div." + f, "div.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_MOV, "mov." + f, "mov.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_NEG, "neg." + f, "neg.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_CVT_S, "cvts." + f, "cvts.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_CVT_D, "cvtd." + f, "cvtd.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_CVT_W, "cvtw." + f, "cvtw.{fpfmt}"));
			add(newCop1Fn(rs, FP_FN_C_ULT, "c.ult." + f, "ult.{fpc}"));
			add(newCop1Fn(rs, FP_FN_C_EQ, "c.eq." + f, "eq.{fptf}"));
			add(newCop1Fn(rs, FP_FN_C_LT, "c.lt." + f, "lt.{fptf}"));
			add(newCop1Fn(rs, FP_FN_C_LE, "c.le." + f, "le.{fptf}"));
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
