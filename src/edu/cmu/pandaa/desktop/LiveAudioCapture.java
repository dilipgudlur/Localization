package edu.cmu.pandaa.desktop;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.FrameStream;
import edu.cmu.pandaa.stream.RawAudioFileStream;

public class LiveAudioCapture extends JPanel {
	JButton convertWavToFrameButton, playWavAudioButton;
	JButton captureAudioButton;
	JButton openWavButton, saveAsButton;
	JTextField wavFilePath, frameFilePath, txtDeviceName, txtTrialName;
	JTextArea log;
	JPanel audioCapturePanel, testPanel, playAudioPanel;
	JTabbedPane tabbedPanel;
	JFileChooser fc;
	int audioCaptureTime;

	boolean stopCapture = false;

	ByteArrayOutputStream byteArrayOutputStream;
	FrameStream fs;
	public long timeStamp;
	public boolean isTimeStamped;
	private int audioFormat, bitsPerSample;
	private int numChannels, samplingRate, dataSize;
	private int frameLength;
	protected String filePath;

	private final static int DEFAULT_FORMAT = 1; // PCM
	private final static int DEFAULT_CHANNELS = 1; // MONO
	private final static int DEFAULT_SAMPLING_RATE = 16000;
	private final static int DEFAULT_BITS_PER_SAMPLE = 16;
	private final static int DEFAULT_SUBCHUNK1_SIZE = 16; // For PCM
	private final static int DEFAULT_DATA_SIZE = 0;
	private final static int DEFAULT_FRAMELENGTH = 100;
	private final static int DEFAULT_AUDIO_CAPTURE_TIME = 10;

	public LiveAudioCapture(int format, int samplingRate, int bitsPerSample, int frameLen,
	    String outFile) {
		super(new BorderLayout());
		audioFormat = format;
		this.samplingRate = samplingRate;
		this.bitsPerSample = bitsPerSample;
		frameLength = frameLen;
		numChannels = DEFAULT_CHANNELS;
		dataSize = DEFAULT_DATA_SIZE;
		filePath = outFile;
		isTimeStamped = false;
	}

	public LiveAudioCapture() {
		this(DEFAULT_FORMAT, DEFAULT_SAMPLING_RATE, DEFAULT_BITS_PER_SAMPLE, DEFAULT_FRAMELENGTH, null);

		fc = new JFileChooser();

		audioCapturePanel = new JPanel();
		testPanel = new JPanel();
		playAudioPanel = new JPanel();

		createAudioCapturePanel();

		log = new JTextArea(4, 20);
		log.setMargin(new Insets(5, 5, 5, 5));
		log.setEditable(false);
		JScrollPane logScrollPane = new JScrollPane(log);

		add(audioCapturePanel, BorderLayout.CENTER);
		add(logScrollPane, BorderLayout.PAGE_END);
	}

	private void createAudioCapturePanel() {
		txtTrialName = new JTextField(25);
		txtDeviceName = new JTextField(25);

		captureAudioButton = new JButton("Capture Audio In a file");
		captureAudioButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				captureAudioButton.setEnabled(false);
				if (txtTrialName.getText().length() == 0)
					log.append("Enter Trial Name\n");
				else if (txtDeviceName.getText().length() == 0)
					log.append("Enter Device name\n");
				else {
					log.setText("");
					fc.setDialogTitle("Select Directory to save the Audio");
					fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					int returnVal = fc.showSaveDialog(LiveAudioCapture.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
					filePath = fc.getSelectedFile().toString() + File.separator + txtTrialName.getText() + "-" + txtDeviceName.getText() + ".wav";
					//saveAudio(new LiveAudioStream(20000), filePath);
					} else {
						log.setText("Audio capture cancelled");
					}
				}
				captureAudioButton.setEnabled(true);
			}
		});

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		JPanel audioButtonPanel = new JPanel(new GridBagLayout());
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 5;
		gbc.fill = GridBagConstraints.VERTICAL;
		audioButtonPanel.add(new JLabel("Capture audio and save in wav file"), gbc);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		audioButtonPanel.add(new JLabel("Trial Name"), gbc);
		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.gridwidth = 3;
		audioButtonPanel.add(txtTrialName, gbc);
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.BOTH;
		audioButtonPanel.add(new JLabel("Device Name"), gbc);
		gbc.gridx = 2;
		gbc.gridy = 2;
		gbc.gridwidth = 3;
		audioButtonPanel.add(txtDeviceName, gbc);
		gbc.gridx = 2;
		gbc.gridy = 3;
		gbc.gridwidth = 2;
		audioButtonPanel.add(captureAudioButton, gbc);
		audioCapturePanel.add(audioButtonPanel);
	}
	
	private void saveAudio(final LiveAudioStream liveAudioStream, final String fileName) {
		new Thread() {
			public void run() {
				RawAudioFileStream rawAudioOutputStream;
				try {
					rawAudioOutputStream = new RawAudioFileStream(fileName, true);
					rawAudioOutputStream.setHeader(liveAudioStream.getHeader());
					log.append("Saving captured audio in " + fileName + "\n");
					System.out.println("Saving captured audio in " + fileName);
					StreamFrame frame;
					while ((frame = liveAudioStream.recvFrame()) != null) {
						rawAudioOutputStream.sendFrame(frame);
					}
					liveAudioStream.close();
					rawAudioOutputStream.close();
					log.append("Audio saved\n");
					System.out.println("Audio saved");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	private void createAndShowGUI() {
		JFrame frame = new JFrame("PANDAA - Live Audio Capture");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.add(new LiveAudioCapture());
		frame.pack();
		frame.setBounds(400, 250, 500, 280);
		frame.setResizable(false);
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		LiveAudioCapture liveAudioCapture = new LiveAudioCapture();
		liveAudioCapture.createAndShowGUI();
	}
}
