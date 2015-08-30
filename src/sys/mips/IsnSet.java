package sys.mips;

import java.util.*;

import static sys.mips.MipsConstants.*;
import static sys.mips.Isn.*;
import static sys.mips.Decoder.*;
import static sys.mips.IsnUtil.*;

/**
 * all instructions (not including the actual implementation)
 */
public class IsnSet {
	
	public static final IsnSet INSTANCE = new IsnSet();
	
	// interactive disassembly types
	// official format: actual values
	private static final String SF_LOAD = "{rt} <- [{base}+{offset}]: {membaseoffset} <- {baseoffset}";
	private static final String SF_STORE = "[{base}+{offset}] <- {rt}: [{baseoffset}] <- {regrt}";
	private static final String SF_JUMP = "{jump}";
	private static final String SF_CONDBRA = "{rs} ~ {rt}, {branch}: {regrs} ~ {regrt}";
	private static final String SF_IMM = "{rt} <- {rs} * {imm}";
	private static final String SF_REG = "{rd} <- {rs} * {rt}";
	private static final String SF_REG2 = "{rd} <- {rs}";
	private static final String SF_COND = "{rs} ~ {rt}";
	private static final String SF_SHIFT = "{rd} <- {rt} * {sa}";
	private static final String SF_SHIFTREG = "{rd} <- {rt} * {rs}";
	private static final String SF_ZCONDBRA = "{rs} ~ 0, {branch}: {regrs} ~ 0";
	private static final String SF_ZCONDMOV = "{rd} <- {rs}, {rt} ~ 0: {regrs}, {regrt} ~ 0";
	private static final String SF_HILO = "hi:lo <- {rs} * {rt}";
	private static final String SF_FP_COND = "{fs} ~ {ft}: {regfs} ~ {regft}";
	private static final String SF_FP_REG = "{fd} <- {fs} * {ft}: {regfs} * {regft}";
	private static final String SF_FP_REG2 = "{fd} <- {fs}: {regfs}";
	
	private static void set (Isn[] isns, int i, Isn isn) {
		if (isns[i] != null) {
			throw new RuntimeException("duplicate " + isn.toString());
		}
		isns[i] = isn;
	}
	
	//
	// instance stuff
	//
	
	/** all instructions by name */
	public final SortedMap<String, Isn> nameMap = new TreeMap<>();
	
	private final Isn[] operation = new Isn[64];
	private final Isn[] function = new Isn[64];
	private final Isn[] function2 = new Isn[64];
	private final Isn[] regimm = new Isn[32];
	private final Isn[] systemRs = new Isn[32];
	private final Isn[] systemFn = new Isn[64];
	private final Isn[] fpuRs = new Isn[32];
	private final Isn[] fpuFnSingle = new Isn[64];
	private final Isn[] fpuFnDouble = new Isn[64];
	private final Isn[] fpuFnWord = new Isn[64];
	private final Isn[] fpuFnLong = new Isn[64];
	private final Isn[] fpuFnX = new Isn[64];
	
