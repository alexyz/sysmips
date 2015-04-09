package sys.elf;

import java.io.*;

/**
 * ELF relocation type
 */
public class ELF32Relocation {
	
	public final int r_offset;
	public final int r_info;
	public final int r_addend;
	
	public ELF32Relocation (DataInput in, boolean addend) throws IOException {
		r_offset = in.readInt();
		r_info = in.readInt();
		r_addend = addend ? in.readInt() : 0;
	}
	
	@Override
	public String toString () {
		return String.format("ELF32Relocation[offset: %-8x info: %-8x  addend: %d", r_offset, r_info, r_addend);
	}
	
}
