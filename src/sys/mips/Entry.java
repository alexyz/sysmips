package sys.mips;

/** 
 * joint tlb entry
 */
public class Entry {
	
	public static class Data {
		public int physicalFrameNumber;
		public boolean dirty;
		public boolean valid;
		@Override
		public String toString () {
			return String.format("Data[pfn=%x %s %s]", physicalFrameNumber, dirty ? "dirty" : "clean", valid ? "valid" : "invalid");
		}
	}
	
	public final Data[] data = new Data[2];
	
	public int pageMask;
	public int virtualPageNumber2;
	public int addressSpaceId;
	public boolean global;
	
	public Entry () {
		for (int n = 0; n < data.length; n++) {
			data[n] = new Data();
		}
	}
	
	@Override
	public String toString () {
		return String.format("Entry[%s vpn2=%x even=%s odd=%s]",
				global ? "global" : "asid=" + Integer.toHexString(addressSpaceId),
				virtualPageNumber2,
				data[0], data[1]);
	}
}

