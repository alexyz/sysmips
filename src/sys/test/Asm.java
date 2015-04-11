package sys.test;

import java.io.*;
import java.util.*;

import sys.mips.Isn;
import sys.mips.MipsConstants;

public class Asm {
	
	static final List<I> isns = new ArrayList<>();
	
	public static void main (String[] args) throws Exception {
		try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
			String line;
			while ((line = br.readLine()) != null) {
				StringTokenizer t = new StringTokenizer(line, " ,");
				List<String> list = new LinkedList<>();
				if (t.hasMoreTokens()) {
					read(t);
				}
			}
		}
	}
	
	private static void read(StringTokenizer st) {
		String t = st.nextToken();
		switch (t) {
			case "add": readAdd(MipsConstants.OP_SPECIAL, MipsConstants.FN_ADDU, st); return;
			case "addi": readAddImm(MipsConstants.OP_ADDI, st); return;
			case "addiu": 
		}
		
	}
	
	private static void readAddImm (int op, StringTokenizer st) {
		int rs = reg(st);
		int rt = reg(st);
		short imm = imm(st);
		int isn = (op << 26) | (rs << 21) | (rt << 16) | imm;
		isns.add(new CI(isn));
	}
	
	private static void readAdd (int op, int fn, StringTokenizer st) {
		int rd = reg(st);
		int rs = reg(st);
		int rt = reg(st);
		int isn = (op << 26) | (rs << 21) | (rt << 16) | (rd << 11) | fn;
		isns.add(new CI(isn));
	}

	private static int reg (StringTokenizer st) {
		String t = st.nextToken();
		return Integer.parseInt(t.substring(1));
	}
	
	private static short imm (StringTokenizer st) {
		String t = st.nextToken();
		return Short.parseShort(t);
	}
}

abstract class I {
	public int a;
}

class CI extends I {
	public final int isn;
	public CI (int isn) {
		this.isn = isn;
	}
}
