package sys.ui;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import sys.util.Log;

/**
 * display log
 */
public class LoggerJPanel extends JPanel {

	public static LoggerJPanel instance;
	
	private final JTextField textField = new JTextField();
	private final LogsTableModel tableModel = new LogsTableModel();
	private final JTable table = new JTable(tableModel);
	
	public LoggerJPanel() {
		super(new BorderLayout());
		textField.setColumns(20);
//		textField.addKeyListener(new KeyAdapter() {
//			@Override
//			public void keyTyped (KeyEvent e) {
//				update();
//			}
//		});
		table.setAutoCreateRowSorter(true);
		table.getColumnModel().getColumn(0).setMaxWidth(100);
		table.getColumnModel().getColumn(1).setMaxWidth(33);
		table.getColumnModel().getColumn(2).setMaxWidth(33);
		table.getColumnModel().getColumn(3).setMaxWidth(33);
		table.getColumnModel().getColumn(4).setMaxWidth(100);
		
		JPanel northPanel = new JPanel();
		northPanel.add(textField);
		
		JScrollPane scrollPane = new JScrollPane(table);
		
		add(northPanel, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
		setBorder(new EmptyBorder(5,5,5,5));
		
		instance = this;
	}
	
	public void addLog(Log l) {
		tableModel.addLog(l);
//		update();
	}
	
//	private void update() {
//		if (symbols != null) {
//			String t = textField.getText().trim().toLowerCase();
//			List<Symbol> l = new ArrayList<>();
//			for (Symbol s : symbols.getSymbols()) {
//				if (t.length() == 0 || s.name.contains(t) || s.name.toLowerCase().contains(t) || Integer.toHexString(s.addr).contains(t)) {
//					l.add(s);
//				}
//			}
//			tableModel.setSymbols(l);
//		}
//	}
}