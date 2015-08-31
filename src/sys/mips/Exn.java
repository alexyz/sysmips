package sys.mips;

/** external interrupt */
public class Exn {
	public final int in;
	public final int type;

	public Exn (int extype, int interrupt) {
		this.in = interrupt;
		this.type = extype;
	}
}
