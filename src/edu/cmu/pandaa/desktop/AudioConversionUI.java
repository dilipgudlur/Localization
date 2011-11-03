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

import edu.cmu.pandaa.header.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.stream.FrameStream;
import edu.cmu.pandaa.stream.RawAudioFileStream;
import edu.cmu.pandaa.utils.WavUtil;

public class AudioConversionUI extends JPanel {
	JButton convertWavToFrameButton, playWavAudioButton;
	JButton captureAudioButton, stopAudioButton, saveAudioButton;
	JButton openWavButton, saveAsButton;
	JTextField wavFilePath, frameFilePath;
	JTextArea log;
	JPanel audioCapturePanel, testPanel, playAudioPanel;
	JTabbedPane tabbedPanel;
	JFileChooser fc;

	boolean stopCapture = false;

	ByteArrayOutputStream byteArrayOutputStream;
	FrameStream fs;
	WavUtil capturedWavUtil;

	public AudioConversionUI() {
		super(new BorderLayout());

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
		captureAudioButton = new JButton("Capture Audio");
		captureAudioButton.addActionListener( new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				capturedWavUtil = new WavUtil();
				captureAudioButton.setEnabled(false);
				stopAudioButton.setEnabled(true);
				saveAudioButton.setEnabled(false);
				// captureAudio(new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000.0F, 16, 1, 2,
				// 16000.0F, true));
				captureAudio(new AudioFormat((float) capturedWavUtil.getSamplingRate(),
						capturedWavUtil.getBitsPerSample(), (int) capturedWavUtil.getNumChannels(),
						true, false));
			}
		});

		stopAudioButton = new JButton("Stop Audio Capture");
		stopAudioButton.setEnabled(false);
		stopAudioButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopCapture = true;
				stopAudioButton.setEnabled(false);
				saveAudioButton.setEnabled(true);
				captureAudioButton.setEnabled(false);
			}
		});

		saveAudioButton = new JButton("Save Audio");
		saveAudioButton.setEnabled(false);
		saveAudioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fc.setFileFilter(new FileNameExtensionFilter("WAVE file (.wav)", "wav"));
				int returnVal = fc.showSaveDialog(AudioConversionUI.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					captureAudioButton.setEnabled(true);
					stopAudioButton.setEnabled(false);
					saveAudioButton.setEnabled(false);
					String filePath = fc.getSelectedFile().getAbsolutePath();
					int extensionIndex = filePath.lastIndexOf('.');
					if (extensionIndex != -1)
						filePath = filePath.substring(0, extensionIndex);
					filePath = filePath + ".wav";
					log.append("Saving captured audio in " + filePath + ".\n");
					capturedWavUtil.saveAudioData(byteArrayOutputStream.toByteArray());
					capturedWavUtil.saveWavFile(filePath);
				}
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
		gbc.gridx = 0;
		gbc.gridy = 2;
		audioButtonPanel.add(stopAudioButton, gbc);
		gbc.gridx = 0;
		gbc.gridy = 3;
		audioButtonPanel.add(saveAudioButton, gbc);
		audioCapturePanel.add(audioButtonPanel);
	}

	private void createTestConversionPanel() {
		openWavButton = new JButton("Open Wav file");
		openWavButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				fc.setFileFilter(new FileNameExtensionFilter("WAVE file (.wav)", "wav"));
				int returnVal = fc.showOpenDialog(AudioConversionUI.this);
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
		saveAsButton.addActionListener( new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				fc.setFileFilter(new FileNameExtensionFilter("Wav file (.wav)", "wav"));
				int returnVal = fc.showSaveDialog(AudioConversionUI.this);
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
						rfsOutput = new RawAudioFileStream(frameFilePath.getText(),true);
						rfsOutput.setHeader(rfsInput.getHeader());
						while((rawFrame = (RawAudioFrame)rfsInput.recvFrame()) != null) {
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
				int returnVal = fc.showOpenDialog(AudioConversionUI.this);
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

		frame.add(new AudioConversionUI());
		frame.pack();
		frame.setBounds(400, 250, 500, 280);
		frame.setResizable(false);
		frame.setVisible(true);
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

			AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream,
					audioFormat, audioData.length / audioFormat.getFrameSize());

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
		AudioConversionUI audioConversionUI = new AudioConversionUI();
		audioConversionUI.createAndShowGUI();
	}
}
