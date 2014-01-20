import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.sun.jna.platform.win32.Crypt32Util;

public final class RdpPassSaver {
	final static Charset ENCODING = StandardCharsets.UTF_16LE;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new RdpPassSaver();
	}

	private final JFrame frame;
	private final JTextField password;

	RdpPassSaver() {
		try {
			UIManager
					.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception e) {
			System.out.println("Unable to load Windows look and feel");
		}
		// Create the frame and set its content pane
		frame = new JFrame("Rdp password saver");
		final JPanel jPanel1 = new JPanel();
		jPanel1.setLayout(new java.awt.GridLayout(0, 1));

		final JLabel jLabel3 = new JLabel(
				"Введите пароль и выберите файл для сохранения");
		jPanel1.add(jLabel3);
		// jPanel1.add(blankSpace);

		final JPanel pnlChooseFile = new JPanel();
		pnlChooseFile.setLayout(new GridLayout(0, 2));

		password = new JPasswordField();

		final JButton choose = new JButton("Выбрать");
		choose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (password.getText().trim().isEmpty()) {
					ShowMsg("Введите пароль для сохранения");
					return;
				}
				final JFileChooser fc = new JFileChooser(".");
				final FileNameExtensionFilter filter = new FileNameExtensionFilter(
						"RDP FILES", "rdp");
				fc.setFileFilter(filter);
				fc.setDialogTitle("Выберите файл для сохранения пароля для rdp");
				fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				// In response to a button click:
				final int returnVal = fc.showOpenDialog(frame);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					final File file = fc.getSelectedFile();
					log(file.getAbsolutePath());
					savePass(file);
					// This is where a real application would open the file.
					// String filePath = file.getAbsolutePath());

					// Desktop.getDesktop().open(new File(dir));
				}
			}
		});
		pnlChooseFile.add(password);
		pnlChooseFile.add(choose);
		jPanel1.add(pnlChooseFile);

		frame.add(jPanel1);
		// Set the size of the window to be big enough to accommodate all
		// controls
		frame.pack();
		frame.setLocationRelativeTo(null);

		// Display the window
		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}

	private void savePass(final File file) {
		try {
			final List<String> aLines = readLargerTextFileAlternate(
					file.getAbsolutePath(), "password");
			final String rdpFile = file.getAbsolutePath();
			final String result = rdpFile + ".withsavedpass.rdp";
			aLines.add("password 51:b:" + cryptRdpPassword(password.getText()));
			writeLargerTextFile(result, aLines);
			//
			File tmpFile = new File(rdpFile + ".tmp");
			int cnt = 0;
			while (tmpFile.exists()) {
				tmpFile = new File(rdpFile + "." + cnt + ".tmp");
				++cnt;
			}
			file.renameTo(tmpFile);

			new File(result).renameTo(new File(rdpFile));

			ShowMsg("Пароль успешно сохранен в файле " + rdpFile
					+ "\n Старый файл сохранен в " + tmpFile.getAbsolutePath());
		} catch (IOException e) {
			ShowMsg(e.getLocalizedMessage());
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private List<String> readLargerTextFileAlternate(final String aFileName,
			final String aPrefixFilter) throws IOException {
		final Path path = Paths.get(aFileName);
		List<String> aLines = new ArrayList<String>();
		try (BufferedReader reader = Files.newBufferedReader(path, ENCODING)) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				// process each line in some way
				if (!line.startsWith(aPrefixFilter)) {
					aLines.add(line);
				}
			}
		}
		return aLines;
	}

	private void writeLargerTextFile(final String aFileName,
			final List<String> aLines) throws IOException {
		final Path path = Paths.get(aFileName);
		try (BufferedWriter writer = Files.newBufferedWriter(path, ENCODING)) {
			for (String line : aLines) {
				writer.write(line);
				writer.newLine();
			}
		}
	}

	private static void log(Object aMsg) {
		System.out.println(String.valueOf(aMsg));
	}

	private void ShowMsg(Object aMsg) {
		JOptionPane.showMessageDialog(frame, aMsg);
	}

	private static String ToHexString(byte[] bytes) {
		final StringBuilder sb = new StringBuilder();
		final Formatter formatter = new Formatter(sb);
		for (byte b : bytes) {
			formatter.format("%02x", b);
		}
		formatter.close();
		return sb.toString();
	}

	private static String cryptRdpPassword(String pass) {
		return ToHexString(Crypt32Util.cryptProtectData(
				pass.getBytes(ENCODING), null, 0, "psw", null));
	}
}
