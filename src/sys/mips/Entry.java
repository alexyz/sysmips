package sys.mips;

/** 
 * joint tlb entry
 */
public class Entry {
	public final Data evenData = new Data();
	public final Data oddData = new Data();
	
	public int pageMask;
	public int virtualPageNumber;
	public int addressSpaceId;
	public boolean global;
}

class Data {
	public int physicalFrameNumber;
	public boolean dirty;
	public boolean valid;
}