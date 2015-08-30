package sys.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.beans.*;
import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import javax.swing.text.*;

import sys.mips.Cpu;
import sys.mips.CpuUtil;
import sys.mips.Exn;

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
	private final JButton fileButton = new JButton("...");
	private final JButton runButton = new JButton("Run");
	private final JButton stopButton = new JButton("Stop");
	private final Timer timer;
	
	private volatile Thread thread;
	private Cpu cpu;
	
	public MaltaJFrame () {
		super("Sysmips");
		
		timer = new Timer(100, e -> updateCycle());
		timer.start();
		
		// should load these from prefs
		// command line of console=ttyS0 initrd=? root=?
		// environment keys: ethaddr, modetty0, memsize (defaults to 32MB)
		fileField.setText("images/vmlinux-3.2.0-4-4kc-malta");
		fileButton.addActionListener(ae -> selectFile());
		argsField.setText("console=ttyS0");
		envField.setText("key=value");
		
		runButton.addActionListener(ae -> start());
		
		stopButton.addActionListener(ae -> stop());
		
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
		topPanel1.add(runButton);
		topPanel1.add(stopButton);
		
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

	private void stop () {
		if (cpu != null) {
			cpu.interrupt(new Exn(-1));
		}
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
		if (thread != null) {
			showErrorDialog("Run", "Already running");
			return;
		}
		
		final File file = new File(fileField.getText());
		if (!file.isFile()) {
			showErrorDialog("Run", "Invalid file: " + file);
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
		
		final Cpu cpu;
		
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			int[] top = new int[1];
			cpu = CpuUtil.loadElf(raf, top);
			CpuUtil.setMainArgs(cpu, top[0] + 0x100000, args, env);
			
		} catch (Exception e) {
			e.printStackTrace();
			showErrorDialog("Start", e);
			return;
		}
		
		cpu.getMemory().print(System.out);
		cpu.getMemory().getMalta().getSupport().addPropertyChangeListener(this);
		
		this.cpu = cpu;
		updateCycle();
		
		run();
	}
	
	private void run() {
		Thread t = new Thread(() -> {
			try {
				cpu.run();
				
			} catch (Exception e) {
				e.printStackTrace(System.out);
				final List<String> l = cpu.getIsnCount().entrySet()
						.stream()
						.filter(x -> x.getValue()[0] > 0)
						.sorted((x,y) -> y.getValue()[0] - x.getValue()[0])
						.map(x -> x.getKey() + "=" + x.getValue()[0])
						.collect(Collectors.toList());
				System.out.println("isn count " + l);
				
				SwingUtilities.invokeLater(() -> {
					consoleArea.append(exceptionString(e));
					updateCycle();
					showErrorDialog("Start", e);
				});
				
			} finally {
				this.thread = null;
			}
		});
		
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
		
		thread = t;
	}

	private void showErrorDialog(String title, Throwable t) {
		showErrorDialog(title, exceptionString(t));
	}

	private static String exceptionString (Throwable t) {
		StringBuilder sb = new StringBuilder();
		while (t != null) {
			if (sb.length() > 0) {
				sb.append(": ");
			}
			sb.append(t);
			t = t.getCause();
		}
		return sb.toString();
	}
	
	private void showErrorDialog(String title, String msg) {
		StringBuilder sb = new StringBuilder();
		int l = 0;
		for (int n = 0; n < msg.length(); n++) {
			final char c = msg.charAt(n);
			sb.append(c);
			if (l++ > 72 && " /".indexOf(c) >= 0) {
				sb.append("\n");
				l = 0;
			}
		}
		JOptionPane.showMessageDialog(this, sb.toString(), title, JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public void propertyChange (PropertyChangeEvent evt) {
		// System.out.println("jframe event " + evt);
		SwingUtilities.invokeLater(() -> update(evt.getPropertyName(), evt.getNewValue()));
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
	
	private void updateCycle () {
		if (cpu != null) {
			cycleLabel.setText("Cycle " + NumberFormat.getInstance().format(cpu.getCycle()));
		}
	}
	
}
