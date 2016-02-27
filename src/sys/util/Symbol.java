package sys.util;

public class Symbol {
	public final String name;
	public final int size;
	public final int addr;
	
	public Symbol (int addr, String name, int size) {
		this.addr = addr;
		this.name = name;
		this.size = size;
	}
	
	@Override
	public String toString () {
		return name;
	}

}
