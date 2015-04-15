package sys.test;

import java.io.*;

public class Test3 {
	public static void main (String[] args) throws Exception {
		double a = Math.PI, b = -Math.E, c = Math.sqrt(2), d = -Math.log(2);
		int t = 100;
		
		// TODO float/double
		// rounding modes
		// conditions
		
		try (PrintWriter pw = new PrintWriter(new FileWriter("xsrc/gen1.c"))) {
			pw.println("#define GEN1() gen1(" + a + ", " + b + ", " + c + ", " + d + ")");
			pw.println("__attribute__((noinline)) void gen1(float a, float b, float c, float d) {");
			pw.println("  " + ftest(t++, "abs.s %0, %1", "'f' (a)", Math.abs(a)));
			pw.println("  " + ftest(t++, "add.s %0, %1, %2", "'f' (a), 'f' (b)", a + b));
			pw.println("  " + ftest(t++, "div.s %0, %1, %2", "'f' (a), 'f' (b)", a / b));
			pw.println("  " + ftest(t++, "madd.s %0, %1, %2, %3", "'f' (a), 'f' (b), 'f' (c)", (b*c) + a));
			// msub
			pw.println("  " + ftest(t++, "mul.s %0, %1, %2", "'f' (a), 'f' (b)", a*b));
			pw.println("  " + ftest(t++, "neg.s %0, %1", "'f' (a)", -a));
			// nmadd
			// nmsub
			pw.println("  " + ftest(t++, "recip.s %0, %1", "'f' (a)", 1/a));
			pw.println("  " + ftest(t++, "rsqrt.s %0, %1", "'f' (a)", 1/Math.sqrt(a)));
			pw.println("  " + ftest(t++, "sqrt.s %0, %1", "'f' (a)", Math.sqrt(a)));
			pw.println("  " + ftest(t++, "sub.s %0, %1, %2", "'f' (a), 'f' (b)", a-b));
			pw.println("}");
		}
		
		System.out.println("done");
	}
	
	private static String ftest (int testId, String asm, String in, double value) {
		// asm("add.s %0, %1, %2" : "=f" (r) : "f" (x), "f" (y));
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append(" float r;");
		sb.append(" " + asm(asm, "'=f' (r)", in));
		sb.append(" assertdeq(" + testId + ", r, " + value + ");");
		sb.append(" }");
		return sb.toString().replace("'", "\"");
	}
	
	private static String asm (String asm, String in, String out) {
		return ("asm('" + asm + "': " + in + ": " + out + ");").replace("'", "\"");
	}
}
