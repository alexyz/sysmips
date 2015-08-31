package sys.mips;

import static sys.mips.MipsConstants.*;
import static sys.mips.Decoder.*;

/**
 * disassembly and Isn utils
 */
public class IsnUtil {

	private static final Isn NOP = new Isn("nop");

	/** names of general registers */
	public static final String[] REG_NAMES = new String[] { 
		"zero", "at", "v0", "v1", 
		"a0", "a1", "a2", "a3",
		// 8
		"t0", "t1", "t2", "t3", 
		"t4", "t5", "t6", "t7",
		// 16
		"s0", "s1", "s2", "s3", 
		"s4", "s5", "s6", "s7", 
		// 24
		"t8", "t9", "k0", "k1", 
		"gp", "sp", "s8", "ra"
	};

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
	
	//
	// methods
	//

	public static String opString (final int op) {
		return "hex=" + Integer.toHexString(op) + " dec=" + op + " oct=" + Integer.toOctalString(op);
	}
	
	/** convert rs meta instruction to d, s, w, or l */
	public static String fpFormatString (final int rs) {
		switch (rs) {
			case FP_RS_D:
				return "d";
			case FP_RS_S:
				return "s";
			case FP_RS_W:
				return "w";
			case FP_RS_L:
				return "l";
			default:
				throw new RuntimeException("unknown fp format " + rs);
		}
	}

	public static String cpRegName (int rd, int sel) {
		String name = "unknown";
		if (rd < CP_REG_NAMES.length) {
			final String[] rdnames = CP_REG_NAMES[rd];
			if (rdnames != null && sel < rdnames.length) {
				name = rdnames[sel];
			}
		}
		return rd + ", " + sel + ": " + name;
	}
	
	public static String gpRegName (int reg) {
		return REG_NAMES[reg];
	}
	
	public static String fpRegName (int reg) {
		return "f" + reg;
	}
	
	/**
	 * Disassemble an instruction
	 */
	public static String isnString (final Cpu cpu, final int isn) {
		final Memory mem = cpu.getMemory();
		//final Symbols syms = mem.getSymbols();
		//final int pc = cpu.getPc();
		final int op = op(isn);
		final int rs = rs(isn);
		final int rt = rt(isn);
		final int fn = fn(isn);
		final int rd = rd(isn);
		
		Isn isnObj;
		if (op == OP_SPECIAL && fn == FN_SLL && rd == 0) {
			isnObj = NOP;
		} else {
			isnObj = IsnSet.INSTANCE.getIsn(isn);
		}
		
		final String isnValue;
		if (isnObj != null) {
			isnValue = IsnUtil.formatIsn(isnObj, cpu, isn);
		} else {
			isnValue = "op=" + op + " rt=" + rt + " rs=" + rs + " fn=" + fn;
		}
		
		//final String addr = syms.getName(pc);
		//return String.format("%-40s %08x %s", addr, isn, isnValue);
		return String.format("%08x %s", isn, isnValue);
	}
	
	public static String formatIsn (final Isn isnObj, final Cpu cpu, final int isn) {
		final StringBuilder sb = new StringBuilder(isnObj.name);
		while (sb.length() < 8) {
			sb.append(" ");
		}
		sb.append(isnObj.format);
		int i;
		while ((i = sb.indexOf("{")) >= 0) {
			final int j = sb.indexOf("}", i);
			if (j > i) {
				sb.replace(i, j + 1, String.valueOf(formatCode(sb.substring(i + 1, j), cpu, isn)));
			} else {
				throw new RuntimeException("invalid format " + isnObj.format);
			}
		}
		return sb.toString();
	}
	
