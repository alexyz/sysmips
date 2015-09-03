package sys.mips;

import java.util.*;

import static sys.mips.Constants.*;
import static sys.mips.Decoder.*;
import static sys.mips.IsnUtil.*;

/**
 * all instructions (not including the actual implementation)
 */
public class IsnSet {
	
	private static final IsnSet instance = new IsnSet();
	
	// interactive disassembly types
	// official format: actual values
	private static final String SF_LOAD = "{rt} <- [{base}+{offset}]: {membaseoffset} <- {baseoffset}";
	private static final String SF_STORE = "[{base}+{offset}] <- {rt}: [{baseoffset}] <- {regrt}";
	private static final String SF_JUMP = "{jump}";
	private static final String SF_CONDBRA = "{rs} ~ {rt}, {branch}: {regrs} ~ {regrt}";
	private static final String SF_IMM = "{rt} <- {rs} * {imm}: {regrs}";
	private static final String SF_REG = "{rd} <- {rs} * {rt}: {regrs} * {regrt}";
	private static final String SF_REG2 = "{rd} <- {rs}: {regrs}";
	private static final String SF_COND = "{rs} ~ {rt}: {regrs} ~ {regrt}";
	private static final String SF_SHIFT = "{rd} <- {rt} * {sa}: {regrt}";
	private static final String SF_SHIFTREG = "{rd} <- {rt} * {rs}: {regrt} * {regrs}";
	private static final String SF_ZCONDBRA = "{rs} ~ 0, {branch}: {regrs} ~ 0";
	private static final String SF_ZCONDMOV = "{rd} <- {rs}, {rt} ~ 0: {regrs}, {regrt} ~ 0";
	private static final String SF_HILO = "hilo <- {rs} * {rt}: {regrs} * {regrt}";
	private static final String SF_HILOAND = "hilo <- {rs} * {rt} + hilo: {regrs} * {regrt} * {hilo}";
	private static final String SF_FP_COND = "{fs} ~ {ft}: {regfs} ~ {regft}";
	private static final String SF_FP_REG = "{fd} <- {fs} * {ft}: {regfs} * {regft}";
	private static final String SF_FP_REG2 = "{fd} <- {fs}: {regfs}";
	
