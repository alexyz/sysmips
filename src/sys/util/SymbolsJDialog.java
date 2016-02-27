package sys.util;

import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.JDialog;

import sys.ui.SymbolJPanel;

public class SymbolsJDialog extends JDialog {
	private final SymbolJPanel panel = new SymbolJPanel();
	public SymbolsJDialog(Frame frame) {
		super(frame, "Symbols");
		setContentPane(panel);
		setPreferredSize(new Dimension(480, 480));
		pack();
	}
	public void setSymbols(Symbols s) {
		panel.setSymbols(s);
	}
}
