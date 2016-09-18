package sys.ui;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.*;

import sys.mips.Cpu;

public class MemoryJPanel extends JPanel {
	
	private final JTextField addrField = new JTextField(10);
	private final JButton upButton = new JButton(">");
	private final JButton downButton = new JButton("<");
	private final MemoryTableModel model = new MemoryTableModel();
	private final JTable table = new JTable(model);
	private final JCheckBox disasmCheckBox = new JCheckBox("Disasm");
	
	private Cpu cpu;
	
	public MemoryJPanel () {
		super(new BorderLayout());
		addrField.setText(Integer.toHexString(0x80100000));
		addrField.addActionListener(e -> update());
		upButton.addActionListener(e -> next(1));
		downButton.addActionListener(e -> next(-1));
		disasmCheckBox.addChangeListener(e -> update());
		table.getColumnModel().getColumn(0).setPreferredWidth(100);
		table.getColumnModel().getColumn(1).setPreferredWidth(200);
		table.getColumnModel().getColumn(2).setPreferredWidth(400);
		table.getColumnModel().getColumn(0).setMaxWidth(100);
		table.getColumnModel().getColumn(1).setMaxWidth(200);
		table.getColumnModel().getColumn(2).setCellRenderer(new MonoRenderer());
		JPanel p = new JPanel();
		p.add(new JLabel("Address"));
		p.add(addrField);
		p.add(downButton);
		p.add(upButton);
		p.add(disasmCheckBox);
		add(p, BorderLayout.NORTH);
		JScrollPane sp = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(sp, BorderLayout.CENTER);
	}
	
	private void next (int d) {
		int a = (int) Long.parseLong(addrField.getText(),16) + (d * (disasmCheckBox.isSelected() ? 64 : 256));
		addrField.setText(Integer.toHexString(a));
		update();
	}

	public void setCpu(Cpu cpu) {
		this.cpu = cpu;
		update();
	}

	private void update () {
		try {
			int a = (int) (Long.parseLong((String)addrField.getText(),16) & 0xffff_fff0);
			addrField.setText(Integer.toHexString(a));
			addrField.setBackground(Color.white);
			model.update(cpu, a, disasmCheckBox.isSelected());
		} catch (NumberFormatException e) {
			System.out.println(e);
			addrField.setBackground(Color.yellow);
		}
	}

	public void openMemory (int addr) {
		addrField.setText(Integer.toHexString(addr & 0xffffff00));
		update();
	}
	
}
