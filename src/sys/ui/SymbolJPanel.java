package sys.ui;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import sys.util.Symbol;
import sys.util.Symbols;

/**
 * display symbol table
 */
public class SymbolJPanel extends JPanel {

	private final JTextField textField = new JTextField();
	private final SymbolsTableModel tableModel = new SymbolsTableModel();
	private final JTable table = new JTable(tableModel);
	private Symbols symbols;
	
	public SymbolJPanel() {
		super(new BorderLayout());
		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped (KeyEvent e) {
				update();
			}
		});
		add(textField, BorderLayout.NORTH);
		add(new JScrollPane(table), BorderLayout.CENTER);
	}
	
	public void setSymbols(Symbols s) {
		this.symbols = s;
		update();
	}
	
	private void update() {
		if (symbols != null) {
			String t = textField.getText().trim().toLowerCase();
			List<Symbol> l = new ArrayList<>();
			for (Symbol s : symbols.getSymbols()) {
				if (t.length() == 0 || s.name.contains(t) || s.name.toLowerCase().contains(t) || Integer.toHexString(s.addr).contains(t)) {
					l.add(s);
				}
			}
			tableModel.setSymbols(l);
		}
	}
}

class SymbolsTableModel extends AbstractTableModel {

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
			case 2: return s.size;
			default: throw new RuntimeException();
		}
	}
	
}