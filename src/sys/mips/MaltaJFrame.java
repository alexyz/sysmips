package sys.mips;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.beans.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;
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
	private final JLabel cycleLabel = new JLabel("");
	private final JTextArea consoleArea = new JTextArea();
	private final JButton startButton = new JButton("Start");
	private final JButton fileButton = new JButton("...");
	private final Timer timer;
	
	private volatile Malta malta;
	
	public MaltaJFrame () {
		super("Sysmips");
		
		timer = new Timer(100, e -> updateCycle(malta));
		timer.start();
		
		// should load these from prefs
		// command line of console=ttyS0 initrd=? root=?
		// environment keys: ethaddr, modetty0, memsize (defaults to 32MB)
		fileField.setText("images/vmlinux-3.2.0-4-4kc-malta");
		fileButton.addActionListener(ae -> selectFile());
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
		topPanel1.add(fileButton);
		topPanel1.add(new JLabel("Args"));
		topPanel1.add(argsField);
		topPanel1.add(new JLabel("Env"));
		topPanel1.add(envField);
		topPanel1.add(startButton);
		
		JPanel topPanel2 = new JPanel();
		topPanel2.add(displayLabel);
		topPanel2.add(cycleLabel);
		
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

	private void selectFile () {
		File dir = new File(System.getProperty("user.dir"));
		JFileChooser fc = new JFileChooser(dir);
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			fileField.setText(fc.getSelectedFile().toString());
		}
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
		
		Malta malta = new Malta();
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			final int top = CpuUtil.loadElf(malta.getCpu(), raf);
			CpuUtil.setMainArgs(malta.getCpu(), top + 0x100000, args, env);
			
		} catch (Exception e) {
			e.printStackTrace();
			showErrorDialog("Start", e);
			return;
		}
		
		malta.getSupport().addPropertyChangeListener(this);
		updateCycle(malta);
		
		this.malta = malta;
		
		final Thread t = new Thread(() -> {
			try {
				malta.getCpu().run();
			} catch (Exception e) {
				e.printStackTrace(System.out);
				SwingUtilities.invokeLater(() -> {
					updateCycle(malta);
					showErrorDialog("Start", e);
				});
			} finally {
				this.malta = null;
			}
		});
		
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}
	
	private void showErrorDialog(String title, Throwable t) {
		StringBuilder sb = new StringBuilder();
		while (t != null) {
			if (sb.length() > 0) {
				sb.append(": ");
			}
			sb.append(t);
			t = t.getCause();
		}
		showErrorDialog(title, sb.toString());
	}
	
	private void showErrorDialog(String title, String msg) {
		StringBuilder sb = new StringBuilder();
		int l = 0;
		for (int n = 0; n < msg.length(); n++) {
			final char c = msg.charAt(n);
			sb.append(c);
			if (l++ > 72 && c == ' ') {
				sb.append("\n");
				l = 0;
			}
		}
		JOptionPane.showMessageDialog(this, sb.toString(), title, JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public void propertyChange (PropertyChangeEvent evt) {
		// System.out.println("jframe event " + evt);
		SwingUtilities.invokeLater(() -> {
			update(evt.getPropertyName(), evt.getNewValue());
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
	
	private void updateCycle (Malta m) {
		if (m != null) {
			cycleLabel.setText("Cycle " + m.getCpu().getCycle());
		}
	}
	
}
