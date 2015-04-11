package sys.test;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import sys.mips.*;
import static sys.mips.IsnSet.*;
import static sys.mips.MipsConstants.*;

/**
 * Mips assembler
 */
public class Asm {
	
	private static final Pattern ISNPAT = Pattern.compile("(\\w+(\\.[wsdl])?)");
	private static final Pattern LABELPAT = Pattern.compile("(\\w+):");
	private static final Pattern WORDPAT = Pattern.compile("(\\w+)");
	private static final Pattern HEXPAT = Pattern.compile("0x([0-9a-f]+)");
	private static final Pattern DECPAT = Pattern.compile("(-?[0-9]+)");
	private static final Pattern REGPAT = Pattern.compile("[$r]([0-9]+)");
	private static final Pattern FPREGPAT = Pattern.compile("\\$?f([0-9]+)");
	private static final String DELIM = " ,";
	private static final Map<String, Isn> isnNames = new TreeMap<>();
	private static final Map<String, Integer> regNames = new TreeMap<>();
	private static final Map<String, String> macroNames = new TreeMap<>();
	
	public static void main (String[] args) throws Exception {
		for (final Isn[] isns : Arrays.asList(SET.op, SET.fn, SET.fpu, SET.fpuFnSingle, SET.fpuFnDouble, SET.regimm, SET.system, SET.systemFn)) {
			for (final Isn isn : isns) {
				if (isn != null) {
					if (isnNames.put(isn.name, isn) != null) {
						throw new RuntimeException("duplicate isn name " + isn.name);
					}
				}
			}
		}
		System.out.println("isnNames=" + isnNames.keySet());
		for (int n = 0; n < Disasm.REG_NAMES.length; n++) {
			regNames.put(Disasm.REG_NAMES[n], n);
		}
		macroNames.put("b", "beq r0, r0, {1}");
		macroNames.put("bal", "bgezal r0, {1}");
		macroNames.put("bc1f", "bc1 0, {1}");
		macroNames.put("bc1t", "bc1 1, {1}");
		macroNames.put("nop", "sll r0, r0, 0");
		final Asm a = new Asm();
		try (BufferedReader br = new BufferedReader(new FileReader("test.asm"))) {
			a.run(br);
			a.print(System.out);
		} catch (final Exception e) {
			e.printStackTrace(System.out);
		}
	}
	
	private void print (PrintStream ps) {
		for (int n = 0; n < out.size(); n++) {
			final I i = out.get(n);
			ps.println(String.format("%08x # %s", i.getIsn(this, n * 4), i.line));
		}
	}
	
	public final Map<String,Integer> labelsMap = new TreeMap<>();
	private final List<I> out = new ArrayList<>();
	private StringTokenizer st;
	private String line;
	private int pos = 0;
	
	private void run(BufferedReader br) throws Exception {
		try {
			while ((line = br.readLine()) != null) {
				st = new StringTokenizer(line, DELIM);
				if (st.hasMoreTokens()) {
					//System.out.println(line);
					readLine();
					assertEnd();
				}
			}
		} catch (final Exception e) {
			System.out.println("line: " + line);
			throw e;
		}
	}
	
	
	private Token readToken () {
		if (!st.hasMoreTokens()) {
			return null;
		}
		final String t = st.nextToken();
		Matcher m;
		if ((m = ISNPAT.matcher(t)).matches()) {
			final Isn isn = isnNames.get(m.group(1));
			if (isn != null) {
				return new IsnToken(isn);
			}
		}
		if ((m = LABELPAT.matcher(t)).matches()) {
			return new LabelT(m.group(1));
		}
		if ((m = HEXPAT.matcher(t)).matches()) {
			return new Num(Integer.parseInt(m.group(1), 16));
		}
		if ((m = DECPAT.matcher(t)).matches()) {
			return new Num(Integer.parseInt(m.group(1)));
		}
		if ((m = REGPAT.matcher(t)).matches()) {
			return new Reg(Integer.parseInt(m.group(1)));
		}
		if ((m = FPREGPAT.matcher(t)).matches()) {
			return new FReg(Integer.parseInt(m.group(1)));
		}
		if ((m = WORDPAT.matcher(t)).matches()) {
			final Integer i = regNames.get(t);
			if (i != null) {
				return new Reg(i.intValue());
			}
			final String macro = macroNames.get(t);
			if (macro != null) {
				eval(macro);
				return readToken();
			}
			final Integer label = labelsMap.get(t);
			if (label != null) {
				return new Address(label);
			} else {
				return new Ref(t);
			}
		}
		throw new RuntimeException("unrecognised token " + t);
	}
	
