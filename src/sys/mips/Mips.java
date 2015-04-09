package sys.mips;

import java.util.Arrays;

/**
 * Symbolic constants and names for MIPS instructions. Generally, CP means
 * coprocessor 0 (system control), FP means coprocessor 1 (floating point).
 */
public final class Mips {

	public static class Format {
		public static final String LOAD = "{rt} <- [{base}+{offset}]: {membaseoffset} <- {baseoffset}";
		public static final String STORE = "[{base}+{offset}] <- {rt}: [{baseoffset}] <- {regrt}";
		public static final String JUMP = "{jump}";
		public static final String CONDBRA = "{rs} ~ {rt}: {regrs} ~ {regrt} => {branch}";
		public static final String OPIMM = "{rt} <- {rs} * {imm}";
		public static final String OP = "{rd} <- {rs} * {rt}";
		public static final String COND = "{rs} ~ {rt}";
		public static final String SHIFT = "{rd} <- {rt} * {sa}";
		public static final String SHIFTREG = "{rd} <- {rt} * {rs}";
		public static final String ZCONDBRA = "{rs} ~ 0: {regrs} => {branch}";
		public static final String CONDZMOV = "{rd} <- {rs} if {regrt} ~ 0";
		public static final String HLOP = "hi:lo <- {rs} * {rt}";
	}

	public static class Status {
		public static final int ERL = 2;
		public static final int BEV = 22;
		public static final int CU = 28;
	}
	
	public static int base (final int isn) {
		return rs(isn);
	}
	
	public static int branch (final int isn, final int pc) {
		return pc + (simm(isn) * 4);
	}
	
	private static String cpRegName (int rd, int sel) {
		String name = "unknown";
		if (rd < CP_REG_NAMES.length) {
			final String[] rdnames = CP_REG_NAMES[rd];
			if (rdnames != null && sel < rdnames.length) {
				name = rdnames[sel];
			}
		}
		return rd + "." + sel + ":" + name;
	}

	public static String cpRegString (Cpu cpu) {
		final int[][] reg = cpu.getCpRegisters();
		final Memory mem = cpu.getMemory();
		final Symbols syms = mem.getSymbols();

		final StringBuilder sb = new StringBuilder(256);
		sb.append("cycle=").append(cpu.getCycle());
		for (int n = 0; n < reg.length; n++) {
			for (int m = 0; m < reg[n].length; m++) {
				final int v = reg[n][m];
				if (v != 0) {
					sb.append(" ").append(cpRegName(n, m)).append("=").append(syms.getName(v));
				}
			}
		}
		return sb.toString();
	}
	
	/** same as sa */
	public static int fd (final int isn) {
		return sa(isn);
	}

	public static int fn (final int isn) {
		return isn & 0x3f;
	}
	
	private static String formatIsn (final Isn name, final int isn, final Cpu cpu) {
		final StringBuilder sb = new StringBuilder(name.name);
		while (sb.length() < 8) {
			sb.append(" ");
		}
		sb.append(name.format);
		int i;
		while ((i = sb.indexOf("{")) >= 0) {
			final int j = sb.indexOf("}", i);
			sb.replace(i, j + 1, valueOf(sb.substring(i + 1, j), isn, cpu));
		}
		return sb.toString();
	}

