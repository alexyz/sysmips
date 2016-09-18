package sys.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import sys.util.Symbol;

public class SymbolsTableModel extends AbstractTableModel {

	private final List<Symbol> symbols = new ArrayList<>();
	
	@Override
	public int getRowCount () {
		return symbols.size();
	}

	public void setSymbols (List<Symbol> l) {
		symbols.clear();
		symbols.addAll(l);
		fireTableDataChanged();
	}

	@Override
	public int getColumnCount () {
		return 3;
	}
	
	@Override
	public String getColumnName (int col) {
		switch (col) {
			case 0: return "Address";
			case 1: return "Name";
			case 2: return "Size";
			default: throw new RuntimeException();
		}
	}

	@Override
	public Object getValueAt (int row, int col) {
		Symbol s = symbols.get(row);
		switch (col) {
			case 0: return Integer.toHexString(s.addr);
			case 1: return s.name;
			case 2: return s.size == Integer.MAX_VALUE ? "" : String.valueOf(s.size);
			default: throw new RuntimeException();
		}
	}
	
	public Symbol getRow (int row) {
		return symbols.get(row);
	}
	
}