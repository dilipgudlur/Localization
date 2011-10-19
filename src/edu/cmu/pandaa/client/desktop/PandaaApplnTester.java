package edu.cmu.pandaa.client.desktop;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import edu.cmu.pandaa.shared.stream.FileStream;

import java.nio.ByteBuffer;

public class PandaaApplnTester extends JPanel implements ActionListener {
	static private final String newline = "\n";
	JButton openButton, saveButton;
	JButton captureButton, stopButton, playButton;
	JTextArea log;
	JFileChooser fc;
	short fileContents[];
	FileInputStream fis = null;
	DataInputStream dis = null;

	boolean stopCapture = false;
	ByteArrayOutputStream byteArrayOutputStream;
	AudioFormat audioFormat;
	TargetDataLine targetDataLine;
	AudioInputStream audioInputStream;
	SourceDataLine sourceDataLine;

	public PandaaApplnTester() {
		super(new BorderLayout());

		log = new JTextArea(5, 20);

		log.setMargin(new Insets(5, 5, 5, 5));
		log.setEditable(false);
		JScrollPane logScrollPane = new JScrollPane(log);

		fc = new JFileChooser();
		/*
		 * openButton = new JButton("Open a File...");
		 * openButton.addActionListener(this);
		 * 
		 * saveButton = new JButton("Save a File...");
		 * saveButton.addActionListener(this);
		 */
		captureButton = new JButton("Capture Audio");
		captureButton.addActionListener(this);

		stopButton = new JButton("Stop Audio Capture");
		stopButton.addActionListener(this);

		playButton = new JButton("Play Audio");
		playButton.addActionListener(this);

		captureButton.setEnabled(true);
		stopButton.setEnabled(false);
//		playButton.setEnabled(false);

		JPanel buttonPanel = new JPanel();
		// buttonPanel.add(openButton);
		// buttonPanel.add(saveButton);

		buttonPanel.add(captureButton);
		buttonPanel.add(stopButton);
		buttonPanel.add(playButton);
		add(buttonPanel, BorderLayout.PAGE_START);
		add(logScrollPane, BorderLayout.CENTER);
	}

	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == captureButton) {
			captureButton.setEnabled(false);
			stopButton.setEnabled(true);
			playButton.setEnabled(false);
			// Capture input data from the
			// microphone until the Stop button is
			// clicked.
			captureAudio();
		} else if (e.getSource() == stopButton) {
			captureButton.setEnabled(true);
			stopButton.setEnabled(false);
			playButton.setEnabled(true);
			// Terminate the capturing of input data
			// from the microphone.
			stopCapture = true;
		} else if(e.getSource() == playButton) {
//			playAudio();
			WavConvertor wavPlayer = new WavConvertor("C:\\Users\\Divya_PKV\\Music\\industry_mad.wav");
			wavPlayer.getBytes();
			FileStream fs = new FileStream("C:\\Users\\Divya_PKV\\Music\\industry_mad_in_frames.wav");
			wavPlayer.saveInFrameFormat(fs);
			byte[] audioData = wavPlayer.readFromFrameFormat(fs);
			wavPlayer.playAudio(audioData);
			System.out.println("Saved audio in frame format: " + "\n" + wavPlayer.getSummary());
		} else if (e.getSource() == openButton) {
			int returnVal = fc.showOpenDialog(PandaaApplnTester.this);

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				int index = 0;
				File file = fc.getSelectedFile();
				try {
					fis = new FileInputStream(file);
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				dis = new DataInputStream(fis);
				log.append("Opening: " + file.getPath() + "." + newline);
				fileContents = new short[(int) file.length()];
				try {
					while ((fileContents[index++] = dis.readShort()) != -1) {
						System.out.println(fileContents[index - 1]);
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					// e1.printStackTrace();
				}

				System.out.println(index + " " + file.length() + " "
						+ fileContents.length);
				// new AudioCapture(file);
				AePlayWave wavPlayer = new AePlayWave(file.getAbsolutePath());
				wavPlayer.start();
				System.out.println("Created new AudioCapture object");
			} else {
				log.append("Open command cancelled by user." + newline);
			}
			log.setCaretPosition(log.getDocument().getLength());

		} else if (e.getSource() == saveButton) {
			int returnVal = fc.showSaveDialog(PandaaApplnTester.this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				log.append("Saving: " + file.getName() + "." + newline);
			} else {
				log.append("Save command cancelled by user." + newline);
			}
			log.setCaretPosition(log.getDocument().getLength());
		}
	}

	// This method captures audio input from a
	// microphone and saves it in a
	// ByteArrayOutputStream object.
	private void captureAudio() {
		try {
			Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
			System.out.println("Available mixers:");
			for (int cnt = 0; cnt < mixerInfo.length; cnt++) {
				System.out.println(mixerInfo[cnt].getName());
			}

			// Get everything set up for capture
			// audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
			// 16000.0F, 16, 1, 2, 16000.0F,
			// true);
			audioFormat = new AudioFormat(8000.0f, 16, 1, true, true);

			DataLine.Info dataLineInfo = new DataLine.Info(
					TargetDataLine.class, audioFormat);

			Mixer mixer = AudioSystem.getMixer(mixerInfo[2]);

			targetDataLine = (TargetDataLine) mixer.getLine(dataLineInfo);
			targetDataLine.open(audioFormat);
			targetDataLine.start();

			// Creating a thread to capture the microphone
			// data and start it running. It will run
			// until the Stop button is clicked.
			Thread captureThread = new CaptureThread();
			captureThread.start();
		} catch (Exception e) {
			System.out.println(e);
			System.exit(0);
		}
	}

	// This method plays back the audio data that
	// has been saved in the ByteArrayOutputStream
	private void playAudio() {
		try {
			byte audioData[] = byteArrayOutputStream.toByteArray();
			InputStream byteArrayInputStream = new ByteArrayInputStream(
					audioData);
			AudioFormat audioFormat = getAudioFormat();
			audioInputStream = new AudioInputStream(byteArrayInputStream,
					audioFormat, audioData.length / audioFormat.getFrameSize());
			DataLine.Info dataLineInfo = new DataLine.Info(
					SourceDataLine.class, audioFormat);
			sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
			sourceDataLine.open(audioFormat);
			sourceDataLine.start();

			// Create a thread to play back the data and
			// start it running. It will run until
			// all the data has been played back.
			Thread playThread = new PlayThread();
			playThread.start();
		} catch (Exception e) {
			System.out.println(e);
			System.exit(0);
		}
	}

	
	private AudioFormat getAudioFormat() {
		float sampleRate = 8000.0F;
		int sampleSizeInBits = 16;
		int channels = 1;
		boolean signed = true;
		boolean bigEndian = false;
		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed,
				bigEndian);
	}

	// Inner class to capture data from microphone
	class CaptureThread extends Thread {
		byte tempBuffer[] = new byte[10000];

		public void run() {
			byteArrayOutputStream = new ByteArrayOutputStream();
			stopCapture = false;
			try {
				while (!stopCapture) {
				
					int cnt = targetDataLine.read(tempBuffer, 0,
							tempBuffer.length);
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

	class PlayThread extends Thread {
		byte tempBuffer[] = new byte[10000];

		public void run() {
			try {
				int cnt;
				while ((cnt = audioInputStream.read(tempBuffer, 0,
						tempBuffer.length)) != -1) {
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
	
	public static final int byteArrayToInt(byte [] b, boolean bigEndian) {
		int x = 0;
		if(bigEndian)
			x = ByteBuffer.wrap(b).getInt();
		else
			x = ByteBuffer.wrap(b).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
		return x;
	}
	
	public static final byte[] intToByteArray(int x, boolean bigEndian) {
		return ByteBuffer.allocate(4).putInt(x).array();		
	}

	private static void createAndShowGUI() {
		JFrame frame = new JFrame("PandaaApplnTester");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.add(new PandaaApplnTester());
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				UIManager.put("swing.boldMetal", Boolean.FALSE);
				createAndShowGUI();
			}
		});
	}
}