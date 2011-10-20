package edu.cmu.pandaa.desktop;

import java.applet.Applet;
import java.applet.AudioClip;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import edu.cmu.pandaa.frame.RawAudioHeader;
import edu.cmu.pandaa.frame.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.stream.FileStream;

/**
 * This class handles the reading, writing, and playing of wav files. It is also
 * capable of converting the file to its raw byte [] form.
 * 
 * based on code by Evan Merz modified by Dan Vargo
 * 
 * @author dvargo
 */
public class WavConvertor {
	/*
	 * WAV File Specification FROM
	 * http://ccrma.stanford.edu/courses/422/projects/WaveFormat/ The canonical
	 * WAVE format starts with the RIFF header: 0 4 ChunkID Contains the letters
	 * "RIFF" in ASCII form (0x52494646 big-endian form). 4 4 ChunkSize 36 +
	 * SubChunk2Size, or more precisely: 4 + (8 + SubChunk1Size) + (8 +
	 * SubChunk2Size) This is the size of the rest of the chunk following this
	 * number. This is the size of the entire file in bytes minus 8 bytes for
	 * the two fields not included in this count: ChunkID and ChunkSize. 8 4
	 * Format Contains the letters "WAVE" (0x57415645 big-endian form).
	 * 
	 * The "WAVE" format consists of two subchunks: "fmt " and "data": The
	 * "fmt " subchunk describes the sound data's format: 12 4 Subchunk1ID
	 * Contains the letters "fmt " (0x666d7420 big-endian form). 16 4
	 * Subchunk1Size 16 for PCM. This is the size of the rest of the Subchunk
	 * which follows this number. 20 2 AudioFormat PCM = 1 (i.e. Linear
	 * quantization) Values other than 1 indicate some form of compression. 22 2
	 * NumChannels Mono = 1, Stereo = 2, etc. 24 4 SampleRate 8000, 44100, etc.
	 * 28 4 ByteRate == SampleRate * NumChannels * BitsPerSample/8 32 2
	 * BlockAlign == NumChannels * BitsPerSample/8 The number of bytes for one
	 * sample including all channels. I wonder what happens when this number
	 * isn't an integer? 34 2 BitsPerSample 8 bits = 8, 16 bits = 16, etc.
	 * 
	 * The "data" subchunk contains the size of the data and the actual sound:
	 * 36 4 Subchunk2ID Contains the letters "data" (0x64617461 big-endian
	 * form). 40 4 Subchunk2Size == NumSamples * NumChannels * BitsPerSample/8
	 * This is the number of bytes in the data. You can also think of this as
	 * the size of the read of the subchunk following this number. 44 * Data The
	 * actual sound data.
	 * 
	 * 
	 * The thing that makes reading wav files tricky is that java has no
	 * unsigned types. This means that the binary data can't just be read and
	 * cast appropriately. Also, we have to use larger types than are normally
	 * necessary.
	 * 
	 * In many languages including java, an integer is represented by 4 bytes.
	 * The issue here is that in most languages, integers can be signed or
	 * unsigned, and in wav files the integers are unsigned. So, to make sure
	 * that we can store the proper values, we have to use longs to hold
	 * integers, and integers to hold shorts.
	 * 
	 * Then, we have to convert back when we want to save our wav data.
	 * 
	 * It's complicated, but ultimately, it just results in a few extra
	 * functions at the bottom of this file. Once you understand the issue,
	 * there is no reason to pay any more attention to it.
	 * 
	 * ALSO:
	 * 
	 * This code won't read ALL wav files. This does not use to full
	 * specification. It just uses a trimmed down version that most wav files
	 * adhere to.
	 */

	ByteArrayOutputStream byteArrayOutputStream;
	AudioFormat audioFormat;
	TargetDataLine targetDataLine;
	AudioInputStream audioInputStream;
	SourceDataLine sourceDataLine;
	float frequency = 8000.0F; // 8000,11025,16000,22050,44100
	int samplesize = 16;
	private String myPath;
	private long myChunkSize;
	private long mySubChunk1Size;
	private int myFormat;
	private long myChannels;
	private long mySampleRate;
	private long myByteRate;
	private int myBlockAlign;
	private int myBitsPerSample;
	private long myDataSize;
	// I made this public so that you can toss whatever you want in here
	// maybe a recorded buffer, maybe just whatever you want
	public byte[] myData;

