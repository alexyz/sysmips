package sys.mips;

public class EntryData {
	/** 20 bits */
	public int physicalFrameNumber;
	public boolean dirty;
	public boolean valid;
	@Override
	public String toString () {
		return String.format("Data[pfn=%x %s %s]", physicalFrameNumber, dirty ? "dirty" : "clean", valid ? "valid" : "invalid");
	}
}