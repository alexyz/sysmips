package sys.mips;

import static sys.mips.MipsConstants.*;

public class IsnUtil {

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
	private static Object formatCode (final String name, final Cpu cpu, final int isn) {
		final Memory mem = cpu.getMemory();
		final int[] reg = cpu.getRegisters();
		final int pc = cpu.getPc();
		final Symbols syms = mem.getSymbols();
		fmt(isn);
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
				return formatDouble(cpu.getFpRegister(ft(isn), FpAccess.access(fmt(isn))));
			case "regfs":
				return formatDouble(cpu.getFpRegister(fs(isn), FpAccess.access(fmt(isn))));
			case "regfss":
				return formatDouble(cpu.getFpRegister(fs(isn), FpAccess.SINGLE));
			case "regfrs":
				return formatDouble(cpu.getFpRegister(fr(isn), FpAccess.SINGLE));
			case "regfd":
				return formatDouble(cpu.getFpRegister(fd(isn), FpAccess.access(fmt(isn))));
			case "regfts":
				return formatDouble(cpu.getFpRegister(ft(isn), FpAccess.SINGLE));
			case "regftd":
				return formatDouble(cpu.getFpRegister(ft(isn), FpAccess.DOUBLE));
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
				return simm(isn);
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
				return cpu.getFpCondition(fpcc(isn));
			default:
				throw new RuntimeException("unknown name " + name);
		}
	}
	
	private static String formatDouble(double d) {
		return String.format("%.6f", d);
	}
}