	public WavConvertor() {
		myPath = "";
	}

	// constructor takes a wav path
	public WavConvertor(String tmpPath) {
		myPath = tmpPath;
	}

	// get/set for the Path property
	public String getPath() {
		return myPath;
	}

	public void setPath(String newPath) {
		myPath = newPath;
	}

	// read a wav file into this class
	public boolean read() {
		DataInputStream inFile = null;
		myData = null;
		byte[] tmpLong = new byte[4];
		byte[] tmpInt = new byte[2];

		try {
			inFile = new DataInputStream(new FileInputStream(myPath));

			String chunkID = "" + (char) inFile.readByte()
					+ (char) inFile.readByte() + (char) inFile.readByte()
					+ (char) inFile.readByte();

			inFile.read(tmpLong); // read the ChunkSize
			myChunkSize = byteArrayToLong(tmpLong);

			String format = "" + (char) inFile.readByte()
					+ (char) inFile.readByte() + (char) inFile.readByte()
					+ (char) inFile.readByte();

			// print what we've read so far
			// System.out.println("chunkID:" + chunkID + " chunk1Size:" +
			// myChunkSize + " format:" + format); // for debugging only

			String subChunk1ID = "" + (char) inFile.readByte()
					+ (char) inFile.readByte() + (char) inFile.readByte()
					+ (char) inFile.readByte();

			inFile.read(tmpLong); // read the SubChunk1Size
			mySubChunk1Size = byteArrayToLong(tmpLong);

			inFile.read(tmpInt); // read the audio format. This should be 1 for
									// PCM
			myFormat = byteArrayToInt(tmpInt);

			inFile.read(tmpInt); // read the # of channels (1 or 2)
			myChannels = byteArrayToInt(tmpInt);

			inFile.read(tmpLong); // read the samplerate
			mySampleRate = byteArrayToLong(tmpLong);

			inFile.read(tmpLong); // read the byterate
			myByteRate = byteArrayToLong(tmpLong);

			inFile.read(tmpInt); // read the blockalign
			myBlockAlign = byteArrayToInt(tmpInt);

			inFile.read(tmpInt); // read the bitspersample
			myBitsPerSample = byteArrayToInt(tmpInt);

			// print what we've read so far
			// System.out.println("SubChunk1ID:" + subChunk1ID +
			// " SubChunk1Size:" + mySubChunk1Size + " AudioFormat:" + myFormat
			// + " Channels:" + myChannels + " SampleRate:" + mySampleRate);

			// read the data chunk header - reading this IS necessary, because
			// not all wav files will have the data chunk here - for now, we're
			// just assuming that the data chunk is here
			String dataChunkID = "" + (char) inFile.readByte()
					+ (char) inFile.readByte() + (char) inFile.readByte()
					+ (char) inFile.readByte();

			inFile.read(tmpLong); // read the size of the data
			myDataSize = byteArrayToLong(tmpLong);

			// read the data chunk
			myData = new byte[(int) myDataSize];
			inFile.read(myData);

			// close the input stream
			inFile.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true; // this should probably be something more descriptive
	}

	// write out the wav file
	public boolean save() {
		try {
			DataOutputStream outFile = new DataOutputStream(
					new FileOutputStream(myPath + "temp"));

			// write the wav file per the wav file format
			outFile.writeBytes("RIFF");
			outFile.write(intToByteArray((int) myChunkSize), 0, 4);
			outFile.writeBytes("WAVE");
			outFile.writeBytes("fmt ");
			outFile.write(intToByteArray((int) mySubChunk1Size), 0, 4);
			outFile.write(shortToByteArray((short) myFormat), 0, 2);
			outFile.write(shortToByteArray((short) myChannels), 0, 2);
			outFile.write(intToByteArray((int) mySampleRate), 0, 4);
			outFile.write(intToByteArray((int) myByteRate), 0, 4);
			outFile.write(shortToByteArray((short) myBlockAlign), 0, 2);
			outFile.write(shortToByteArray((short) myBitsPerSample), 0, 2);
			outFile.writeBytes("data");
			outFile.write(intToByteArray((int) myDataSize), 0, 4);
			outFile.write(myData);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return false;
		}

		return true;
	}

	public byte[] readFromFrameFormat(FileStream fs) {
		byte[] myAudioData = null;
		try {
			RawAudioHeader audioHeader = (RawAudioHeader)fs.getHeader();
			RawAudioFrame audioFrame = null;
			String chunkID = "RIFF";
			myChunkSize = 36 + audioHeader.getSubChunk2Size();

			String format = "WAVE";
			String subChunk1ID = "fmt ";
			mySubChunk1Size = 16;
			myFormat = audioHeader.getAudioFormat();
			myChannels = audioHeader.getNumChannels();
			mySampleRate = audioHeader.getSamplingRate();
			myBitsPerSample = audioHeader.getBitsPerSample();
			myByteRate = (mySampleRate * myChannels * myBitsPerSample) / 8;
			myBlockAlign = (int) (myChannels * myBitsPerSample) / 8;
			String dataChunkID = "data";
			myDataSize = audioHeader.getSubChunk2Size();
			myAudioData = new byte[(int)myDataSize];
			int numSamples = 0;
			boolean dataComplete = false;
			try {
				while ((audioFrame = (RawAudioFrame) fs.recvFrame()) != null) {
					byte[] audioData = audioFrame.getAudioData();
					for (int i = 0; i < audioData.length; i++) {
						myAudioData[numSamples] = audioData[i];
						if (numSamples++ == myDataSize-1) {
							System.out.println("Number of samples: "
									+ numSamples);
							dataComplete = true;
							break;
						}
					}
					if(dataComplete)
						break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("My Data size length: " + myData.length);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return myAudioData;
	}

	// write out the wav file
	public boolean saveInFrameFormat(FileStream fs) {
		try {
			RawAudioHeader rawAudioHeader = new RawAudioHeader(
					System.currentTimeMillis(), 100, myFormat,
					(int) myChannels, mySampleRate, myBitsPerSample, myDataSize);
			fs.setHeader(rawAudioHeader);
			int frameLength = (int) (mySampleRate / 1000) * 100;
			System.out.println("Number of frames: "
					+ ((float) myDataSize / (float) frameLength));
			RawAudioFrame audioFrame = null;
			int audioIndex = 0, numFrames = 0;
			for (int idxBuffer = 0; idxBuffer < myDataSize; ++idxBuffer) {

				// When a frame is full send it to the stream and create a new
				// frame
				if (audioFrame == null || audioIndex >= frameLength) {
					if (audioFrame != null) {
						try {
							fs.sendFrame(audioFrame);
							numFrames++;
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					audioFrame = rawAudioHeader.makeFrame(frameLength);
					audioIndex = 0;
				}
				audioFrame.audioData[audioIndex++] = myData[idxBuffer];
			}
			if (audioIndex > 0) {
				try {
					fs.sendFrame(audioFrame);
					numFrames++;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				audioIndex = 0;
				audioFrame = null;
			}
			System.out
					.println("Number of frames written to file: " + numFrames);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	// return a printable summary of the wav file
	public String getSummary() {
		// String newline = System.getProperty("line.separator");
		String newline = "\n";
		String summary = "Format: " + myFormat + newline + "Channels: "
				+ myChannels + newline + "SampleRate: " + mySampleRate
				+ newline + "ByteRate: " + myByteRate + newline
				+ "BlockAlign: " + myBlockAlign + newline + "BitsPerSample: "
				+ myBitsPerSample + newline + "DataSize: " + myDataSize + "";
		return summary;
	}

	public byte[] getBytes() {
		read();
		return myData;
	}

	/**
	 * Plays back audio stored in the byte array using an audio format given by
	 * freq, sample rate, ect.
	 * 
	 * @param data
	 *            The byte array to play
	 */
	public void playAudio(byte[] data) {
		try {
			byte audioData[] = data;
			// Get an input stream on the byte array containing the data
			InputStream byteArrayInputStream = new ByteArrayInputStream(
					audioData);
			// AudioFormat audioFormat = getAudioFormat();
			AudioFormat audioFormat = new AudioFormat(mySampleRate, samplesize,
					2, true, false);
			audioInputStream = new AudioInputStream(byteArrayInputStream,
					audioFormat, audioData.length / audioFormat.getFrameSize());
			DataLine.Info dataLineInfo = new DataLine.Info(
					SourceDataLine.class, audioFormat);
			sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
			sourceDataLine.open(audioFormat);
			sourceDataLine.start();

			// Create a thread to play back the data and start it running. It
			// will run \
			// until all the data has been played back.
			Thread playThread = new Thread(new PlayThread());
			playThread.start();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	/**
	 * This method creates and returns an AudioFormat object for a given set of
	 * format parameters. If these parameters don't work well for you, try some
	 * of the other allowable parameter values, which are shown in comments
	 * following the declarations.
	 * 
	 * @return
	 */
	private AudioFormat getAudioFormat() {
		float sampleRate = frequency;
		// 8000,11025,16000,22050,44100
		int sampleSizeInBits = samplesize;
		// 8,16
		int channels = 1;
		// 1,2
		boolean signed = true;
		// true,false
		boolean bigEndian = false;
		// true,false
		// return new AudioFormat( AudioFormat.Encoding.PCM_SIGNED, 8000.0f, 8,
		// 1, 1,
		// 8000.0f, false );

		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed,
				bigEndian);
	}

	public void playWav(String filePath) {
		try {
			AudioClip clip = Applet.newAudioClip(new File(filePath)
					.toURI().toURL());
			clip.play();
		} catch (Exception e) {
			e.printStackTrace();
			// Logger.getLogger(Wav.class.getName()).log(Level.SEVERE, null, e);
		}

	}

	// ===========================
	// CONVERT BYTES TO JAVA TYPES
	// ===========================
	public static long byteArrayToLong(byte[] b) {
		long value = 0;
		for (int i = 0; i < b.length; i++) {
			value += (b[i] & 0xff) << (8 * i);
		}
		return value;
	}

	public static final int byteArrayToInt(byte[] b) {
		int value = 0;
		for (int i = 0; i < b.length; i++) {
			value += (b[i] & 0xff) << (8 * i);
		}
		byte[] check = intToByteArray(value);
		return value;
	}

	// convert a short to a byte array
	public static byte[] shortToByteArray(short data) {
		return new byte[] { (byte) (data & 0xff), (byte) ((data >>> 8) & 0xff) };
	}

	public static final byte[] intToByteArray(int value) {
		byte[] intBytes = new byte[2];
		for (int i = 0; i < intBytes.length; i++) {
			intBytes[i] = (byte) (value >>> (8 * (i)));
		}
		return intBytes;
	}

	public static byte[] longToByteArray(long value) {
		byte[] longBytes = new byte[4];
		for (int i = 0; i < longBytes.length; i++) {
			longBytes[i] = (byte) (value >>> (8 * (i)));
		}
		return longBytes;
	}

	/**
	 * Inner class to play back the data that was saved
	 */
	class PlayThread extends Thread {

		byte tempBuffer[] = new byte[10000];

		@Override
		public void run() {
			try {
				int cnt;
				// Keep looping until the input
				// read method returns -1 for
				// empty stream.
				while ((cnt = audioInputStream.read(tempBuffer, 0,
						tempBuffer.length)) != -1) {
					if (cnt > 0) {
						// Write data to the internal
						// buffer of the data line
						// where it will be delivered
						// to the speaker.
						sourceDataLine.write(tempBuffer, 0, cnt);
					}
				}
				// Block and wait for internal
				// buffer of the data line to
				// empty.
				sourceDataLine.drain();
				sourceDataLine.close();
			} catch (Exception e) {
				System.out.println(e);
				System.exit(0);
			}
		}
	}
}
