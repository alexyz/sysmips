package sys.mips;

/** external interrupt */
public class Exn {
	public final int type;

	public Exn (int type) {
		this.type = type;
	}
}
