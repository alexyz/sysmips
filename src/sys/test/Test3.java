package sys.test;

import static java.lang.Math.*;

import java.io.*;

public class Test3 {
	public static void main (String[] args) throws Exception {
		// should gen this as well
		double a = PI, b = -E, c = sqrt(2), d = -log(2);
		
		try (PrintWriter pw = new PrintWriter(new FileWriter("xsrc/gen1.c"))) {
			pw.println("{");
			pw.println("  double a=" + a + ", b=" + b + ", c=" + c + ", d=" + d + ";");
			pw.println("  gen1(a,b,c,d);");
			pw.println("}");
		}
		
		try (PrintWriter pw = new PrintWriter(new FileWriter("xsrc/gen1def.c"))) {
			pw.println("__attribute__((noinline)) void gen1(float a, float b, float c, float d) {");
			pw.println("  " + testString(1, abs(a), "abs.s %0, %1", "a"));
			pw.println("  " + testString(2, a + b, "add.s %0, %1, %2", "a", "b"));
			pw.println("}");
		}
		
		System.out.println("done");
	}
	
	private static String testString (int testId, double value, String asm, String... args) {
		// asm("add.s %0, %1, %2" : "=f" (r) : "f" (x), "f" (y));
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append(" double r;");
		sb.append(" asm('" + asm + "':");
		sb.append(" '=f' (r):");
		for (int a = 0; a < args.length; a++) {
			if (a > 0) {
				sb.append(",");
			}
			sb.append(" 'f' (" + args[a] + ")");
		}
		sb.append(");");
		sb.append(" assertdeq(" + testId + ", r, " + value + ");");
		sb.append(" }");
		return sb.toString().replace("'", "\"");
	}
}
