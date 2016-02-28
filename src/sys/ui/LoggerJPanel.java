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

	private final JTextField textField = new JTextField();
	private final LogsTableModel tableModel = new LogsTableModel();
	private final JTable table = new JTable(tableModel);
	
	public LoggerJPanel() {
		super(new BorderLayout());
		textField.setColumns(20);
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
