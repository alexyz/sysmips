package sys.ui;

import javax.swing.table.AbstractTableModel;

import sys.mips.Cpu;
import sys.mips.InstructionUtil;
import sys.mips.Memory;
import sys.util.Symbols;

public class MemoryTableModel extends AbstractTableModel {
	
	private Cpu cpu;
	private int offset;
	private boolean disasm;
	
	@Override
	public int getRowCount () {
		return cpu != null ? 64 : 0;
	}
	
	public void update (Cpu cpu, int offset, boolean disasm) {
		this.cpu = cpu;
		this.offset = offset;
		this.disasm = disasm;
		fireTableDataChanged();
	}
	
	@Override
	public int getColumnCount () {
		return 3;
	}
	
	@Override
	public String getColumnName (int c) {
		switch (c) {
			case 0: return "Address";
			case 1: return "Name";
			case 2: return "Value";
			default: throw new RuntimeException();
		}
	}
	
	@Override
	public Object getValueAt (int r, int c) {
		return disasm ? getDisasmValueAt(r, c) : getHexValueAt(r, c);
	}
	
	private Object getDisasmValueAt (int r, int c) {
		Memory m = cpu.getMemory();
		int a = offset+r*4;
		Symbols s = cpu.getSymbols();
		switch (c) {
			case 0: return Integer.toHexString(a);
			case 1: return s.getNameOffset(a);
			case 2: {
				int w = m.loadWordKernel(a);
				return String.format("%08x: %s", w, InstructionUtil.isnString(a, w, s, null));
			}
			default: throw new RuntimeException();
		}
	}
	
	private Object getHexValueAt (int r, int c) {
		Memory m = cpu.getMemory();
		int a = offset+r*16;
		Symbols s = cpu.getSymbols();
		
		switch (c) {
			case 0: return Integer.toHexString(a);
			case 1: return s.getNameOffset(a);
			case 2: {
				StringBuilder sb = new StringBuilder();
				StringBuilder sb2 = new StringBuilder();
				for (int n = 0; n < 16; n++) { 
					int v = Byte.toUnsignedInt(m.loadByteKernel(a+n)); 
					String x = Integer.toHexString(v);
					if (sb.length() > 0) sb.append(" ");
					if (x.length() == 1) sb.append(" ");
					sb.append(x);
					sb2.append(v < 32 ? ' ' : (char) v);
				}
				sb.append(": ");
				sb.append(sb2);
				return sb.toString();
			}
			default: throw new RuntimeException();
		}
	}
	
}