	private void eval (String macro) {
		for (int n = 1; st.hasMoreTokens(); n++) {
			macro = macro.replace("{" + n + "}", st.nextToken());
		}
		line = line + " => " + macro;
		st = new StringTokenizer(macro, DELIM);
	}
	
	private void readLine () {
		final Token t = readToken();
		if (t instanceof IsnToken) {
			final IsnToken it = (IsnToken)t;
			final I i;
			switch (it.isn.type) {
				case T_R3:
					i = genR3(it.isn);
					break;
				case T_FR2:
					i = genFR2(it.isn);
					break;
				case T_FR3:
					i = genFR3(it.isn);
					break;
				case T_IB2:
					i = genImm2(it.isn, true);
					break;
				case T_I2_N:
					i = genImm2N(it.isn);
					break;
				case T_I3:
					i = genImm3(it.isn, false);
					break;
				case T_IB3:
					i = genImm3(it.isn, true);
					break;
				case T_S:
					i = genSyscall(it.isn);
					break;
				default:
					throw new RuntimeException("unknown isn type " + it.isn);
			}
			i.line = line;
			push(i);
			
		} else if (t instanceof LabelT) {
			final LabelT lt = (LabelT) t;
			if (labelsMap.put(lt.name, pos) != null) {
				throw new RuntimeException("redefinition of " + lt.name);
			}
			
		} else {
			throw new RuntimeException("unexpected token " + t);
		}
	}
	
	private I genSyscall (Isn isn) {
		final Num syscall = next(Num.class);
		final int i = (isn.op << 26) | (syscall.value << 6) | isn.fn;
		return new CI(i);
	}

	private <X extends Token> X next (Class<X> tc) {
		final Token t = readToken();
		if (t != null && tc.isAssignableFrom(t.getClass())) {
			return (X) t;
		} else {
			throw new RuntimeException("expected: " + tc.getSimpleName() + " actual: " + (t != null ? t.getClass().getSimpleName() : null));
		}
	}
	
	private void assertEnd () {
		final Token t = readToken();
		if (t != null) {
			throw new RuntimeException("expected: null actual: " + t);
		}
	}
	
	private I genImm2N (Isn isn) {
		final Num n = next(Num.class);
		final Value imm = next(Value.class);
		return new I() {
			@Override
			int getIsn (Asm asm, int addr) {
				int value = (imm.getValue(asm, addr) - addr - 4) >>> 2;
				return (isn.op << 26) | (isn.rs << 21) | (n.value << 16) | (value & 0xffff);
			}
		};
	}
	
	private I genImm2 (Isn isn, boolean branch) {
		final Reg rs = next(Reg.class);
		final Value imm = next(Value.class);
		return new I() {
			@Override
			int getIsn (Asm asm, int addr) {
				int value = imm.getValue(asm, addr);
				if (branch) {
					value = (value - addr - 4) >>> 2;
				}
				return (isn.op << 26) | (rs.reg << 21) | (isn.rt << 16) | (value & 0xffff);
			}
		};
	}
	
