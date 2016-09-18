package sys.ui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import sys.util.Symbol;
import sys.util.Symbols;

/**
 * display symbol table
 */
public class SymbolJPanel extends JPanel {

	private final JTextField textField = new JTextField();
	private final SymbolsTableModel tableModel = new SymbolsTableModel();
	private final JTable table = new JTable(tableModel);
	private final JButton openButton = new JButton("Open");
	
	private Symbols symbols;
	
	public SymbolJPanel() {
		super(new BorderLayout());
		
		textField.setColumns(20);
		textField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate (DocumentEvent e) {
				update();
			}
			@Override
			public void insertUpdate (DocumentEvent e) {
				update();
			}
			@Override
			public void changedUpdate (DocumentEvent e) {
				update();
			}
		});
		
		openButton.addActionListener(e -> open());
		
		table.setAutoCreateRowSorter(true);
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked (MouseEvent e) {
				if (e.getClickCount() == 2) {
					int r = table.rowAtPoint(e.getPoint());
					if (r >= 0) {
						Symbol s = tableModel.getRow(table.convertRowIndexToModel(r));
						getFrame().openMemory(s.addr);
					}
				}
			}
		});
		
		JPanel northPanel = new JPanel();
		northPanel.add(new JLabel("Filter"));
		northPanel.add(textField);
		northPanel.add(openButton);
		
		JScrollPane scrollPane = new JScrollPane(table);
		
		add(northPanel, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
		setBorder(new EmptyBorder(5,5,5,5));
	}
	
	private void open () {
		int r = table.getSelectedRow();
		if (r >= 0) {
			Symbol s = tableModel.getRow(table.convertRowIndexToModel(r));
			getFrame().openMemory(s.addr);
		}
	}

	private MaltaJFrame getFrame() {
		return (MaltaJFrame) SwingUtilities.getAncestorOfClass(MaltaJFrame.class, getParent());
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
				if (t.length() == 0 || s.name.toLowerCase().contains(t) || Integer.toHexString(s.addr).contains(t)) {
					l.add(s);
				}
			}
			tableModel.setSymbols(l);
		}
	}
}