	public static IsnSet getInstance() {
		return instance;
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
	
	/** all instructions by name */
	private final SortedMap<String, Isn> nameMap = new TreeMap<>();
	private final SortedMap<String, Isn> nameMapUnmod = Collections.unmodifiableSortedMap(nameMap);
	
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
		addOp(OP_J, "j", SF_JUMP);
		addOp(OP_JAL, "jal", SF_JUMP);
		addOp(OP_BEQ, "beq", SF_CONDBRA);
		addOp(OP_BNE, "bne", SF_CONDBRA);
		addOp(OP_BLEZ, "blez", SF_ZCONDBRA);
		addOp(OP_BGTZ, "bgtz", SF_ZCONDBRA);
		addOp(OP_ADDI, "addi", SF_IMM);
		addOp(OP_ADDIU, "addiu", SF_IMM);
		addOp(OP_SLTI, "slti", SF_IMM);
		addOp(OP_SLTIU, "sltiu", SF_IMM);
		addOp(OP_ANDI, "andi", SF_IMM);
		addOp(OP_ORI, "ori", SF_IMM);
		addOp(OP_XORI, "xori", SF_IMM);
		addOp(OP_LUI, "lui", "{rt} <- {imm}");
		addOp(OP_BEQL, "beql", "");
		addOp(OP_BNEL, "bnel", "");
		addOp(OP_BLEZL, "blezl", "");
		addOp(OP_BGTZL, "bgtzl", "");
		addOp(OP_JALX, "jalx", "");
		addOp(OP_LB, "lb", SF_LOAD);
		addOp(OP_LH, "lh", SF_LOAD);
		addOp(OP_LWL, "lwl", SF_LOAD);
		addOp(OP_LW, "lw", SF_LOAD);
		addOp(OP_LBU, "lbu", SF_LOAD);
		addOp(OP_LHU, "lhu", SF_LOAD);
		addOp(OP_LWR, "lwr", SF_LOAD);
		addOp(OP_SB, "sb", SF_STORE);
		addOp(OP_SH, "sh", SF_STORE);
		addOp(OP_SWL, "swl", SF_STORE);
		addOp(OP_SW, "sw", SF_STORE);
		addOp(OP_SC, "sc", SF_STORE);
		addOp(OP_SWR, "swr", SF_STORE);
		addOp(OP_CACHE, "cache", "");
		addOp(OP_LL, "ll", SF_LOAD);
		addOp(OP_LWC1, "lwc1", "{ft} <- [{base}+{offset}]: {membaseoffsets} <- {baseoffset}");
		addOp(OP_LWC2, "lwc2", "");
		addOp(OP_PREF, "pref", "{rt}, [{base}+{offset}]: {baseoffset}");
		addOp(OP_LDC1, "ldc1",  "{ft} <- [{base}+{offset}]: {membaseoffsetd} <- {baseoffset}");
		addOp(OP_LDC2, "ldc2", "");
		addOp(OP_SWC1, "swc1", "[{base}+{offset}] <- {ft}: [{baseoffset}] <- {regfts}");
		addOp(OP_SWC2, "swc2", "");
		addOp(OP_SDC1, "sdc1", "[{base}+{offset}] <- {ft}: [{baseoffset}] <- {regftd}");
		addOp(OP_SDC2, "sdc2", "");
		
		addRegImm(RT_BLTZ, "bltz", SF_ZCONDBRA);
		addRegImm(RT_BGEZ, "bgez", SF_ZCONDBRA);
		addRegImm(RT_BLTZL, "bltzl", "");
		addRegImm(RT_BGEZL, "bgezl", "");
		addRegImm(RT_TGEI, "tgei", "");
		addRegImm(RT_TGEIU, "tgeiu", "");
		addRegImm(RT_TLTI, "tlti", "");
		addRegImm(RT_TLTIU, "tltiu", "");
		addRegImm(RT_TEQI, "teqi", "");
		addRegImm(RT_TNEI, "tnei", "");
		addRegImm(RT_BLTZAL, "bltzal", SF_ZCONDBRA);
		addRegImm(RT_BGEZAL, "bgezal", SF_ZCONDBRA);
		addRegImm(RT_BLTZALL, "bltzall", "");
		addRegImm(RT_BGEZALL, "bgezall", "");
		addRegImm(RT_SYNCI, "synci", "");
		
		addFn(FN_SLL, "sll", SF_SHIFT);
		addFn(FN_SRL, "srl", SF_SHIFT);
		addFn(FN_SRA, "sra", SF_SHIFT);
		addFn(FN_SLLV, "sllv", SF_SHIFTREG);
		addFn(FN_SRLV, "srlv", SF_SHIFTREG);
		addFn(FN_SRAV, "srav", SF_SHIFTREG);
		addFn(FN_JR, "jr", "{rs}: {regrs}");
		addFn(FN_JALR, "jalr", "{rd}, {rs}: {regrs}");
		addFn(FN_MOVZ, "movz", SF_ZCONDMOV);
		addFn(FN_MOVN, "movn", SF_ZCONDMOV);
		addFn(FN_SYSCALL, "syscall", "{syscall}");
		addFn(FN_BREAK, "break", "{syscall}");
		addFn(FN_SYNC, "sync", "");
		addFn(FN_MFHI, "mfhi", "{rd} <- hi : {hi}");
		addFn(FN_MFLO, "mflo", "{rd} <- lo : {lo}");
		addFn(FN_MTHI, "mthi", "hi <- {rs} : {regrs}");
		addFn(FN_MTLO, "mtlo", "lo <- {rs} : {regrs}");
		addFn(FN_MULT, "mult", SF_HILO);
		addFn(FN_MULTU, "multu", SF_HILO);
		addFn(FN_DIV, "div", SF_HILO);
		addFn(FN_DIVU, "divu", SF_HILO);
		addFn(FN_ADD, "add", SF_REG);
		addFn(FN_ADDU, "addu", SF_REG);
		addFn(FN_SUBU, "subu", SF_REG);
		addFn(FN_AND, "and", SF_REG);
		addFn(FN_OR, "or", SF_REG);
		addFn(FN_XOR, "xor", SF_REG);
		addFn(FN_NOR, "nor", SF_REG);
		addFn(FN_SLT, "slt", SF_REG);
		addFn(FN_SLTU, "sltu", SF_REG);
		addFn(FN_TNE, "tne", SF_COND);
		
		addFn2(FN2_MADD, "madd", SF_HILOAND);
		addFn2(FN2_MADDU, "maddu", SF_HILOAND);
		addFn2(FN2_MUL, "mul", SF_REG);
		addFn2(FN2_MSUB, "msub", SF_HILOAND);
		addFn2(FN2_MSUBU, "msubu", SF_HILOAND);
		addFn2(FN2_CLZ, "clz", SF_REG2);
		addFn2(FN2_CLO, "clo", "");
		addFn2(FN2_SDBBP, "sdbbp", "");
		
		addCop0(CP_RS_MFC0, "mfc0", "{rt} <- {cprd}: {cpregrd}");
		addCop0(CP_RS_MTC0, "mtc0", "{cprd} <- {rt}: {regrt}");
		addCop0(CP_RS_RDPGPR, "rdpgpr", "");
		addCop0(CP_RS_MFMC0, "mfmc0", "");
		addCop0(CP_RS_WRPGPR, "wrpgpr", "");
		
		addCop0Fn(CP_FN_TLBR, "tlbr", "");
		addCop0Fn(CP_FN_TLBWI, "tlbwi", "");
		addCop0Fn(CP_FN_TLBINV, "tlbinv", "");
		addCop0Fn(CP_FN_TLBINVF, "tlbinvf", "");
		addCop0Fn(CP_FN_TLBWR, "tlbiwr", "");
		addCop0Fn(CP_FN_TLBP, "tlbp", "");
		addCop0Fn(CP_FN_ERET, "eret", "");
		addCop0Fn(CP_FN_DERET, "deret", "");
		addCop0Fn(CP_FN_WAIT, "wait", "");
		
		addCop1(FP_RS_MFC1, "mfc1", "{rt} <- {fs}: {regfsx}");
		addCop1(FP_RS_CFC1, "cfc1", "{rt} <- {fscw}");
		addCop1(FP_RS_MTC1, "mtc1", "{fs} <- {rt}: {regrtx}");
		addCop1(FP_RS_CTC1, "ctc1", "{fscw} <- {rt}");
		addCop1(FP_RS_BC1, "bc1", "{fptf}, {fpcc}, {branch} : {regfpcc}");
		
		for (int rs : new int[] { FP_RS_S, FP_RS_D }) {
			String f = fpFormatString(rs);
			addCop1Fn(rs, FP_FN_ABS, "abs." + f, SF_FP_REG2);
			addCop1Fn(rs, FP_FN_ADD, "add." + f, SF_FP_REG);
			addCop1Fn(rs, FP_FN_SUB, "sub." + f, SF_FP_REG);
			addCop1Fn(rs, FP_FN_MUL, "mul." + f, SF_FP_REG);
			addCop1Fn(rs, FP_FN_DIV, "div." + f, SF_FP_REG);
			addCop1Fn(rs, FP_FN_MOV, "mov." + f, SF_FP_REG2);
			addCop1Fn(rs, FP_FN_NEG, "neg." + f, SF_FP_REG2);
			addCop1Fn(rs, FP_FN_C_ULT, "c.ult." + f, SF_FP_COND);
			addCop1Fn(rs, FP_FN_C_EQ, "c.eq." + f, SF_FP_COND);
			addCop1Fn(rs, FP_FN_C_LT, "c.lt." + f, SF_FP_COND);
			addCop1Fn(rs, FP_FN_C_LE, "c.le." + f, SF_FP_COND);
		}
		
		for (int rs : new int[] { FP_RS_S, FP_RS_D, FP_RS_W, FP_RS_L }) {
			String f = fpFormatString(rs);
			addCop1Fn(rs, FP_FN_CVT_S, "cvt.s." + f, SF_FP_REG2);
			addCop1Fn(rs, FP_FN_CVT_D, "cvt.d." + f, SF_FP_REG2);
			addCop1Fn(rs, FP_FN_CVT_W, "cvt.w." + f, SF_FP_REG2);
		}
		
		addCop1FnX(FP_FNX_MADDS, "madd.s", "{fd} <- {fs} * {ft} + {fr}: {regfss} * {regfts} + {regfrs}");
	}

