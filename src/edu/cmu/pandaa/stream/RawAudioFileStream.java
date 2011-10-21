package edu.cmu.pandaa.stream;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.RawAudioHeader.RawAudioFrame;

public class RawAudioFileStream extends FileStream {
	public RawAudioFileStream(String fileName) throws IOException {
		super(fileName);
	}
   /* DNC

	public RawAudioFileStream(String fileName, boolean overwrite)
			throws IOException {
		super(fileName, overwrite);
	}

	public boolean saveInFrameFormat(WavUtil wavUtil) {
		try {
			RawAudioHeader rawAudioHeader = new RawAudioHeader(
					System.currentTimeMillis(),
					RawAudioHeader.DEAFULT_FRAMETIME, wavUtil.getFormat(),
					wavUtil.getNumChannels(), wavUtil.getSamplingRate(),
					wavUtil.getBitsPerSample(), wavUtil.getDataSize());
			setHeader(rawAudioHeader);
			
			int frameLength = (int) (wavUtil.getSamplingRate() / 1000) * 100;
			RawAudioFrame audioFrame = null;
			int audioIndex = 0;
			for (int idxBuffer = 0; idxBuffer < wavUtil.getDataSize(); ++idxBuffer) {

				// When a frame is full send it to the stream and create a new
				// frame
				if (audioFrame == null || audioIndex >= frameLength) {
					if (audioFrame != null) {
						try {
							sendFrame(audioFrame);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					audioFrame = rawAudioHeader.makeFrame(frameLength);
					audioIndex = 0;
				}
				audioFrame.audioData[audioIndex++] = wavUtil.audioData[idxBuffer];
			}
			if (audioIndex > 0) {
				try {
					sendFrame(audioFrame);
				} catch (Exception e) {
					e.printStackTrace();
				}
				audioIndex = 0;
				audioFrame = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public byte[] readFromFrameFormat() {
		byte[] audioData = null;
		try {
			RawAudioHeader audioHeader = (RawAudioHeader) getHeader();
			RawAudioFrame audioFrame = null;

			long wavDataSize = audioHeader.getSubChunk2Size();
			audioData = new byte[(int) wavDataSize];
			int numSamples = 0;
			boolean dataComplete = false;
			try {
				while ((audioFrame = (RawAudioFrame) recvFrame()) != null) {
					byte[] data = audioFrame.getAudioData();
					for (int i = 0; i < data.length; i++) {
						audioData[numSamples] = data[i];
						if (numSamples++ == wavDataSize - 1) {
							System.out.println("Number of samples: "
									+ numSamples);
							dataComplete = true;
							break;
						}
					}
					if (dataComplete)
						break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("My Data size length: " + audioData.length);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return audioData;
	}

	public WavUtil readWavFile(String filePath) {
		DataInputStream inFile = null;
		byte[] tmpLong = new byte[4];
		byte[] tmpInt = new byte[2];

		long wavChunkSize = 0, wavSubChunk1Size = 0, wavDataSize = 0;
		long wavByteRate = 0, wavSamplingRate = 0;
		int wavFormat = 0, wavChannels = 0, wavBlockAlign = 0, wavBitsPerSample = 0;
		byte[] wavData;

		try {
			inFile = new DataInputStream(new FileInputStream(filePath));

			String chunkID = "" + (char) inFile.readByte()
					+ (char) inFile.readByte() + (char) inFile.readByte()
					+ (char) inFile.readByte();

			inFile.read(tmpLong);
			wavChunkSize = byteArrayToLong(tmpLong);

			String format = "" + (char) inFile.readByte()
					+ (char) inFile.readByte() + (char) inFile.readByte()
					+ (char) inFile.readByte();

			String subChunk1ID = "" + (char) inFile.readByte()
					+ (char) inFile.readByte() + (char) inFile.readByte()
					+ (char) inFile.readByte();

			inFile.read(tmpLong);
			wavSubChunk1Size = byteArrayToLong(tmpLong);

			inFile.read(tmpInt);
			wavFormat = byteArrayToInt(tmpInt);

			inFile.read(tmpInt);
			wavChannels = byteArrayToInt(tmpInt);

			inFile.read(tmpLong);
			wavSamplingRate = byteArrayToLong(tmpLong);

			inFile.read(tmpLong);
			wavByteRate = byteArrayToLong(tmpLong);

			inFile.read(tmpInt);
			wavBlockAlign = byteArrayToInt(tmpInt);

			inFile.read(tmpInt);
			wavBitsPerSample = byteArrayToInt(tmpInt);

			String dataChunkID = "" + (char) inFile.readByte()
					+ (char) inFile.readByte() + (char) inFile.readByte()
					+ (char) inFile.readByte();

			inFile.read(tmpLong);
			wavDataSize = byteArrayToLong(tmpLong);

			wavData = new byte[(int) wavDataSize];
			inFile.read(wavData);
			

			inFile.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		WavUtil wavUtil = new WavUtil(wavFormat, wavChannels, wavSamplingRate,
				wavBitsPerSample, wavDataSize);
		wavUtil.audioData = wavData;
		return wavUtil;
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
	*/
}
