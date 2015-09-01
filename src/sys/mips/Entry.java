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
			return String.format("Data[pfn=%x d=%x v=%x]", physicalFrameNumber, dirty ? 1 : 0, valid ? 1 : 0);
		}
	}
	
	public final Data data0 = new Data();
	public final Data data1 = new Data();
	
	public int pageMask;
	public int virtualPageNumber;
	public int addressSpaceId;
	public boolean global;
	
	@Override
	public String toString () {
		return String.format("Entry[pm=%x vpn=%x asid=%x g=%x d0=%s d1=%s]",
				pageMask, virtualPageNumber, addressSpaceId, global ? 1 : 0, data0, data1);
	}
}

