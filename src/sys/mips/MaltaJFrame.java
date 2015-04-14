package sys.mips;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.beans.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;

public class MaltaJFrame extends JFrame implements PropertyChangeListener {
	
	public static void main (String[] args) {
		MaltaJFrame frame = new MaltaJFrame();
		frame.setVisible(true);
	}
	
	private static final Font MONO = new Font("Monospaced", Font.BOLD, 12);
	
	private final JTextField fileField = new JTextField(10);
	private final JTextField argsField = new JTextField(10);
	private final JTextField envField = new JTextField(10);
	private final JLabel displayLabel = new JLabel(" ");
	private final JTextArea consoleArea = new JTextArea();
	private final JButton startButton = new JButton("Start");
	
	private Malta malta;
	
	public MaltaJFrame () {
		super("Sysmips");
		
		// should load these from prefs
		// command line of console=ttyS0 initrd=? root=?
		// environment keys: ethaddr, modetty0, memsize (defaults to 32MB)
		fileField.setText("images/vmlinux-3.2.0-4-4kc-malta");
		argsField.setText("console=ttyS0");
		envField.setText("key=value");
		
		startButton.addActionListener(ae -> start());
		
		displayLabel.setFont(MONO);
		displayLabel.setBorder(new EtchedBorder());
		
		consoleArea.setFont(MONO);
		consoleArea.setLineWrap(true);
		consoleArea.setEditable(false);
		
		JPanel topPanel1 = new JPanel();
		topPanel1.add(new JLabel("File"));
		topPanel1.add(fileField);
		topPanel1.add(new JLabel("Args"));
		topPanel1.add(argsField);
		topPanel1.add(new JLabel("Env"));
		topPanel1.add(envField);
		topPanel1.add(startButton);
		
		JPanel topPanel2 = new JPanel();
		topPanel2.add(displayLabel);
		
		JPanel topPanel = new JPanel(new GridLayout(2, 1));
		topPanel.add(topPanel1);
		topPanel.add(topPanel2);
		
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
	
	public void setFile(String file) {
		fileField.setText(file);
	}
	
	private void start () {
		if (malta != null) {
			showErrorDialog("Start", "Already started");
			return;
		}
		
		final File file = new File(fileField.getText());
		if (!file.isFile()) {
			showErrorDialog("Start", "Invalid file: " + file);
			return;
		}
		
		List<String> args = Collections.singletonList(argsField.getText());
		
		List<String> env = new ArrayList<>();
		StringTokenizer envSt = new StringTokenizer(envField.getText());
		while (envSt.hasMoreTokens()) {
			String t = envSt.nextToken();
			int i = t.indexOf("=");
			if (i > 1) {
				env.add(t.substring(0, i));
				env.add(t.substring(i + 1));
			} else {
				showErrorDialog("Start", "Invalid environment: " + t);
				return;
			}
		}
		
		displayLabel.setText(" ");
		consoleArea.setText("");
		malta = new Malta();
		
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			final int top = CpuUtil.loadElf(malta.getCpu(), raf);
			CpuUtil.setMainArgs(malta.getCpu(), top + 0x100000, args, env);
			
		} catch (Exception e) {
			e.printStackTrace();
			showErrorDialog("Title", e.toString());
			return;
		}
		
		malta.getSupport().addPropertyChangeListener(this);
		
		final Thread t = new Thread(() -> {
			try {
				malta.getCpu().run();
			} catch (Exception e) {
				e.printStackTrace();
				SwingUtilities.invokeLater(() -> {
					showErrorDialog("Cpu", e.toString());
				});
			} finally {
				malta = null;
			}
		});
		
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}
	
	private void showErrorDialog(String title, String msg) {
		JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
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