	private IsnSet () {
		addIsn(newOp(OP_J, "j", SF_JUMP));
		addIsn(newOp(OP_JAL, "jal", SF_JUMP));
		addIsn(newOp(OP_BEQ, "beq", SF_CONDBRA));
		addIsn(newOp(OP_BNE, "bne", SF_CONDBRA));
		addIsn(newOp(OP_BLEZ, "blez", SF_ZCONDBRA));
		addIsn(newOp(OP_BGTZ, "bgtz", SF_ZCONDBRA));
		addIsn(newOp(OP_ADDI, "addi", SF_IMM));
		addIsn(newOp(OP_ADDIU, "addiu", SF_IMM));
		addIsn(newOp(OP_SLTI, "slti", SF_IMM));
		addIsn(newOp(OP_SLTIU, "sltiu", SF_IMM));
		addIsn(newOp(OP_ANDI, "andi", SF_IMM));
		addIsn(newOp(OP_ORI, "ori", SF_IMM));
		addIsn(newOp(OP_XORI, "xori", SF_IMM));
		addIsn(newOp(OP_LUI, "lui", "{rt} <- {imm}"));
		addIsn(newOp(OP_BEQL, "beql", ""));
		addIsn(newOp(OP_LB, "lb", SF_LOAD));
		addIsn(newOp(OP_LH, "lh", SF_LOAD));
		addIsn(newOp(OP_LWL, "lwl", SF_LOAD));
		addIsn(newOp(OP_LW, "lw", SF_LOAD));
		addIsn(newOp(OP_LBU, "lbu", SF_LOAD));
		addIsn(newOp(OP_LHU, "lhu", SF_LOAD));
		addIsn(newOp(OP_LWR, "lwr", SF_LOAD));
		addIsn(newOp(OP_SB, "sb", SF_STORE));
		addIsn(newOp(OP_SH, "sh", SF_STORE));
		addIsn(newOp(OP_SWL, "swl", SF_STORE));
		addIsn(newOp(OP_SW, "sw", SF_STORE));
		addIsn(newOp(OP_SC, "sc", SF_STORE));
		addIsn(newOp(OP_SWR, "swr", SF_STORE));
		addIsn(newOp(OP_LL, "ll", SF_LOAD));
		addIsn(newOp(OP_LWC1, "lwc1", "{ft} <- [{base}+{offset}]: {membaseoffsets} <- {baseoffset}"));
		addIsn(newOp(OP_PREF, "pref", "{rt}, [{base}+{offset}]: {baseoffset}"));
		addIsn(newOp(OP_LDC1, "ldc1",  "{ft} <- [{base}+{offset}]: {membaseoffsetd} <- {baseoffset}"));
		addIsn(newOp(OP_SWC1, "swc1", "[{base}+{offset}] <- {ft}: [{baseoffset}] <- {regfts}"));
		addIsn(newOp(OP_SDC1, "sdc1", "[{base}+{offset}] <- {ft}: [{baseoffset}] <- {regftd}"));
		
		addIsn(newRegimm(RT_BLTZ, "bltz", SF_ZCONDBRA));
		addIsn(newRegimm(RT_BGEZ, "bgez", SF_ZCONDBRA));
		addIsn(newRegimm(RT_BLTZAL, "bltzal", SF_ZCONDBRA));
		addIsn(newRegimm(RT_BGEZAL, "bgezal", SF_ZCONDBRA));
		
		addIsn(newFn(FN_SLL, "sll", SF_SHIFT));
		addIsn(newFn(FN_SRL, "srl", SF_SHIFT));
		addIsn(newFn(FN_SRA, "sra", SF_SHIFT));
		addIsn(newFn(FN_SLLV, "sllv", SF_SHIFTREG));
		addIsn(newFn(FN_SRLV, "srlv", SF_SHIFTREG));
		addIsn(newFn(FN_SRAV, "srav", SF_SHIFTREG));
		addIsn(newFn(FN_JR, "jr", "{rs}: {regrs}"));
		addIsn(newFn(FN_JALR, "jalr", "{rd}, {rs}: {regrs}"));
		addIsn(newFn(FN_MOVZ, "movz", SF_ZCONDMOV));
		addIsn(newFn(FN_MOVN, "movn", SF_ZCONDMOV));
		addIsn(newFn(FN_SYSCALL, "syscall", "{syscall}"));
		addIsn(newFn(FN_BREAK, "break", "{syscall}"));
		addIsn(newFn(FN_MFHI, "mfhi", "{rd} <- hi : {hi}"));
		addIsn(newFn(FN_MFLO, "mflo", "{rd} <- lo : {lo}"));
		addIsn(newFn(FN_MTHI, "mthi", "hi <- {rs} : {regrs}"));
		addIsn(newFn(FN_MTLO, "mtlo", "lo <- {rs} : {regrs}"));
		addIsn(newFn(FN_MULT, "mult", SF_HILO));
		addIsn(newFn(FN_MULTU, "multu", SF_HILO));
		addIsn(newFn(FN_DIV, "div", SF_HILO));
		addIsn(newFn(FN_DIVU, "divu", SF_HILO));
		addIsn(newFn(FN_ADD, "add", SF_REG));
		addIsn(newFn(FN_ADDU, "addu", SF_REG));
		addIsn(newFn(FN_SUBU, "subu", SF_REG));
		addIsn(newFn(FN_AND, "and", SF_REG));
		addIsn(newFn(FN_OR, "or", SF_REG));
		addIsn(newFn(FN_XOR, "xor", SF_REG));
		addIsn(newFn(FN_NOR, "nor", SF_REG));
		addIsn(newFn(FN_SLT, "slt", SF_REG));
		addIsn(newFn(FN_SLTU, "sltu", SF_REG));
		addIsn(newFn(FN_TNE, "tne", SF_COND));
		
		addIsn(newFn2(FN2_MUL, "mul", SF_REG));
		addIsn(newFn2(FN2_CLZ, "clz", SF_REG2));
		
		addIsn(newCop0(CP_RS_MFC0, "mfc0", "{rt} <- {cprd}"));
		addIsn(newCop0(CP_RS_MTC0, "mtc0", "{cprd} <- {rt}: {regrt}"));
		addIsn(newCop0(CP_RS_RDPGPR, "rdpgpr", ""));
		addIsn(newCop0(CP_RS_WRPGPR, "wrpgpr", ""));
		
		addIsn(newCop0Fn(CP_FN_TLBR, "tlbr", ""));
		addIsn(newCop0Fn(CP_FN_TLBWI, "tlbwi", ""));
		addIsn(newCop0Fn(CP_FN_TLBINV, "tlbinv", ""));
		addIsn(newCop0Fn(CP_FN_TLBINVF, "tlbinvf", ""));
		addIsn(newCop0Fn(CP_FN_TLBWR, "tlbiwr", ""));
		addIsn(newCop0Fn(CP_FN_TLBP, "tlbp", ""));
		
		addIsn(newCop1(FP_RS_MFC1, "mfc1", "{rt} <- {fs}: {regfsx}"));
		addIsn(newCop1(FP_RS_CFC1, "cfc1", "{rt} <- {fscw}"));
		addIsn(newCop1(FP_RS_MTC1, "mtc1", "{fs} <- {rt}: {regrtx}"));
		addIsn(newCop1(FP_RS_CTC1, "ctc1", "{fscw} <- {rt}"));
		addIsn(newCop1(FP_RS_BC1, "bc1", "{fptf}, {fpcc}, {branch} : {regfpcc}"));
		
		for (int rs : new int[] { FP_RS_S, FP_RS_D }) {
			String f = fpFormatString(rs);
			addIsn(newCop1Fn(rs, FP_FN_ABS, "abs." + f, SF_FP_REG2));
			addIsn(newCop1Fn(rs, FP_FN_ADD, "add." + f, SF_FP_REG));
			addIsn(newCop1Fn(rs, FP_FN_SUB, "sub." + f, SF_FP_REG));
			addIsn(newCop1Fn(rs, FP_FN_MUL, "mul." + f, SF_FP_REG));
			addIsn(newCop1Fn(rs, FP_FN_DIV, "div." + f, SF_FP_REG));
			addIsn(newCop1Fn(rs, FP_FN_MOV, "mov." + f, SF_FP_REG2));
			addIsn(newCop1Fn(rs, FP_FN_NEG, "neg." + f, SF_FP_REG2));
			addIsn(newCop1Fn(rs, FP_FN_C_ULT, "c.ult." + f, SF_FP_COND));
			addIsn(newCop1Fn(rs, FP_FN_C_EQ, "c.eq." + f, SF_FP_COND));
			addIsn(newCop1Fn(rs, FP_FN_C_LT, "c.lt." + f, SF_FP_COND));
			addIsn(newCop1Fn(rs, FP_FN_C_LE, "c.le." + f, SF_FP_COND));
		}
		
		for (int rs : new int[] { FP_RS_S, FP_RS_D, FP_RS_W, FP_RS_L }) {
			String f = fpFormatString(rs);
			addIsn(newCop1Fn(rs, FP_FN_CVT_S, "cvt.s." + f, SF_FP_REG2));
			addIsn(newCop1Fn(rs, FP_FN_CVT_D, "cvt.d." + f, SF_FP_REG2));
			addIsn(newCop1Fn(rs, FP_FN_CVT_W, "cvt.w." + f, SF_FP_REG2));
		}
		
		addIsn(newCop1FnX(FP_FNX_MADDS, "madd.s", "{fd} <- {fs} * {ft} + {fr}: {regfss} * {regfts} + {regfrs}"));
	}

