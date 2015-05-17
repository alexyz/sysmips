package sys.elf;

import java.io.*;

/**
 * ELF relocation type
 */
public class ELF32Relocation {
	
	public final int r_offset;
	public final int r_info;
	public final int r_addend;
	
	public ELF32Relocation (ELF32Header header, DataInput in, boolean addend) throws IOException {
		r_offset = header.decode(in.readInt());
		r_info = header.decode(in.readInt());
		r_addend = addend ? header.decode(in.readInt()) : 0;
	}
	
	@Override
	public String toString () {
		return String.format("ELF32Relocation[offset: %-8x info: %-8x  addend: %d", r_offset, r_info, r_addend);
	}
	
}