	/** get value of format code */
	private static String formatCode (final String name, final Cpu cpu, final int isn) {
		final Memory mem = cpu.getMemory();
		final int[] reg = cpu.getRegisters();
		final int pc = cpu.getPc();
		final Symbols syms = mem.getSymbols();
		
		switch (name) {
			case "fr":
				return fpRegName(fr(isn));
			case "ft":
				return fpRegName(ft(isn));
			case "fd":
				return fpRegName(fd(isn));
			case "fs":
				return fpRegName(fs(isn));
			case "regft":
				return formatDouble(cpu.getFpRegister(ft(isn), FpFormat.getInstance(fmt(isn))));
			case "regfs":
				return formatDouble(cpu.getFpRegister(fs(isn), FpFormat.getInstance(fmt(isn))));
			case "regfss":
				return formatDouble(cpu.getFpRegister(fs(isn), FpFormat.SINGLE));
			case "regfrs":
				return formatDouble(cpu.getFpRegister(fr(isn), FpFormat.SINGLE));
			case "regfd":
				return formatDouble(cpu.getFpRegister(fd(isn), FpFormat.getInstance(fmt(isn))));
			case "regfts":
				return formatDouble(cpu.getFpRegister(ft(isn), FpFormat.SINGLE));
			case "regftd":
				return formatDouble(cpu.getFpRegister(ft(isn), FpFormat.DOUBLE));
			case "regfsx": {
				int v = cpu.getFpRegister(fs(isn));
				return "0x" + Integer.toHexString(v) + "(" + formatDouble(Float.intBitsToFloat(v)) + ")";
			}
			case "rs":
				return gpRegName(rs(isn));
			case "rd":
				return gpRegName(rd(isn));
			case "rt":
				return gpRegName(rt(isn));
			case "regrtx": {
				int v = reg[rt(isn)];
				return "0x" + Integer.toHexString(v) + "(" + formatDouble(Float.intBitsToFloat(v)) + ")";
			}
			case "base":
				return gpRegName(base(isn));
			case "regrd":
				return syms.getName(reg[rd(isn)]);
			case "regrt":
				return syms.getName(reg[rt(isn)]);
			case "regrs":
				return syms.getName(reg[rs(isn)]);
			case "offset":
				return Integer.toString(simm(isn));
			case "cprd":
				return cpRegName(rd(isn), sel(isn));
			case "imm":
				return String.valueOf(simm(isn));
			case "branch":
				return syms.getName(branch(isn, pc));
			case "hi":
				return "0x" + Integer.toHexString(cpu.getRegHi());
			case "lo":
				return "0x" + Integer.toHexString(cpu.getRegLo());
			case "baseoffset":
				return syms.getName(reg[base(isn)] + simm(isn));
			case "syscall":
				return "0x" + Integer.toHexString(syscall(isn));
			case "membaseoffset": {
				Integer w = mem.loadWordSafe((reg[base(isn)] + simm(isn)));
				return w != null ? "0x" + Integer.toHexString(w) : null;
			}
			case "membaseoffsets": {
				Integer w = mem.loadWordSafe((reg[base(isn)] + simm(isn)));
				if (w != null) {
					return formatDouble(Float.intBitsToFloat(w));
				}
				return null;
			}
			case "membaseoffsetd": {
				Long dw = mem.loadDoubleWordSafe((reg[base(isn)] + simm(isn)));
				if (dw != null) {
					return formatDouble(Double.longBitsToDouble(dw));
				}
				return null;
			}
			case "jump":
				return syms.getName(jump(isn, pc));
			case "sa":
				return String.valueOf(sa(isn));
			case "fptf":
				return fptf(isn) ? "t" : "f";
			case "fpcc":
				return "cc" + fpcc(isn);
			case "regfpcc":
				return String.valueOf(fccrFcc(cpu.getFpControlReg(), fpcc(isn)));
			default:
				throw new RuntimeException("unknown name " + name);
		}
	}
	
	private static String formatDouble(double d) {
		return String.format("%.6f", d);
	}
	
	public static String exceptionName (int ex) {
		switch (ex) {
			case EX_INT: return "Interrupt";
			case EX_MOD: return "TLB modification";
			case EX_TLBL: return "TLB (load/ifetch)";
			case EX_TLBS: return "TLB  (store)";
			case EX_AdEL: return "Address error (load/ifetch)";
			case EX_AdES: return "Address error (store)";
			case EX_IBE: return "Bus error (ifetch)";
			case EX_DBE: return "Bus error (load/store)";
			case EX_Sys: return "Syscall";
			case EX_Bp: return "Breakpoint";
			case EX_RI: return "Reserved instruction";
			case EX_CpU: return "Coprocessor unusable";
			case EX_Ov: return "Integer overflow";
			case EX_Tr: return "Trap";
			case EX_WATCH: return "Watch";
			case EX_MCheck: return "Machine check";
			default: return "Reserved " + ex;
		}
	}
}
