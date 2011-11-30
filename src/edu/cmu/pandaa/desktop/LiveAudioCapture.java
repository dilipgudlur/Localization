package edu.cmu.pandaa.desktop;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
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
import edu.cmu.pandaa.stream.FrameStream;
import edu.cmu.pandaa.stream.RawAudioFileStream;
import edu.cmu.pandaa.utils.DataConversionUtil;
import edu.cmu.pandaa.utils.WavUtil;

public class LiveAudioCapture extends JPanel {
	JButton convertWavToFrameButton, playWavAudioButton;
	JButton captureAudioButton;
	JButton openWavButton, saveAsButton;
	JTextField wavFilePath, frameFilePath;
	JTextArea log;
	JPanel audioCapturePanel, testPanel, playAudioPanel;
	JTabbedPane tabbedPanel;
	JFileChooser fc;

	boolean stopCapture = false;

	ByteArrayOutputStream byteArrayOutputStream;
	FrameStream fs;
	public long timeStamp;
	public boolean isTimeStamped;
	private int audioFormat, bitsPerSample;
	private long numChannels, samplingRate, dataSize, headerSize;
	private int frameLength;
	private int audioCaptureTime;
	protected String filePath;

	private final static int DEFAULT_FORMAT = 1; // PCM
	private final static long DEFAULT_CHANNELS = 1; // MONO
	private final static long DEFAULT_SAMPLING_RATE = 16000;
	private final static int DEFAULT_BITS_PER_SAMPLE = 16;
	private final static long DEFAULT_SUBCHUNK1_SIZE = 16; // For PCM
	private final static long DEFAULT_DATA_SIZE = 0;
	private final static int DEFAULT_FRAMELENGTH = 100;
	private final static int DEFAULT_AUDIO_CAPTURE_TIME = 10;

	public LiveAudioCapture(int format, long samplingRate, int bitsPerSample, int frameLen,
			String outFile) {
		super(new BorderLayout());
		audioFormat = format;
		this.samplingRate = samplingRate;
		this.bitsPerSample = bitsPerSample;
		frameLength = frameLen;
		numChannels = DEFAULT_CHANNELS;
		headerSize = DEFAULT_SUBCHUNK1_SIZE;
		dataSize = DEFAULT_DATA_SIZE;
		filePath = outFile;
		isTimeStamped = false;
		audioCaptureTime = DEFAULT_AUDIO_CAPTURE_TIME * 1000; //numSeconds * 1000 ms
	}

	public LiveAudioCapture() {
		this(DEFAULT_FORMAT, DEFAULT_SAMPLING_RATE, DEFAULT_BITS_PER_SAMPLE, DEFAULT_FRAMELENGTH, null);

		fc = new JFileChooser();

		audioCapturePanel = new JPanel();
		testPanel = new JPanel();
		playAudioPanel = new JPanel();
		tabbedPanel = new JTabbedPane();

		createAudioCapturePanel();
		createTestConversionPanel();
		createPlayAudioPanel();

		tabbedPanel.addTab("Capture Audio", audioCapturePanel);
		tabbedPanel.addTab("Test Audio Conversion", testPanel);
		tabbedPanel.addTab("Play Audio", playAudioPanel);

		log = new JTextArea(4, 20);
		log.setMargin(new Insets(5, 5, 5, 5));
		log.setEditable(false);
		JScrollPane logScrollPane = new JScrollPane(log);

		add(tabbedPanel, BorderLayout.CENTER);
		add(logScrollPane, BorderLayout.PAGE_END);
	}

