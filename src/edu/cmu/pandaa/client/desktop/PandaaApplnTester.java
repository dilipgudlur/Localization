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
		playButton.setEnabled(false);

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
			playAudio();
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
			// Get and display a list of
			// available mixers.
			Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
			System.out.println("Available mixers:");
			for (int cnt = 0; cnt < mixerInfo.length; cnt++) {
				System.out.println(mixerInfo[cnt].getName());
			}// end for loop

			// Get everything set up for capture
			// audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
			// 16000.0F, 16, 1, 2, 16000.0F,
			// true);
			audioFormat = new AudioFormat(8000.0f, 16, 1, true, true);

			DataLine.Info dataLineInfo = new DataLine.Info(
					TargetDataLine.class, audioFormat);

			System.out.println(" AKTUALNY => " + mixerInfo[2].getName());
			Mixer mixer = AudioSystem.getMixer(mixerInfo[2]);

			// Get a TargetDataLine on the selected
			// mixer.
			targetDataLine = (TargetDataLine) mixer.getLine(dataLineInfo);
			// Prepare the line for use.
			targetDataLine.open(audioFormat);
			targetDataLine.start();

			// Create a thread to capture the microphone
			// data and start it running. It will run
			// until the Stop button is clicked.
			Thread captureThread = new CaptureThread();
			captureThread.start();
		} catch (Exception e) {
			System.out.println(e);
			System.exit(0);
		}// end catch
	}// end captureAudio method

	// This method plays back the audio data that
	// has been saved in the ByteArrayOutputStream
	private void playAudio() {
		try {
			// Get everything set up for playback.
			// Get the previously-saved data into a byte
			// array object.
			byte audioData[] = byteArrayOutputStream.toByteArray();
			// Get an input stream on the byte array
			// containing the data
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
		}// end catch
	}// end playAudio

	// This method creates and returns an
	// AudioFormat object for a given set of format
	// parameters. If these parameters don't work
	// well for you, try some of the other
	// allowable parameter values, which are shown
	// in comments following the declartions.
	private AudioFormat getAudioFormat() {
		float sampleRate = 8000.0F;
		// 8000,11025,16000,22050,44100
		int sampleSizeInBits = 16;
		// 8,16
		int channels = 1;
		// 1,2
		boolean signed = true;
		// true,false
		boolean bigEndian = false;
		// true,false
		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed,
				bigEndian);
	}// end getAudioFormat

	// Inner class to capture data from microphone
	class CaptureThread extends Thread {
		// An arbitrary-size temporary holding buffer
		byte tempBuffer[] = new byte[10000];

		public void run() {
			byteArrayOutputStream = new ByteArrayOutputStream();
			stopCapture = false;
			try {// Loop until stopCapture is set by
					// another thread that services the Stop
					// button.
				while (!stopCapture) {
					// Read data from the internal buffer of
					// the data line.
					int cnt = targetDataLine.read(tempBuffer, 0,
							tempBuffer.length);
					if (cnt > 0) {
						// Save data in output stream object.
						byteArrayOutputStream.write(tempBuffer, 0, cnt);
					}// end if
				}// end while
				byteArrayOutputStream.close();
			} catch (Exception e) {
				System.out.println(e);
				System.exit(0);
			}// end catch
		}// end run
	}// end inner class CaptureThread
		// ===================================//
		// Inner class to play back the data
		// that was saved.

	class PlayThread extends Thread {
		byte tempBuffer[] = new byte[10000];

		public void run() {
			try {
				int cnt;
				// Keep looping until the input read method
				// returns -1 for empty stream.
				while ((cnt = audioInputStream.read(tempBuffer, 0,
						tempBuffer.length)) != -1) {
					if (cnt > 0) {
						// Write data to the internal buffer of
						// the data line where it will be
						// delivered to the speaker.
						sourceDataLine.write(tempBuffer, 0, cnt);
					}// end if
				}// end while
					// Block and wait for internal buffer of the
					// data line to empty.
				sourceDataLine.drain();
				sourceDataLine.close();
			} catch (Exception e) {
				System.out.println(e);
				System.exit(0);
			}// end catch
		}// end run
	}// end inner class PlayThread
	
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