	private I genImm3 (Isn isn, boolean branch) {
		final Reg rt = next(Reg.class);
		final Reg rs = next(Reg.class);
		final Value imm = next(Value.class);
		return new I() {
			@Override
			int getIsn (Asm asm, int addr) {
				int value = imm.getValue(asm, addr);
				if (branch) {
					value = (value - addr + 4) >>> 2;
				}
				return (isn.op << 26) | (rs.reg << 21) | (rt.reg << 16) | (value & 0xffff);
			}
		};
	}
	
	private void assertImm(int value) {
		if (value < Short.MIN_VALUE || value > Short.MAX_VALUE * 2) {
			throw new RuntimeException("imm out of range");
		}
	}
	
	private void assertAligned (Isn isn, FReg... regs) {
		if (isn.rs == FP_RS_D || isn.rs == FP_RS_L) {
			for (final FReg reg : regs) {
				if ((reg.reg & 1) != 0) {
					throw new RuntimeException("unaligned fp register");
				}
			}
		}
	}
	
	private I genFR3 (Isn isn) {
		final FReg fd = next(FReg.class);
		final FReg fs = next(FReg.class);
		final FReg ft = next(FReg.class);
		assertAligned(isn, fd, fs, ft);
		final int i = (isn.op << 26) | (isn.rs << 21) | (ft.reg << 16) | (fs.reg << 11) | (fd.reg << 6) | isn.fn;
		return new CI(i);
	}
	
	private I genFR2 (Isn isn) {
		final FReg fd = next(FReg.class);
		final FReg fs = next(FReg.class);
		assertAligned(isn, fd, fs);
		final int i = (isn.op << 26) | (isn.rs << 21) | (fs.reg << 11) | (fd.reg << 6) | isn.fn;
		return new CI(i);
	}
	
	private I genR3 (Isn isn) {
		final Reg rd = next(Reg.class);
		final Reg rs = next(Reg.class);
		final Reg rt = next(Reg.class);
		final int i = (isn.op << 26) | (rs.reg << 21) | (rt.reg << 16) | (rd.reg << 11) | isn.fn;
		return new CI(i);
	}
	
	private void push (I i) {
		out.add(i);
		pos += 4;
	}
}

abstract class I {
	public String line;
	public I () {
		//
	}
	abstract int getIsn(Asm asm, int addr);
}
class CI extends I {
	public int isn;
	public CI (int isn) {
		this.isn = isn;
	}
	@Override
	int getIsn (Asm asm, int addr) {
		return isn;
	}
}
abstract class Token {
	//
}
class IsnToken extends Token {
	public Isn isn;
	
	public IsnToken (Isn isn) {
		this.isn = isn;
	}
}

class LabelT extends Token {
	public String name;
	public LabelT (String name) {
		this.name = name;
	}
}
class Num extends Value {
	public int value;
	public Num (int value) {
		this.value = value;
	}
	@Override
	int getValue (Asm asm, int addr) {
		return value;
	}
}
class Reg extends Token {
	public int reg;
	public Reg (int reg) {
		if (reg >= 32) {
			throw new RuntimeException("invalid register " + reg);
		}
		this.reg = reg;
	}
}
class FReg extends Token {
	public int reg;
	public FReg (int freg) {
		if (freg >= 32) {
			throw new RuntimeException("invalid fp register " + freg);
		}
		this.reg = freg;
	}
}
class Ref extends Value {
	public String name;
	public Ref (String name) {
		this.name = name;
	}
	@Override
	boolean hasValue (Asm asm) {
		return asm.labelsMap.containsKey(name);
	}
	@Override
	int getValue (Asm asm, int addr) {
		final Integer value = asm.labelsMap.get(name);
		if (value != null) {
			return value.intValue();
		} else {
			throw new RuntimeException("could not get value of " + name);
		}
	}
	@Override
	public String toString () {
		return name;
	}
}
class Address extends Value {
	public int value;
	public Address (int value) {
		this.value = value;
	}
	@Override
	int getValue (Asm asm, int addr) {
		return value;
	}
}
abstract class Value extends Token {
	boolean hasValue(Asm asm) {
		return true;
	}
	abstract int getValue(Asm asm, int addr);
}
