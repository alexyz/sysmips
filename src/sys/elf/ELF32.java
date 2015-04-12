package sys.elf;

import java.io.*;
import java.util.*;

public class ELF32 {
	
	public static void main (String[] args) throws Exception {
		try (RandomAccessFile file = new RandomAccessFile(args[0], "r")) {
			ELF32 elf = new ELF32(file);
			System.out.println("elf=" + elf);
			elf.print(System.out);
		}
	}
	
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
	public final List<ELF32Relocation> relocations = new ArrayList<>();
	
	public ELF32 (RandomAccessFile file) throws Exception {
		header = new ELF32Header(file);
		
		// load section headers
		file.seek(header.sectionHeaderOffset);
		for (int n = 0; n < header.sectionHeaders; n++) {
			sections.add(new ELF32Section(file));
		}
		
		// load section names
		if (header.stringTableSection != ELF32Header.SHN_UNDEF) {
			ELF32Section stSection = sections.get(header.stringTableSection);
			byte[] strings = new byte[stSection.fileSize];
			file.seek(stSection.fileOffset);
			file.read(strings, 0, strings.length);
			for (ELF32Section section : sections) {
				section.name = readString(strings, section.nameIndex);
			}
		}
		
		// load program headers
		file.seek(header.programHeaderOffset);
		for (int n = 0; n < header.programHeaders; n++) {
			programs.add(new ELF32Program(file));
		}
		
		// load symbols and relocations
		for (ELF32Section section : sections) {
			boolean addend = false;
			
			switch (section.type) {
				case ELF32Section.SHT_SYMTAB: {
					// load string table
					ELF32Section stringSection = sections.get(section.linkedSection);
					byte[] strings = new byte[stringSection.fileSize];
					file.seek(stringSection.fileOffset);
					file.read(strings, 0, strings.length);
					// now load symbol table
					file.seek(section.fileOffset);
					int length = section.fileSize / section.entrySize;
					for (int s = 0; s < length; s++) {
						symbols.add(new ELF32Symbol(file, strings));
					}
					break;
				}
					
				case ELF32Section.SHT_RELA:
					addend = true;
					// fall through
					
				case ELF32Section.SHT_REL: {
					// load relocations, these are not actually needed though
					int length = section.fileSize / section.entrySize;
					file.seek(section.fileOffset);
					for (int r = 0; r < length; r++) {
						relocations.add(new ELF32Relocation(file, addend));
					}
					break;
				}
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
		ps.println("RELOCATIONS " + relocations.size());
	}
	
	@Override
	public String toString () {
		return "ELF[" + header.typeString() + " sections=" + sections.size() + " programs=" + programs.size() + " symbols=" + symbols.size() + " relocations=" + relocations.size() + "]";
	}
}
