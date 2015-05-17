package sys;

public class ByteOrder {

	public static int swap (final int x) {
		// abcd -> dcba
		return ((x & 0xff) << 24) | ((x & 0xff00) << 8) | ((x & 0xff0000) >>> 8) | ((x & 0xff000000) >>> 24);
	}

	public static short swap (final short s) {
		// ab -> ba
		return (short) (((s & 0xff) << 8) | ((s & 0xff00) >>> 8));
	}
	
}