	private static String fpFormatName (final int rs) {
		switch (rs) {
			case FP_RS_D:
				return "d";
			case FP_RS_S:
				return "s";
			case FP_RS_W:
				return "w";
			default:
				throw new RuntimeException("unknown fp format " + rs);
		}
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
	
	private static String gpRegName (int reg) {
		// return "$" + reg + ":" + REG_NAMES[reg];
		return REG_NAMES[reg];
	}
	
	public static String gpRegString (final Cpu cpu) {
		final int pc = cpu.getPc();
		final int[] reg = cpu.getRegisters();
		final Memory mem = cpu.getMemory();
		final Symbols syms = mem.getSymbols();

		final StringBuilder sb = new StringBuilder(256);
		sb.append("pc=").append(syms.getName(pc));
		for (int n = 0; n < reg.length; n++) {
			if (reg[n] != 0) {
				sb.append(" ").append(gpRegName(n)).append("=").append(syms.getName(reg[n]));
			}
		}
		return sb.toString();
	}
	
	/** unsigned immediate */
	public static final int imm (final int isn) {
		return isn & 0xffff;
	}
	
	/**
	 * Disassemble an instruction
	 */
	public static String isnString (final Cpu cpu) {
		final Memory mem = cpu.getMemory();
		final Symbols syms = mem.getSymbols();
		final int pc = cpu.getPc();
		final int isn = mem.loadWord(pc);
		final int op = op(isn);
		final int rs = rs(isn);
		final int rt = rt(isn);
		final int rd = rd(isn);
		final int fn = fn(isn);

		final Isn name;

		switch (op) {
			case OP_SPECIAL:
				if (fn == FN_SLL && rd == 0) {
					name = NOP;
				} else {
					name = FN_NAMES[fn];
				}
				break;
			case OP_SPEC2:
				name = FN2_NAMES[fn];
				break;
			case OP_REGIMM:
				name = RT_NAMES[rt];
				break;
			case OP_COP0:
				// privileged instruction
				if (rs < 16) {
					name = CP_RS_NAMES[rs];
				} else {
					name = CP_FN_NAMES[fn];
				}
				break;
			case OP_COP1:
				switch (rs) {
					case FP_RS_S:
					case FP_RS_D:
					case FP_RS_W:
					case FP_RS_BC1:
						name = FP_FN_NAMES[fn];
						break;
					default:
						name = FP_RS_NAMES[rs];
						break;
				}
				break;
			default:
				name = OP_NAMES[op];
		}

		final String addr = syms.getName(pc);
		final String isnValue = formatIsn(name, isn, cpu);
		return String.format("%-40s %08x %s", addr, isn, isnValue);
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
	
	private static String valueOf (final String name, final int isn, final Cpu cpu) {
		final Memory mem = cpu.getMemory();
		final int[] reg = cpu.getRegisters();
		final int pc = cpu.getPc();
		final Symbols syms = mem.getSymbols();

		switch (name) {
			case "rs":
				return gpRegName(rs(isn));
			case "base":
				return gpRegName(base(isn));
			case "offset":
				return Integer.toString(simm(isn));
			case "rd":
				return gpRegName(rd(isn));
			case "rt":
				return gpRegName(rt(isn));
			case "regrs":
				return syms.getName(reg[rs(isn)]);
				// case "regrsimm": return mem.getName(reg[rs(isn)] + imm(isn));
			case "regrd":
				return syms.getName(reg[rd(isn)]);
			case "regrt":
				return syms.getName(reg[rt(isn)]);
				// case "regrtb": return "0x" + Integer.toHexString(reg[rt(isn)]
				// & 0xff);
			case "cprd":
				return cpRegName(rd(isn), sel(isn));
			case "imm":
				return "0x" + Integer.toHexString(imm(isn));
			case "branch":
				return syms.getName(branch(isn, pc));
			case "hi":
				return "0x" + Integer.toHexString(reg[HI_GPR]);
			case "lo":
				return "0x" + Integer.toHexString(reg[LO_GPR]);
			case "baseoffset":
				return syms.getName(reg[base(isn)] + simm(isn));
			case "syscall":
				return "0x" + Integer.toHexString(syscall(isn));
			case "membaseoffset":
				return "0x" + Integer.toHexString(mem.loadWordUnchecked((reg[base(isn)] + simm(isn))));
			case "jump":
				return syms.getName(jump(isn, pc));
			case "sa":
				return String.valueOf(sa(isn));
			case "fpfmt":
				return fpFormatName(rs(isn));
			case "fptf":
				return fpTrue(isn) ? "t" : "f";
			default:
				throw new RuntimeException("unknown name " + name);
		}
	}
	
	public static final Isn[] OP_NAMES = new Isn[64];
	public static final Isn[] FN_NAMES = new Isn[64];
	public static final Isn[] FN2_NAMES = new Isn[64];
	public static final Isn[] RT_NAMES = new Isn[32];
	public static final Isn[] CP_RS_NAMES = new Isn[32];
	public static final Isn[] CP_FN_NAMES = new Isn[64];
	public static final Isn[] FP_RS_NAMES = new Isn[32];
	public static final Isn[] FP_FN_NAMES = new Isn[64];
	/** integer operations, typically with 16 bit immediate */

	public static final byte OP_SPECIAL = 0x00;
	/** a register-immediate instruction (all branches) */
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
	/** add immediate unsigned word */
	public static final byte OP_ADDIU = 0x09;
	/** set on less than immediate. compare both as signed. */
	public static final byte OP_SLTI = 0x0a;
	/**
	 * set on less than immediate unsigned. sign extend imm and compare as
	 * unsigned.
	 */
	public static final byte OP_SLTIU = 0x0b;
	/** and immediate (zero extend) */
	public static final byte OP_ANDI = 0x0c;
	/** or immediate. zx */
	public static final byte OP_ORI = 0x0d;
	/** exclusive or immediate (zx) */
	public static final byte OP_XORI = 0x0e;
	public static final byte OP_LUI = 0x0f;
	public static final byte OP_COP0 = 0x10;
	public static final byte OP_COP1 = 0x11;
	public static final byte OP_SPEC2 = 0x1c;
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
	public static final byte FN_SRL = 0x02;
	public static final byte FN_SRA = 0x03;
	public static final byte FN_SLLV = 0x04;

	public static final byte FN_SRLV = 0x06;
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
	public static final byte FN_MULT = 0x18;
	public static final byte FN_MULTU = 0x19;
	public static final byte FN_DIV = 0x1a;

	public static final byte FN_DIVU = 0x1b;
	public static final byte FN_ADDU = 0x21;
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
	public static final byte CP_RS_MTH = 0x06;
	public static final byte CP_RS_RDPGPR = 0x0a;

	public static final byte CP_RS_MFMC0 = 0x0b;
	public static final byte CP_RS_WRPGPR = 0x0e;
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
	/** single precision meta instruction */
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

	public static final byte FP_FN_ADD_D = 0x00;

	public static final byte FP_FN_SUB_D = 0x01;

	public static final byte FP_FN_MUL_D = 0x02;

	public static final byte FP_FN_DIV_D = 0x03;

	public static final byte FP_FN_ABS_D = 0x05;

	public static final byte FP_FN_MOV_D = 0x06;

	public static final byte FP_FN_NEG_D = 0x07;

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
	
	/**
	 * gas names of general registers
	 */
	public static final String[] REG_NAMES = new String[] { "zero", "at", "v0", "v1", "a0", "a1", "a2", "a3", "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7",
		"s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "t8", "t9", "k0", "k1", "gp", "sp", "s8", "ra", "hi", "lo" };

	public static final String[][] CP_REG_NAMES = new String[][] { 
		new String[] { "Index", "MVPControl", "MVPConf0", "MVPConf1" },
		new String[] { "Random", "VPEControl", "VPEConf0", "VPEConf1", "YQMask", "VPESchedule", "VPEScheFBack", "VPEOpt" },
		new String[] { "EntryLo0", "TCStatus", "TCBind", "TCRestart", "TCHalt", "TCContext", "TCSchedule", "TCScheFBack" },
		new String[] { "EntryLo1", null, null, null, null, null, null, "TCOpt", },
		new String[] { "Context", "ContentConfig", "UserLocal" },
		new String[] { "PageMask", "PageGrain", "SegCtl0", "SegCtl1", "SegCtl2", "PWBase", "PWField", "PWSize" },
		new String[] { "Wired", "SRSConf0", "SRSConf1", "SRSConf2", "SRSConf3", "SRSConf4", "PWCtl" }, new String[] { "HWREna" },
		new String[] { "BadVaddr", "BadInstr", "BadInstrP", }, 
		new String[] { "Count" },
		new String[] { "EntryHi", null, null, null, "GuestCtl1", "GuestCtl2", "GuestCtl3" }, 
		new String[] { "Compare", null, null, null, "GuestCtl0Ext" },
		new String[] { "Status", "IntCtl", "SRSCtl", "SRSMap", "View_IPL", "SRSMap2", "GuestCtl0", "GTOffset" },
		new String[] { "Cause", null, null, null, "View_RIPL", "NestedExc", }, 
		new String[] { "EPC", null, "NestedEPC" },
		new String[] { "PRId", "EBase", "CDMMBase", "CMGCRBase" },
		new String[] { "Config", "Config1", "Config2", "Config3", "Config4", "Config5" }, 
	};

	public static final Isn NOP = new Isn("nop");

	/**
	 * set up the opcode names/types array. must call from main.
	 */
	static {
		// make sure they all have non-null entries
		Arrays.fill(OP_NAMES, new Isn("undef-op"));
		Arrays.fill(FN_NAMES, new Isn("undef-fn"));
		Arrays.fill(FN2_NAMES, new Isn("undef-fn2"));
		Arrays.fill(CP_FN_NAMES, new Isn("undef-cp-fn"));
		Arrays.fill(FP_FN_NAMES, new Isn("undef-fp-fn"));
		Arrays.fill(CP_RS_NAMES, new Isn("undef-cp-rs"));
		Arrays.fill(FP_RS_NAMES, new Isn("undef-rs"));
		Arrays.fill(RT_NAMES, new Isn("undef-rt"));

		OP_NAMES[OP_SPECIAL] = new Isn("**spec");
		OP_NAMES[OP_REGIMM] = new Isn("**regimm");
		OP_NAMES[OP_J] = new Isn("j", Format.JUMP);
		OP_NAMES[OP_JAL] = new Isn("jal", Format.JUMP);
		OP_NAMES[OP_BEQ] = new Isn("beq", Format.CONDBRA);
		OP_NAMES[OP_BNE] = new Isn("bne", Format.CONDBRA);
		OP_NAMES[OP_BLEZ] = new Isn("blez", Format.ZCONDBRA);
		OP_NAMES[OP_BGTZ] = new Isn("bgtz", Format.ZCONDBRA);
		OP_NAMES[OP_ADDIU] = new Isn("addiu", Format.OPIMM);
		OP_NAMES[OP_SLTI] = new Isn("slti", Format.OPIMM);
		OP_NAMES[OP_SLTIU] = new Isn("sltiu", Format.OPIMM);
		OP_NAMES[OP_ANDI] = new Isn("andi", Format.OPIMM);
		OP_NAMES[OP_ORI] = new Isn("ori", Format.OPIMM);
		OP_NAMES[OP_XORI] = new Isn("xori", Format.OPIMM);
		OP_NAMES[OP_LUI] = new Isn("lui", "{rt} <- {imm}");
		OP_NAMES[OP_COP0] = new Isn("**cop0");
		OP_NAMES[OP_COP1] = new Isn("**cop1");
		OP_NAMES[OP_SPEC2] = new Isn("**spec2");
		OP_NAMES[OP_LL] = new Isn("ll", Format.LOAD);
		OP_NAMES[OP_LB] = new Isn("lb", Format.LOAD);
		OP_NAMES[OP_LH] = new Isn("lh", Format.LOAD);
		OP_NAMES[OP_LWL] = new Isn("lwl", Format.LOAD);
		OP_NAMES[OP_LW] = new Isn("lw", Format.LOAD);
		OP_NAMES[OP_LBU] = new Isn("lbu", Format.LOAD);
		OP_NAMES[OP_LHU] = new Isn("lhu", Format.LOAD);
		OP_NAMES[OP_LWR] = new Isn("lwr", Format.LOAD);
		OP_NAMES[OP_SB] = new Isn("sb", Format.STORE);
		OP_NAMES[OP_SH] = new Isn("sh", Format.STORE);
		OP_NAMES[OP_SWL] = new Isn("swl", Format.STORE);
		OP_NAMES[OP_SW] = new Isn("sw", Format.STORE);
		OP_NAMES[OP_SC] = new Isn("sc", Format.STORE);
		OP_NAMES[OP_SWR] = new Isn("swr", Format.STORE);
		OP_NAMES[OP_LWC1] = new Isn("lwc1");
		OP_NAMES[OP_SWC1] = new Isn("swc1");

		FN_NAMES[FN_SLL] = new Isn("sll", Format.SHIFT);
		FN_NAMES[FN_SRL] = new Isn("srl", Format.SHIFT);
		FN_NAMES[FN_SRA] = new Isn("sra", Format.SHIFT);
		FN_NAMES[FN_SLLV] = new Isn("sllv", Format.SHIFTREG);
		FN_NAMES[FN_SRLV] = new Isn("srlv", Format.SHIFTREG);
		FN_NAMES[FN_SRAV] = new Isn("srav", Format.SHIFTREG);
		FN_NAMES[FN_JR] = new Isn("jr", "{rs} -> {regrs}");
		FN_NAMES[FN_JALR] = new Isn("jalr", "{rd} <- link, {rs} => {regrs}");
		FN_NAMES[FN_MOVZ] = new Isn("movz", Format.CONDZMOV);
		FN_NAMES[FN_MOVN] = new Isn("movn", Format.CONDZMOV);
		FN_NAMES[FN_SYSCALL] = new Isn("syscall", "{syscall}");
		FN_NAMES[FN_BREAK] = new Isn("break", "{syscall}");
		FN_NAMES[FN_MFHI] = new Isn("mfhi", "{rd} <- hi : {hi}");
		FN_NAMES[FN_MFLO] = new Isn("mflo", "{rd} <- lo : {lo}");
		FN_NAMES[FN_MTHI] = new Isn("mthi", "hi <- {rs} : {regrs}");
		FN_NAMES[FN_MTLO] = new Isn("mtlo", "lo <- {rs} : {regrs}");
		FN_NAMES[FN_MULT] = new Isn("mult", Format.HLOP);
		FN_NAMES[FN_MULTU] = new Isn("multu", Format.HLOP);
		FN_NAMES[FN_DIV] = new Isn("div", Format.HLOP);
		FN_NAMES[FN_DIVU] = new Isn("divu", Format.HLOP);
		FN_NAMES[FN_ADDU] = new Isn("addu", Format.OP);
		FN_NAMES[FN_SUBU] = new Isn("subu", Format.OP);
		FN_NAMES[FN_AND] = new Isn("and", Format.OP);
		FN_NAMES[FN_OR] = new Isn("or", Format.OP);
		FN_NAMES[FN_XOR] = new Isn("xor", Format.OP);
		FN_NAMES[FN_NOR] = new Isn("nor", Format.OP);
		FN_NAMES[FN_SLT] = new Isn("slt", Format.OP);
		FN_NAMES[FN_SLTU] = new Isn("sltu", Format.OP);
		FN_NAMES[FN_TNE] = new Isn("tne", Format.COND);

		FN2_NAMES[FN2_MUL] = new Isn("mul", Format.OP);

		CP_RS_NAMES[CP_RS_MFC0] = new Isn("mfc0", "{rt} <- {cprd}");
		CP_RS_NAMES[CP_RS_MFH] = new Isn("mfh", "");
		CP_RS_NAMES[CP_RS_MFMC0] = new Isn("mfmc0", "");
		CP_RS_NAMES[CP_RS_MTC0] = new Isn("mtc0", "{cprd} <- {rt}");
		CP_RS_NAMES[CP_RS_MTH] = new Isn("mth", "");
		CP_RS_NAMES[CP_RS_RDPGPR] = new Isn("rdpgpr", "");
		CP_RS_NAMES[CP_RS_WRPGPR] = new Isn("wrpgpr", "");

		FP_RS_NAMES[FP_RS_MFC1] = new Isn("mfc1");
		FP_RS_NAMES[FP_RS_CFC1] = new Isn("cfc1");
		FP_RS_NAMES[FP_RS_MTC1] = new Isn("mtc1");
		FP_RS_NAMES[FP_RS_CTC1] = new Isn("ctc1");
		FP_RS_NAMES[FP_RS_BC1] = new Isn("bc1");
		FP_RS_NAMES[FP_RS_S] = new Isn("**fmts");
		FP_RS_NAMES[FP_RS_D] = new Isn("**fmtd");
		FP_RS_NAMES[FP_RS_W] = new Isn("**fmtw");

		CP_FN_NAMES[CP_FN_TLBINV] = new Isn("tlbinv");
		CP_FN_NAMES[CP_FN_TLBINVF] = new Isn("tlbinvf");
		CP_FN_NAMES[CP_FN_TLBP] = new Isn("tlbp");
		CP_FN_NAMES[CP_FN_TLBR] = new Isn("tlbr");
		CP_FN_NAMES[CP_FN_TLBWI] = new Isn("tlbwi");
		CP_FN_NAMES[CP_FN_TLBWR] = new Isn("tlbiwr");

		FP_FN_NAMES[FP_FN_ADD_D] = new Isn("add.{fpfmt}");
		FP_FN_NAMES[FP_FN_SUB_D] = new Isn("sub.{fpfmt}");
		FP_FN_NAMES[FP_FN_MUL_D] = new Isn("mul.{fpfmt}");
		FP_FN_NAMES[FP_FN_DIV_D] = new Isn("div.{fpfmt}");
		FP_FN_NAMES[FP_FN_MOV_D] = new Isn("mov.{fpfmt}");
		FP_FN_NAMES[FP_FN_NEG_D] = new Isn("neg.{fpfmt}");
		FP_FN_NAMES[FP_FN_CVT_S] = new Isn("cvts.{fpfmt}");
		FP_FN_NAMES[FP_FN_CVT_D] = new Isn("cvtd.{fpfmt}");
		FP_FN_NAMES[FP_FN_CVT_W] = new Isn("cvtw.{fpfmt}");
		FP_FN_NAMES[FP_FN_C_ULT] = new Isn("ult.{fpc}");
		FP_FN_NAMES[FP_FN_C_EQ] = new Isn("eq.{fptf}");
		FP_FN_NAMES[FP_FN_C_LT] = new Isn("lt.{fptf}");
		FP_FN_NAMES[FP_FN_C_LE] = new Isn("le.{fptf}");

		RT_NAMES[RT_BLTZ] = new Isn("bltz", Format.ZCONDBRA);
		RT_NAMES[RT_BGEZ] = new Isn("bgez", Format.ZCONDBRA);
		RT_NAMES[RT_BLTZAL] = new Isn("bltzal", Format.ZCONDBRA);
		RT_NAMES[RT_BGEZAL] = new Isn("bgezal", Format.ZCONDBRA);
	}
	
	private Mips () {
		//
	}

}
