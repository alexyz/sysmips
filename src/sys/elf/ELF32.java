package sys.elf;

import java.io.*;

public class ELF32 {
	
	public final ELF32Header header;
	public final ELF32Section[] sections;
	public final ELF32Program[] programs;
	public final ELF32Symbol[] symbols;
	public final ELF32Relocation[] relocations;
	
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
	
	public ELF32 (RandomAccessFile file) throws Exception {
		header = new ELF32Header(file);
		
		// load section headers
		sections = new ELF32Section[header.sectionHeaders];
		file.seek(header.sectionHeaderOffset);
		for (int n = 0; n < sections.length; n++) {
			sections[n] = new ELF32Section(file);
		}
		
		// load section names
		if (header.stringTableSection != ELF32Header.SHN_UNDEF) {
			ELF32Section section = sections[header.stringTableSection];
			byte[] strings = new byte[section.fileSize];
			file.seek(section.fileOffset);
			file.read(strings, 0, strings.length);
			for (int n = 0; n < sections.length; n++) {
				sections[n].name = readString(strings, sections[n].nameIndex);
			}
		}
		
		// load program headers
		programs = new ELF32Program[header.programHeaders];
		file.seek(header.programHeaderOffset);
		for (int n = 0; n < header.programHeaders; n++) {
			programs[n] = new ELF32Program(file);
		}
		
		// load symbols and relocations
		ELF32Symbol[] symbols = new ELF32Symbol[0];
		ELF32Relocation[] relocations = new ELF32Relocation[0];
		
		for (int n = 0; n < sections.length; n++) {
			final ELF32Section section = sections[n];
			boolean addend = false;
			
			switch (section.type) {
				case ELF32Section.SHT_SYMTAB: {
					// load string table
					ELF32Section stringSection = sections[section.linkedSection];
					byte[] strings = new byte[stringSection.fileSize];
					file.seek(stringSection.fileOffset);
					file.read(strings, 0, strings.length);
					// now load symbol table
					file.seek(section.fileOffset);
					symbols = new ELF32Symbol[section.fileSize / section.entrySize];
					for (int s = 0; s < symbols.length; s++) {
						symbols[s] = new ELF32Symbol(file, strings);
					}
					break;
				}
					
				case ELF32Section.SHT_RELA:
					addend = true;
					
				case ELF32Section.SHT_REL:
					// load relocations, these are not actually needed though
					relocations = new ELF32Relocation[section.fileSize / section.entrySize];
					file.seek(section.fileOffset);
					for (int r = 0; r < relocations.length; r++) {
						relocations[r] = new ELF32Relocation(file, addend);
					}
					break;
			}
		}
		
		this.symbols = symbols;
		this.relocations = relocations;
	}
	
	public void print (PrintStream ps) {
		ps.println("HEAD " + header);
		for (int n = 0; n < sections.length; n++) {
			ps.println("SEC" + n + ": " + sections[n]);
		}
		for (int n = 0; n < programs.length; n++) {
			ps.println("PROG" + n + ": " + programs[n]);
		}
		ps.println("SYMS " + symbols.length);
		ps.println("RELS " + relocations.length);
	}
	
	@Override
	public String toString () {
		return "ELF[" + header.type() + " sections=" + sections.length + " programs=" + programs.length + " symbols=" + symbols.length + " relocations=" + relocations.length + "]";
	}
}