	private void createAudioCapturePanel() {
		captureAudioButton = new JButton("Capture Audio In a file");
		captureAudioButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				captureAudioButton.setEnabled(false);
				fc.setFileFilter(new FileNameExtensionFilter("WAVE file (.wav)", "wav"));
				int returnVal = fc.showSaveDialog(LiveAudioCapture.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					filePath = fc.getSelectedFile().getAbsolutePath();
					int extensionIndex = filePath.lastIndexOf('.');
					if (extensionIndex != -1)
						filePath = filePath.substring(0, extensionIndex);
					filePath = filePath + ".wav";
					// captureAudio(new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
					// 16000.0F, 16, 1, 2,
					// 16000.0F, true));
					startAudioCapture();
					captureAudioButton.setEnabled(true);
				} else
					captureAudioButton.setEnabled(true);
			}
		});

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		JPanel audioButtonPanel = new JPanel(new GridBagLayout());
		gbc.gridx = 0;
		gbc.gridy = 0;
		audioButtonPanel.add(new JLabel("Capture audio and save in wav file"), gbc);
		gbc.gridx = 0;
		gbc.gridy = 1;
		audioButtonPanel.add(captureAudioButton, gbc);
		audioCapturePanel.add(audioButtonPanel);
	}

	private void createTestConversionPanel() {
		openWavButton = new JButton("Open Wav file");
		openWavButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				fc.setFileFilter(new FileNameExtensionFilter("WAVE file (.wav)", "wav"));
				int returnVal = fc.showOpenDialog(LiveAudioCapture.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					wavFilePath.setText(fc.getSelectedFile().getAbsolutePath());
				} else {
					log.append("No file selected for conversion.\n");
				}
			}
		});

		wavFilePath = new JTextField(30);
		wavFilePath.setFocusable(false);

		saveAsButton = new JButton("Save As");
		saveAsButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				fc.setFileFilter(new FileNameExtensionFilter("Wav file (.wav)", "wav"));
				int returnVal = fc.showSaveDialog(LiveAudioCapture.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					String filePath = fc.getSelectedFile().getAbsolutePath();
					int extensionIndex = filePath.lastIndexOf('.');
					if (extensionIndex != -1)
						filePath = filePath.substring(0, extensionIndex);
					frameFilePath.setText(filePath + ".wav");
				} else {
					log.append("Save command cancelled by user.\n");
				}
			}
		});

		frameFilePath = new JTextField(30);
		frameFilePath.setFocusable(false);

		convertWavToFrameButton = new JButton("Convert a file to Audio frames");
		convertWavToFrameButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (wavFilePath.getText().length() == 0) {
					log.append("No wav file selected for conversion.\n");
				} else if (frameFilePath.getText().length() == 0) {
					log.append("No output frame file specified.\n");
				} else {
					RawAudioFileStream rfsInput = null;
					RawAudioFileStream rfsOutput = null;
					RawAudioFrame rawFrame = null;
					try {
						rfsInput = new RawAudioFileStream(wavFilePath.getText());
						rfsOutput = new RawAudioFileStream(frameFilePath.getText(), true);
						rfsOutput.setHeader(rfsInput.getHeader());
						while ((rawFrame = (RawAudioFrame) rfsInput.recvFrame()) != null) {
							rfsOutput.sendFrame(rawFrame);
						}
					} catch (IOException ex) {
						ex.printStackTrace();
					} catch (Exception ex) {
						ex.printStackTrace();
					} finally {
						if (rfsInput != null)
							rfsInput.close();
						if (rfsOutput != null)
							rfsOutput.close();
					}
				}
			}
		});

		JPanel buttonPanel = new JPanel(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 5;
		gbc.fill = GridBagConstraints.VERTICAL;
		buttonPanel.add(new JLabel("Test conversion of wav file to frame format"), gbc);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		buttonPanel.add(openWavButton, gbc);
		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.gridwidth = 3;
		buttonPanel.add(wavFilePath, gbc);
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.BOTH;
		buttonPanel.add(saveAsButton, gbc);
		gbc.gridx = 2;
		gbc.gridy = 2;
		gbc.gridwidth = 3;
		buttonPanel.add(frameFilePath, gbc);
		gbc.gridx = 2;
		gbc.gridy = 3;
		gbc.gridwidth = 2;
		buttonPanel.add(convertWavToFrameButton, gbc);

		testPanel.add(buttonPanel);
	}

	private void createPlayAudioPanel() {
		playWavAudioButton = new JButton("Play wav file");
		playWavAudioButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				fc.setFileFilter(new FileNameExtensionFilter("WAVE file (.wav)", "wav"));
				int returnVal = fc.showOpenDialog(LiveAudioCapture.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					WavUtil wavData = WavUtil.readWavFile(fc.getSelectedFile().getAbsolutePath());
					if (wavData != null) {
						playWavFile(wavData);
					} else {
						log.append("Error in reading wav file.\n");
					}
				} else {
					log.append("No .wav file selected for playing.\n");
				}
			}
		});
		playAudioPanel.add(playWavAudioButton);
	}

	private void createAndShowGUI() {
		JFrame frame = new JFrame("PandaaAudioConversionUI");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.add(new LiveAudioCapture());
		frame.pack();
		frame.setBounds(400, 250, 500, 280);
		frame.setResizable(false);
		frame.setVisible(true);
	}

	public void startAudioCapture() {
		System.out.println("Starting audio capture.");
		captureAudio(new AudioFormat((float) samplingRate, bitsPerSample, (int) numChannels, true, false));
		try {
			Thread.sleep(audioCaptureTime);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		stopCapture = true;
		log.append("Saving captured audio in " + filePath + ".\n");
		System.out.println("Saving captured audio in " + filePath + ".");
		saveCapturedAudio(filePath);
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	public void setAudioCaptureTime(int sec) {
		audioCaptureTime = sec * 1000; 
	}

	protected void saveCapturedAudio(String filePath) {
		RawAudioFileStream outFile = null;
		int startIndex = 0, endIndex = 0;
		try {
			outFile = new RawAudioFileStream(filePath, true);
			short[] audio = DataConversionUtil.byteArrayToShortArray(byteArrayOutputStream.toByteArray());
			dataSize = audio.length;
			int numAudioSamples = (int) (frameLength * samplingRate / 1000);
			RawAudioHeader header = new RawAudioHeader(getDeviceID(filePath), timeStamp, frameLength,
					audioFormat, numChannels, samplingRate, bitsPerSample, audioCaptureTime);
			outFile.setHeader(header);
			while (true) {
				startIndex = endIndex;
				endIndex = (int) ((dataSize < startIndex + numAudioSamples) ? dataSize : startIndex
						+ numAudioSamples);
				RawAudioFrame frame = header.makeFrame(numAudioSamples);
				for (int i = 0; i < endIndex - startIndex; i++) {
					frame.audioData[i] = audio[startIndex + i];
				}
				outFile.sendFrame(frame);
				if (endIndex >= dataSize)
					break;
			}
			outFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String getDeviceID(String fileName) {
		int startIndex = 0, endIndex;

		startIndex = fileName.lastIndexOf("\\") + 1;

		endIndex = fileName.lastIndexOf(".");
		if (endIndex == -1) {
			endIndex = fileName.length();
		}

		return fileName.substring(startIndex, endIndex);
	}

	// This method captures audio input from a microphone and saves it in a
	// ByteArrayOutputStream object.
	private void captureAudio(AudioFormat audioFormat) {
		try {
			Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
			Mixer mixer = null;
			for (int cnt = 0; cnt < mixerInfo.length; cnt++) {
				if (mixerInfo[cnt].getName().toLowerCase().contains("microphone")) {
					mixer = AudioSystem.getMixer(mixerInfo[cnt]);
					break;
				}
			}

			// Get everything set up for capture
			// audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
			// 16000.0F, 16, 1, 2,
			// 16000.0F, true);io
			// audioFormat = new AudioFormat(8000.0f, 16, 1, true, true);

			DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

			TargetDataLine targetDataLine = (TargetDataLine) mixer.getLine(dataLineInfo);
			targetDataLine.open(audioFormat);
			targetDataLine.start();

			// Creating a thread to capture the microphone data and start it running.
			// It will run until the Stop button is clicked.
			Thread captureThread = new CaptureThread(targetDataLine);
			captureThread.start();
		} catch (Exception e) {
			System.out.println(e);
			System.exit(0);
		}
	}

	// Inner class to capture data from microphone
	class CaptureThread extends Thread {
		byte tempBuffer[] = new byte[10000];
		TargetDataLine targetDataLine;

		public CaptureThread(TargetDataLine targetDataLine) {
			this.targetDataLine = targetDataLine;
		}

		@Override
		public void run() {
			byteArrayOutputStream = new ByteArrayOutputStream();
			stopCapture = false;
			try {
				while (!stopCapture) {
					if (!isTimeStamped) {
						isTimeStamped = true;
						timeStamp = System.currentTimeMillis();
					}
					int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
					if (cnt > 0) {
						byteArrayOutputStream.write(tempBuffer, 0, cnt);
					}
				}
				byteArrayOutputStream.close();
			} catch (Exception e) {
				System.out.println(e);
				System.exit(0);
			}
		}
	}

	public void playWavFile(WavUtil wavUtil) {
		try {
			byte audioData[] = wavUtil.getAudioData();
			InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
			AudioFormat audioFormat = new AudioFormat(wavUtil.getSamplingRate(),
					wavUtil.getBitsPerSample(), (int) wavUtil.getNumChannels(), true, false);

			AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, audioFormat,
					audioData.length / audioFormat.getFrameSize());

			DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
			SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
			sourceDataLine.open(audioFormat);
			sourceDataLine.start();

			// Create a thread to play back the data and start it running.
			// It will run until all the data has been played back.
			Thread playThread = new PlayThread(audioInputStream, sourceDataLine);
			playThread.start();
		} catch (Exception e) {
			System.out.println(e);
			System.exit(0);
		}
	}

	class PlayThread extends Thread {
		byte tempBuffer[] = new byte[10000];
		AudioInputStream audioInputStream;
		SourceDataLine sourceDataLine;

		public PlayThread(AudioInputStream audioInputStream, SourceDataLine sourceDataLine) {
			this.audioInputStream = audioInputStream;
			this.sourceDataLine = sourceDataLine;
		}

		@Override
		public void run() {
			try {
				int cnt;
				while ((cnt = audioInputStream.read(tempBuffer, 0, tempBuffer.length)) != -1) {
					if (cnt > 0) {
						sourceDataLine.write(tempBuffer, 0, cnt);
					}
				}
				sourceDataLine.drain();
				sourceDataLine.close();
			} catch (Exception e) {
				System.out.println(e);
				System.exit(0);
			}
		}
	}

	public static void main(String[] args) {
		// LiveAudioCapture liveAudioCapture = new LiveAudioCapture();
		// liveAudioCapture.createAndShowGUI();
		if (args.length == 0) {
			throw new RuntimeException("Usage java LiveAudioCapture outputFileName audioCaptureTime");
		}
		int arg = 0;
		String fileName = args[arg++];
		int captureTime = new Integer(args[arg++]);
		LiveAudioCapture liveAudioCapture = new LiveAudioCapture();
		liveAudioCapture.setFilePath(fileName);
		liveAudioCapture.setAudioCaptureTime(captureTime);
		liveAudioCapture.startAudioCapture();
	}
}
