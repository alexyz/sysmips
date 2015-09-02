package sys.util;

class Symbol {
	public final String name;
	public final int size;
	
	public Symbol (String name, int size) {
		this.name = name;
		this.size = size;
	}
	
	@Override
	public String toString () {
		return name;
	}

}
