package sys.ui;

import java.awt.BorderLayout;
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
import sys.util.SymbolsJDialog;

public class MaltaJFrame extends JFrame implements PropertyChangeListener {
	
	public static void main (String[] args) {
		MaltaJFrame frame = new MaltaJFrame();
		frame.setVisible(true);
	}
	
	private static final Font MONO = new Font("Monospaced", Font.BOLD, 14);
	private static int threadInstance = 1;
	
	private final JTextField fileField = new JTextField(10);
	private final JTextField argsField = new JTextField(10);
	private final JLabel displayLabel = new JLabel("            ");
	private final JLabel cycleLabel = new JLabel("");
	private final JTextArea consoleArea = new JTextArea();
	private final JButton fileButton = new JButton("File");
	private final JButton loadButton = new JButton("Load");
	private final JButton runButton = new JButton("Run");
	private final JButton symbolsButton = new JButton("Symbols");
//	private final JButton stopButton = new JButton("Stop");
	private final JSpinner memSpinner = new JSpinner(new SpinnerNumberModel(32,32,512,1));
	private final Timer timer;
	
	private volatile Thread thread;
	private Cpu cpu;
	private SymbolsJDialog symbolsDialog;
	
	public MaltaJFrame () {
		super("Sysmips");
		
		timer = new Timer(1000, e -> updateCycle());
		
		loadButton.addActionListener(ae -> load());
		runButton.addActionListener(ae -> run());
		symbolsButton.addActionListener(e -> showSymbols());
		
		// should load this from prefs
		fileField.setText("images/vmlinux-3.2.0-4-4kc-malta");
		fileButton.addActionListener(ae -> selectFile());
		
		// command line of console=ttyS0 initrd=? root=?
		// environment keys: ethaddr, modetty0, memsize (defaults to 32MB)
		argsField.setText("debug initcall_debug ignore_loglevel");
		
//		stopButton.addActionListener(ae -> stop());
		
		displayLabel.setFont(MONO);
		displayLabel.setBorder(new EtchedBorder());
		
		consoleArea.setColumns(100);
		consoleArea.setRows(24);
		consoleArea.setFont(MONO);
		consoleArea.setLineWrap(true);
		consoleArea.setEditable(false);
		
		JPanel topPanel1 = new JPanel();
		topPanel1.add(new JLabel("File"));
		topPanel1.add(fileField);
		topPanel1.add(fileButton);
		topPanel1.add(new JLabel("Mem"));
		topPanel1.add(memSpinner);
		topPanel1.add(new JLabel("Args"));
		topPanel1.add(argsField);
		//topPanel1.add(new JLabel("Env"));
		//topPanel1.add(envField);
		topPanel1.add(loadButton);
		topPanel1.add(runButton);
		topPanel1.add(symbolsButton);
		//topPanel1.add(stopButton);
		
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
		setContentPane(p);
		pack();
	}
	
	private void showSymbols () {
		if (symbolsDialog == null) {
			symbolsDialog = new SymbolsJDialog(this);
		}
		if (cpu != null) {
			symbolsDialog.setSymbols(cpu.getMemory().getSymbols());
		}
		symbolsDialog.setLocationRelativeTo(this);
		symbolsDialog.setVisible(true);
	}

	@Override
	public void setVisible (boolean b) {
		if (b) {
			timer.start();
		} else {
			timer.stop();
		}
		super.setVisible(b);
	}

//	private void stop () {
//		if (cpu != null) {
//			cpu.interrupt(new CpuException(-1, 0));
//		}
//	}

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
	
	private void load () {
		if (thread != null) {
			showErrorDialog("Run", "Already running");
			return;
		}
		
		final File file = new File(fileField.getText());
		if (!file.isFile()) {
			showErrorDialog("Run", "Invalid file: " + file);
			return;
		}
		
		final int memsize = ((Integer)memSpinner.getValue()).intValue() * 0x100000;
		
		List<String> args = new ArrayList<>();
		// linux ignores first arg...
		args.add("linux");
		{
			StringTokenizer st = new StringTokenizer(argsField.getText());
			while (st.hasMoreTokens()) {
				args.add(st.nextToken());
			}
		}
		
		List<String> env = new ArrayList<>();
		env.add("memsize");
		env.add(String.valueOf(memsize));
		
		displayLabel.setText(" ");
		consoleArea.setText("");
		
		final Cpu cpu;
		
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			int[] top = new int[1];
			cpu = CpuUtil.loadElf(raf, memsize, top);
			CpuUtil.setMainArgs(cpu, top[0] + 0x100000, args, env);
			
		} catch (Exception e) {
			e.printStackTrace();
			showErrorDialog("Start", e);
			return;
		}
		
		cpu.getMemory().print(System.out);
		cpu.getSupport().addPropertyChangeListener(this);
		
		this.cpu = cpu;
		updateCycle();
	}
	
	private void run() {
		System.out.println("run");
		
		if (thread != null) {
			return;
		}
		
		if (cpu == null) {
			load();
			if (cpu == null) {
				return;
			}
		}
		
		Thread t = new Thread(() -> {
			try {
				cpu.run();
				
			} catch (Exception e) {
				System.out.println();
//				cpu.getLog().print(System.out);
				cpu.getMemory().print(System.out);
				
				System.out.println();
				final List<String> l = cpu.getIsnCount().entrySet()
						.stream()
						.filter(x -> x.getValue()[0] > 0)
						.sorted((x,y) -> y.getValue()[0] - x.getValue()[0])
						.map(x -> x.getKey() + "=" + x.getValue()[0])
						.collect(Collectors.toList());
				System.out.println("isn count " + l);
				
				System.out.println();
				e.printStackTrace(System.out);
				
				SwingUtilities.invokeLater(() -> {
					consoleArea.append(exceptionString(e));
					updateCycle();
					showErrorDialog("Start", e);
				});
				
			} finally {
				this.thread = null;
			}
		});
		t.setName("t" + threadInstance++);
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
			if (l++ > 72 && " /:".indexOf(c) >= 0) {
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
