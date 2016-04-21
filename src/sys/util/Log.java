package sys.util;

public class Log {
	public final long cycle;
	public final String name;
	public final String msg;
	public final boolean km;
	public final boolean ie;
	public final boolean ex;
	public final String[] calls;
	public Log(String name, String msg) {
		this(0, false, false, false, name, msg, null);
	}
	public Log(long cycle, boolean km, boolean ie, boolean ex, String name, String msg, String[] calls) {
		this.cycle = cycle;
		this.name = name;
		this.msg = msg;
		this.km = km;
		this.ie = ie;
		this.ex = ex;
		this.calls = calls;
	}
	@Override
	public String toString () {
		return "[" + cycle + ":" + (km?"k":"") + (ie?"i":"") + (ex?"x":"") + ":" + name + "] " + msg;
	}
}
