package sys.mips;

/** 
 * joint tlb entry
 */
public class Entry {
	
	public final EntryData[] data = new EntryData[2];
	
	public int pageMask;
	public int virtualPageNumber2;
	public int addressSpaceId;
	public boolean global;
	
	public Entry () {
		for (int n = 0; n < data.length; n++) {
			data[n] = new EntryData();
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