	public Isn getIsn (int isn) {
		final int op = op(isn);
		final int rs = rs(isn);
		final int rt = rt(isn);
		final int fn = fn(isn);
		
		switch (op) {
			case OP_SPECIAL:
				return function[fn];
			case OP_REGIMM:
				return regimm[rt];
			case OP_COP0:
				if (rs < CP_RS_CO) {
					return systemRs[rs];
				} else {
					return systemFn[fn];
				}
			case OP_COP1:
				switch (rs) {
					case FP_RS_S:
						return fpuFnSingle[fn];
					case FP_RS_D:
						return fpuFnDouble[fn];
					case FP_RS_W:
						return fpuFnWord[fn];
					case FP_RS_L:
						return fpuFnLong[fn];
					default:
						return fpuRs[rs];
				}
			case OP_COP1X:
				return fpuFnX[fn];
			case OP_SPECIAL2:
				return function2[fn];
			default:
				return operation[op];
		}
	}
	
	private void addIsn (Isn isn) {
		if (nameMap.put(isn.name, isn) != null) {
			throw new RuntimeException("duplicate name " + isn);
		}
		
		switch (isn.op) {
			case OP_SPECIAL:
				set(function, isn.fn, isn);
				break;
				
			case OP_REGIMM:
				set(regimm, isn.rt, isn);
				break;
				
			case OP_COP0:
				if (isn.rs < CP_RS_CO) {
					set(systemRs, isn.rs, isn);
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
						set(fpuRs, isn.rs, isn);
				}
				break;
				
			case OP_SPECIAL2:
				set(function2, isn.fn, isn);
				break;
				
			default:
				set(operation, isn.op, isn);
		}
	}
	
}
