package sys.test;

import java.io.*;
import java.util.*;

import sys.mips.*;
import static sys.mips.IsnSet.*;

public class Asm {
	
	static Map<String, Isn> map = new TreeMap<>();
	static final List<I> isns = new ArrayList<>();
	
	public static void main (String[] args) throws Exception {
		
		for (Isn isn : SET.op) {
			map.put(isn.name, isn);
		}
		for (Isn isn : SET.fn) {
			map.put(isn.name, isn);
		}
		
		try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
			String line;
			while ((line = br.readLine()) != null) {
				StringTokenizer t = new StringTokenizer(line, " ,");
				if (t.hasMoreTokens()) {
					read(t);
				}
			}
		}
	}
	
	private static void read(StringTokenizer st) {
		String t = st.nextToken();
		Isn isn = map.get(t);
		if (isn != null) {
			switch (isn.asm) {
				case F_R:
					
			}
			// switch on asm format
		} else {
			// it's a label
		}
	}
	
	private static void readAddImm (int op, StringTokenizer st) {
		int rs = reg(st);
		int rt = reg(st);
		short imm = imm(st);
		int isn = (op << 26) | (rs << 21) | (rt << 16) | imm;
		isns.add(new CI(isn));
	}
	
	private static void genR (Isn isnObj, StringTokenizer st) {
		int rd = reg(st);
		int rs = reg(st);
		int rt = reg(st);
		int isn = (isnObj.op << 26) | (rs << 21) | (rt << 16) | (rd << 11) | isnObj.fn;
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
