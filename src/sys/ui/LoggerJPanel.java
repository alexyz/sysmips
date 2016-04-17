package sys.ui;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

import sys.util.Log;

/**
 * display log
 */
public class LoggerJPanel extends JPanel {

	private final JTextField textField = new JTextField();
	private final LogsTableModel tableModel = new LogsTableModel();
	private final JTable table;
	
	public LoggerJPanel() {
		super(new BorderLayout());
		textField.setColumns(20);
		table = new JTable(tableModel) {
			@Override
			public javax.swing.table.TableCellRenderer getCellRenderer(int row, int column) {
				TableCellRenderer tcr = super.getCellRenderer(row, column);
				JComponent c = (JComponent) tcr;
				boolean l = tableModel.getRow(row).ex;
				c.setBackground(l ? Color.lightGray : Color.white);
				return tcr;
			}
		};
		table.setAutoCreateRowSorter(true);
		
		table.getColumnModel().getColumn(0).setMaxWidth(100);
		table.getColumnModel().getColumn(0).setResizable(false);
		table.getColumnModel().getColumn(1).setMaxWidth(33);
		table.getColumnModel().getColumn(1).setResizable(false);
		table.getColumnModel().getColumn(2).setMaxWidth(33);
		table.getColumnModel().getColumn(2).setResizable(false);
		table.getColumnModel().getColumn(3).setMaxWidth(33);
		table.getColumnModel().getColumn(3).setResizable(false);
		table.getColumnModel().getColumn(4).setMaxWidth(100);
		table.getColumnModel().getColumn(4).setResizable(false);
		
		JPanel northPanel = new JPanel();
		northPanel.add(textField);
		
		JScrollPane scrollPane = new JScrollPane(table);
		
		add(northPanel, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
		setBorder(new EmptyBorder(5,5,5,5));
	}
	
	public void addLogs (Log[] logs) {
		tableModel.addLogs(logs);
	}
	
}