	private void addOp (int op, String name, String format) {
		addIsn(new Isn(op, 0, 0, 0, name, format));
	}
	
	private void addRegImm (int rt, String name, String format) {
		addIsn(new Isn(OP_REGIMM, 0, rt, 0, name, format));
	}
	
	private void addFn (int fn, String name, String format) {
		addIsn(new Isn(OP_SPECIAL, 0, 0, fn, name, format));
	}
	
	private void addFn2 (int fn, String name, String format) {
		addIsn(new Isn(OP_SPECIAL2, 0, 0, fn, name, format));
	}
	
	private void addCop0 (int rs, String name, String format) {
		addIsn(new Isn(OP_COP0, rs, 0, 0, name, format));
	}
	
	private void addCop0Fn (int fn, String name, String format) {
		addIsn(new Isn(OP_COP0, CP_RS_CO, 0, fn, name, format));
	}
	
	private void addCop1 (int rs, String name, String format) {
		addIsn(new Isn(OP_COP1, rs, 0, 0, name, format));
	}
	
	private void addCop1Fn (int rs, int fn, String name, String format) {
		addIsn(new Isn(OP_COP1, rs, 0, fn, name, format));
	}
	
	private void addCop1FnX (int fn, String name, String format) {
		addIsn(new Isn(OP_COP1X, 0, 0, fn, name, format));
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
	
	public SortedMap<String, Isn> getNameMap () {
		return nameMapUnmod;
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
		
}
