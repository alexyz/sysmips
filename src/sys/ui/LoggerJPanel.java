package sys.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;

import sys.util.Log;

/**
 * display log
 */
public class LoggerJPanel extends JPanel {

	private final JTextField filterTextField = new JTextField();
	private final LogsTableModel tableModel = new LogsTableModel();
	private final JCheckBox filterConjCheckBox = new JCheckBox("AND");
	private final JTable table;
	
	public LoggerJPanel() {
		super(new BorderLayout());
		
		filterTextField.setColumns(20);
		filterTextField.addActionListener(ae -> filter());
		
		filterConjCheckBox.addActionListener(ae -> filter());
		
		table = new JTable(tableModel) {
			@Override
			public String getToolTipText (MouseEvent e) {
				int r = rowAtPoint(e.getPoint());
				Log l = tableModel.getRow(r);
				return "<html><b>" + l.sym + "</b><br>" + MaltaJFrame.wrap(l.msg, 100).replace("\n","<br>") + "</html>";
			}
		};
		
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				if (!isSelected) {
					boolean l = tableModel.getRow(row).ex;
					c.setBackground(l ? Color.lightGray : Color.white);
				}
				return c;
			}
		});
		
		table.setAutoCreateRowSorter(true);
		
		table.getColumnModel().getColumn(0).setMaxWidth(100);
		table.getColumnModel().getColumn(1).setMaxWidth(50);
		table.getColumnModel().getColumn(2).setMaxWidth(100);
		table.getColumnModel().getColumn(3).setPreferredWidth(100);
		table.getColumnModel().getColumn(4).setMaxWidth(100);
		
		JPanel northPanel = new JPanel();
		northPanel.add(new JLabel("Filter"));
		northPanel.add(filterTextField);
		northPanel.add(filterConjCheckBox);
		
		JScrollPane scrollPane = new JScrollPane(table);
		
		add(northPanel, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
		setBorder(new EmptyBorder(5,5,5,5));
	}
	
	private void filter () {
		tableModel.setFilter(filterTextField.getText(), filterConjCheckBox.isSelected());
	}

	public void addLogs (Log[] logs) {
		tableModel.addLogs(logs);
	}
	
}
