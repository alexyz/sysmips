package sys.mips;

import static sys.mips.MipsConstants.*;
import static sys.mips.IsnSet.*;

public final class Disasm {
	
	/**
	 * gas names of general registers
	 */
	public static final String[] REG_NAMES = new String[] { 
		"zero", "at", "v0", "v1", 
		"a0", "a1", "a2", "a3", 
		"t0", "t1", "t2", "t3", 
		"t4", "t5", "t6", "t7",
		"s0", "s1", "s2", "s3", 
		"s4", "s5", "s6", "s7", 
		"t8", "t9", "k0", "k1", 
		"gp", "sp", "s8", "ra", 
		"hi", "lo" 
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
	
	private static final Isn NOP = new Isn("nop");
	
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
		
		final Isn isnObj;
		
		switch (op) {
			case OP_SPECIAL:
				if (fn == FN_SLL && rd == 0) {
					isnObj = NOP;
				} else {
					isnObj = SET.fn[fn];
				}
				break;
			case OP_REGIMM:
				isnObj = SET.regimm[rt];
				break;
			case OP_COP0:
				if (rs < CP_RS_CO) {
					isnObj = SET.system[rs];
				} else {
					isnObj = SET.systemFn[fn];
				}
				break;
			case OP_COP1:
				if (rs < FP_RS_S) {
					isnObj = SET.fpu[rs];
				} else {
					// XXX not always s...
					isnObj = SET.fpuFnSingle[fn];
				}
				break;
			case OP_SPECIAL2:
				isnObj = SET.fn2[fn];
				break;
			default:
				isnObj = SET.op[op];
		}
		
		final String addr = syms.getName(pc);
		final String isnValue;
		if (isnObj != null) {
			isnValue = formatIsn(isnObj, isn, cpu);
		} else {
			isnValue = "op=" + op + " rt=" + rt + " rs=" + rs + " fn=" + fn;
		}
		return String.format("%-40s %08x %s", addr, isn, isnValue);
	}
	
	private static String gpRegString (final Cpu cpu) {
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
	
	private static String cpRegString (Cpu cpu) {
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
	
	private static String formatIsn (final Isn isnObj, final int isn, final Cpu cpu) {
		final StringBuilder sb = new StringBuilder(isnObj.name);
		while (sb.length() < 8) {
			sb.append(" ");
		}
		sb.append(isnObj.disasm);
		int i;
		while ((i = sb.indexOf("{")) >= 0) {
			final int j = sb.indexOf("}", i);
			if (j > i) {
				sb.replace(i, j + 1, String.valueOf(eval(sb.substring(i + 1, j), isn, cpu)));
			} else {
				throw new RuntimeException("invalid format " + isnObj.disasm);
			}
		}
		return sb.toString();
	}
	
	private static String gpRegName (int reg) {
		// return "$" + reg + ":" + REG_NAMES[reg];
		return REG_NAMES[reg];
	}
	
	private static String eval (final String name, final int isn, final Cpu cpu) {
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
			case "membaseoffset": {
				Integer w = mem.loadWordSafe((reg[base(isn)] + simm(isn)));
				return w != null ? "0x" + Integer.toHexString(w) : null;
			}
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
	
	private Disasm () {
		//
	}
	
}
