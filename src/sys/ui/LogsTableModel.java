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
		return 6;
	}
	
	@Override
	public Class<?> getColumnClass (int col) {
		switch (col) {
			case 0: return Long.class;
			case 1:
			case 2: 
			case 3: 
			case 4:
			case 5: return String.class;
			default: throw new RuntimeException();
		}
	}
	
	@Override
	public String getColumnName (int col) {
		switch (col) {
			case 0: return "Cycle";
			case 1: return "KM";
			case 2: return "IE";
			case 3: return "EX";
			case 4: return "Name";
			case 5: return "Message";
			default: throw new RuntimeException();
		}
	}
	
	@Override
	public Object getValueAt (int row, int col) {
		Log l = logs.get(row);
		switch (col) {
			case 0: return l.cycle;
			case 1: return l.km ? "km" : "";
			case 2: return l.ie ? "ie" : "";
			case 3: return l.ex ? "ex" : "";
			case 4: return l.name;
			case 5: return l.msg;
			default: throw new RuntimeException();
		}
	}

}
