package sys.util;

import java.util.HashMap;

public class Log {
	
	private static final HashMap<String,String> CACHE = new HashMap<>();
	
	private static String cache(String s) {
		synchronized (CACHE) {
			String t = CACHE.get(s);
			if (t != null) {
				return t;
			} else {
				CACHE.put(s, s);
				return s;
			}
		}
	}
	
	public final long cycle;
	public final String name;
	public final String msg;
	public final boolean km;
	public final boolean ie;
	public final boolean ex;
	public final String sym;
	public Log(String name, String msg) {
		this(0, false, false, false, name, msg, null);
	}
	public Log(long cycle, boolean km, boolean ie, boolean ex, String name, String msg, String sym) {
		this.cycle = cycle;
		this.name = cache(name);
		this.msg = cache(msg);
		this.km = km;
		this.ie = ie;
		this.ex = ex;
		this.sym = cache(sym);
	}
	@Override
	public String toString () {
		return "[" + cycle + ":" + (km?"k":"") + (ie?"i":"") + (ex?"x":"") + ":" + name + ":" + sym + "] " + msg;
	}
}
