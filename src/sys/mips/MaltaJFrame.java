package sys.mips;

import java.awt.Dimension;
import java.awt.Font;
import java.beans.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public class MaltaJFrame extends JFrame implements PropertyChangeListener {
	
	private static final Font MONO = new Font("Monospaced", Font.BOLD, 14);
	
	private final JTextField displayField = new JTextField(8);
	private final Malta malta;
	
	public MaltaJFrame (Malta malta) {
		super("Malta");
		this.malta = malta;
		malta.getSupport().addPropertyChangeListener(this);
		
		displayField.setFont(MONO);
		displayField.setEditable(false);
		JPanel displayPanel = new JPanel();
		displayPanel.setBorder(new TitledBorder("Display"));
		displayPanel.add(displayField);
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(640, 480));
		setContentPane(displayPanel);
		pack();
	}
	
	@Override
	public void propertyChange (PropertyChangeEvent evt) {
		System.out.println("jframe event " + evt);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run () {
				update(evt.getPropertyName(), evt.getNewValue());
			}
		});
	}

	private void update (String propertyName, Object newValue) {
		System.out.println("jframe update " + propertyName);
		if (propertyName.equals("display")) {
			System.out.println("jframe update display");
			displayField.setText((String) newValue);
			displayField.repaint();
		}
	}
}
