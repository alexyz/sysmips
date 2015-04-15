package sys.mips;

import static sys.mips.MipsConstants.*;

public final class Disasm {
	
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
		final int fn = fn(isn);
		final int rd = rd(isn);
		
		Isn isnObj;
		if (fn == FN_SLL && rd == 0) {
			isnObj = NOP;
		} else {
			isnObj = IsnSet.getIsn(isn);
		}
		
		final String isnValue;
		if (isnObj != null) {
			isnValue = IsnUtil.formatIsn(isnObj, cpu, isn);
		} else {
			isnValue = "op=" + op + " rt=" + rt + " rs=" + rs + " fn=" + fn;
		}
		
		final String addr = syms.getName(pc);
		return String.format("%-40s %08x %s", addr, isn, isnValue);
	}
	
	private Disasm () {
		//
	}
	
}
