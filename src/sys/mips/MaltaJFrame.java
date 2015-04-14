package sys.mips;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.beans.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;

public class MaltaJFrame extends JFrame implements PropertyChangeListener {
	
	private static final Font MONO = new Font("Monospaced", Font.BOLD, 12);
	
	private final JLabel displayLabel = new JLabel(" ");
	private final JTextArea consoleArea = new JTextArea();
	
	public MaltaJFrame () {
		super("Sysmips");
		
		displayLabel.setFont(MONO);
		displayLabel.setBorder(new EtchedBorder());
		
		consoleArea.setFont(MONO);
		consoleArea.setLineWrap(true);
		consoleArea.setEditable(false);
		
		JPanel topPanel = new JPanel();
		topPanel.add(displayLabel);
		
		JScrollPane consoleSp = new JScrollPane(consoleArea);
		consoleSp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		consoleSp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		JPanel p = new JPanel(new BorderLayout());
		p.add(topPanel, BorderLayout.NORTH);
		p.add(consoleSp, BorderLayout.CENTER);
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(640, 480));
		setContentPane(p);
		pack();
	}
	
	@Override
	public void propertyChange (PropertyChangeEvent evt) {
		// System.out.println("jframe event " + evt);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run () {
				update(evt.getPropertyName(), evt.getNewValue());
			}
		});
	}
	
	private void update (String propertyName, Object newValue) {
		// System.out.println("jframe update " + propertyName);
		switch (propertyName) {
			case "display":
				displayLabel.setText((String) newValue);
				displayLabel.repaint();
				break;
			case "console": {
				Document doc = consoleArea.getDocument();
				try {
					doc.insertString(doc.getLength(), (String) newValue, null);
				} catch (BadLocationException e) {
					throw new RuntimeException(e);
				}
				consoleArea.setCaretPosition(doc.getLength());
				break;
			}
			default:
				throw new RuntimeException("unknown property " + propertyName);
		}
	}
}
