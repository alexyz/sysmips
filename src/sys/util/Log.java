package sys.util;

public class Log {
	public final long cycle;
	public final String name;
	public final String msg;
	public final boolean km;
	public final boolean ie;
	public final boolean ex;
	public Log(String name, String msg) {
		this(0, false, false, false, name, msg);
	}
	public Log(long cycle, boolean km, boolean ie, boolean ex, String name, String msg) {
		this.cycle = cycle;
		this.name = name;
		this.msg = msg;
		this.km = km;
		this.ie = ie;
		this.ex = ex;
	}
	@Override
	public String toString () {
		return "[" + cycle + ":" + (km?"k":"") + (ie?"i":"") + (ex?"x":"") + ":" + name + "] " + msg;
	}
}
