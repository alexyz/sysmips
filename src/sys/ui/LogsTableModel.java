package sys.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import sys.util.Log;

public class LogsTableModel extends AbstractTableModel {

	private final List<Log> logs = new ArrayList<>();
	
	@Override
	public int getRowCount () {
		return logs.size();
	}

	public void addLogs (Log[] a) {
		if (logs.size() > 10000) {
			logs.subList(0, 1000).clear();
		}
		//int i = logs.size();
		logs.addAll(Arrays.asList(a));
		fireTableDataChanged();
		//fireTableRowsInserted(i, this.logs.size() - 1);
	}

	@Override
	public int getColumnCount () {
		return 4;
	}
	
	@Override
	public Class<?> getColumnClass (int col) {
		switch (col) {
			case 0: return Long.class;
			case 1:
			case 2: 
			case 3: return String.class;
			default: throw new RuntimeException();
		}
	}
	
	@Override
	public String getColumnName (int col) {
		switch (col) {
			case 0: return "Cycle";
			case 1: return "Mode";
			case 2: return "Name";
			case 3: return "Message";
			default: throw new RuntimeException();
		}
	}
	
	public Log getRow (int row) {
		return logs.get(row);
	}
	
	@Override
	public Object getValueAt (int row, int col) {
		Log l = logs.get(row);
		switch (col) {
			case 0: return l.cycle;
			case 1: return (l.km ? "km" : "") + (l.ie ? "ie" : "") + (l.ex ? "ex" : "");
			case 2: return l.name;
			case 3: return l.msg;
			default: throw new RuntimeException();
		}
	}

}
