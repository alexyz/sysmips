package sys.elf;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

public class ELF32 {
	
	/**
	 * Load a string from a string table byte array
	 */
	public static String readString (final byte[] data, final int index) {
		int len = 0;
		while (data[index + len] != 0) {
			len++;
		}
		return new String(data, index, len);
	}
	
	public final ELF32Header header;
	public final List<ELF32Section> sections = new ArrayList<>();
	public final List<ELF32Program> programs = new ArrayList<>();
	public final List<ELF32Symbol> symbols = new ArrayList<>();
	
	public ELF32 (ByteBuffer buf) {
		header = new ELF32Header(buf);
		System.out.println(header);
		
		// load section headers
		buf.position(header.sectionHeaderOffset);
		for (int n = 0; n < header.sectionHeaders; n++) {
			sections.add(new ELF32Section(header, buf));
		}
		
		// load section names
		if (header.stringTableSection != ELF32Header.SHN_UNDEF) {
			ELF32Section stSection = sections.get(header.stringTableSection);
			byte[] strings = new byte[stSection.fileSize];
			buf.position(stSection.fileOffset);
			buf.get(strings, 0, strings.length);
			for (ELF32Section section : sections) {
				section.name = readString(strings, section.nameIndex);
			}
		}
		
		// load program headers
		buf.position(header.programHeaderOffset);
		for (int n = 0; n < header.programHeaders; n++) {
			programs.add(new ELF32Program(header, buf));
		}
		
		// load symbols
		for (ELF32Section section : sections) {
			if (section.type == ELF32Section.SHT_SYMTAB) {
				// load string table
				ELF32Section stringSection = sections.get(section.linkedSection);
				byte[] strings = new byte[stringSection.fileSize];
				buf.position(stringSection.fileOffset);
				buf.get(strings, 0, strings.length);
				// now load symbol table
				buf.position(section.fileOffset);
				int length = section.fileSize / section.entrySize;
				for (int s = 0; s < length; s++) {
					symbols.add(new ELF32Symbol(header, buf, strings));
				}
				break;
			}
		}
	}
	
	public void print (PrintStream ps) {
		ps.println("HEADER " + header);
		for (int n = 0; n < sections.size(); n++) {
			ps.println("SECTION[" + n + "] " + sections.get(n));
		}
		for (int n = 0; n < programs.size(); n++) {
			ps.println("PROGRAM[" + n + "] " + programs.get(n));
		}
		ps.println("SYMBOLS " + symbols.size());
	}
	
	@Override
	public String toString () {
		return "ELF[" + header.typeString() + " sections=" + sections.size() + " programs=" + programs.size() + " symbols=" + symbols.size() + "]";
	}
}
