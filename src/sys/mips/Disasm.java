package sys.mips;

import static sys.mips.MipsConstants.*;
import static sys.mips.Access.*;

public final class Disasm {
	
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
		final int fn = fn(isn);
		
		Isn isnObj = IsnSet.getIsn(isn);
		final String isnValue;
		if (isnObj != null) {
			isnValue = formatIsn(isnObj, isn, cpu);
		} else {
			isnValue = "op=" + op + " rt=" + rt + " rs=" + rs + " fn=" + fn;
		}
		final String addr = syms.getName(pc);
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
		sb.append(isnObj.format);
		int i;
		while ((i = sb.indexOf("{")) >= 0) {
			final int j = sb.indexOf("}", i);
			if (j > i) {
				sb.replace(i, j + 1, String.valueOf(eval(sb.substring(i + 1, j), isn, cpu)));
			} else {
				throw new RuntimeException("invalid format " + isnObj.format);
			}
		}
		return sb.toString();
	}
	
	private static String gpRegName (int reg) {
		return REG_NAMES[reg];
	}
	
	private static String fpRegName (int reg) {
		return "f" + reg;
	}
	
	private static String eval (final String name, final int isn, final Cpu cpu) {
		final Memory mem = cpu.getMemory();
		final int[] reg = cpu.getRegisters();
		final int pc = cpu.getPc();
		final Symbols syms = mem.getSymbols();
		fmt(isn);
		switch (name) {
			case "ft":
				return fpRegName(ft(isn));
			case "fd":
				return fpRegName(fd(isn));
			case "fs":
				return fpRegName(fs(isn));
			case "regft":
				return "" + cpu.getFPRegister(ft(isn), access(fmt(isn)));
			case "regfs":
				return "" + cpu.getFPRegister(fs(isn), access(fmt(isn)));
			case "regfd":
				return "" + cpu.getFPRegister(fd(isn), access(fmt(isn)));
			case "regfts":
				return "" + cpu.getFPRegister(ft(isn), Access.SINGLE);
			case "regftd":
				return "" + cpu.getFPRegister(ft(isn), Access.DOUBLE);
			case "regfsx": {
				int v = cpu.getFPRegister(fs(isn));
				return "0x" + Integer.toHexString(v) + "(" + Float.intBitsToFloat(v) + ")";
			}
			case "rs":
				return gpRegName(rs(isn));
			case "rd":
				return gpRegName(rd(isn));
			case "rt":
				return gpRegName(rt(isn));
			case "regrtx": {
				int v = reg[rt(isn)];
				return "0x" + Integer.toHexString(v) + "(" + Float.intBitsToFloat(v) + ")";
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
				return "" + simm(isn);
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
			case "membaseoffsets": {
				Integer w = mem.loadWordSafe((reg[base(isn)] + simm(isn)));
				if (w != null) {
					return String.format("%g", Float.intBitsToFloat(w));
				}
				return null;
			}
			case "membaseoffsetd": {
				Long dw = mem.loadDoubleWordSafe((reg[base(isn)] + simm(isn)));
				if (dw != null) {
					return String.format("%g", Double.longBitsToDouble(dw));
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
				// FIXME
				return "?";
			default:
				throw new RuntimeException("unknown name " + name);
		}
	}
	
	private Disasm () {
		//
	}
	
}
