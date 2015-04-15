package sys.test;

import java.io.*;

public class Test3 {
	public static void main (String[] args) throws Exception {
		double a = Math.PI, b = -Math.E, c = Math.sqrt(2), d = -Math.log(2);
		
		// TODO float/double
		
		try (PrintWriter pw = new PrintWriter(new FileWriter("xsrc/gen1.c"))) {
			pw.println("#define GEN1 gen1(" + a + ", " + b + ", " + c + ", " + d + ")");
			pw.println("__attribute__((noinline)) void gen1(float a, float b, float c, float d) {");
			pw.println("  " + ftest(1, "abs.s %0, %1", "'f' (a)", Math.abs(a)));
			pw.println("  " + ftest(2, "add.s %0, %1, %2", "'f' (a), 'f' (b)", a + b));
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
