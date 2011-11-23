package edu.cmu.pandaa.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class WavUtil {
	private long chunkSize;
	private long subChunk1Size;
	private int  format;
	private long numChannels;
	private long samplingRate;
	private long byteRate;
	private int  blockAlign;
	private int  bitsPerSample;
	private long dataSize;
	public  byte[] audioData;
	
	private final String riffString 			= "RIFF";
	private final String formatString 			= "WAVE";
	private final String subChunk1String 		= "fmt ";
	private final String subChunk2String 		= "data";
	private final int 	DEFAULT_FORMAT 			= 1; //PCM
	private final long 	DEFAULT_CHANNELS 		= 1; //MONO
	private final long 	DEFAULT_SAMPLING_RATE 	= 16000;
	private final int 	DEFAULT_BITS_PER_SAMPLE = 16;
	private final long 	DEFAULT_SUBCHUNK1_SIZE 	= 16; // For PCM
	private final long 	DEFAULT_DATA_SIZE 		= 0;
	
	public WavUtil() {
		format 			= DEFAULT_FORMAT;
		numChannels 	= DEFAULT_CHANNELS;
		samplingRate 	= DEFAULT_SAMPLING_RATE;
		bitsPerSample 	= DEFAULT_BITS_PER_SAMPLE;
		subChunk1Size 	= DEFAULT_SUBCHUNK1_SIZE;
		dataSize 		= DEFAULT_DATA_SIZE;
		initializeFields();
	}
	
	public WavUtil(int format, long numChannels, long samplingRate, int bitsPerSample, long dataSize) {
		this.format 		= format;
		this.numChannels 	= numChannels;
		this.samplingRate 	= samplingRate;
		this.bitsPerSample 	= bitsPerSample;
		this.subChunk1Size 	= DEFAULT_SUBCHUNK1_SIZE;
		this.dataSize 		= dataSize;
		initializeFields();
	}
	
	private void initializeFields() {
		chunkSize 	= 36 + dataSize;
		byteRate 	= (samplingRate * numChannels * bitsPerSample) / 8;
		blockAlign 	= (int) (numChannels * bitsPerSample) / 8;
	}
	
	public int getFormat() {
		return format;
	}

	public long getNumChannels() {
		return numChannels;
	}

	public long getSamplingRate() {
		return samplingRate;
	}

	public long getByteRate() {
		return byteRate;
	}

	public int getBitsPerSample() {
		return bitsPerSample;
	}

	public long getDataSize() {
		return dataSize;
	}

	public byte[] getAudioData() {
		return audioData;
	}

	public void saveAudioData(byte[] byteArray) {
		dataSize = byteArray.length;
		chunkSize += dataSize;
		audioData = byteArray;
	}
	
	public static WavUtil readWavFile(String filePath) {
		DataInputStream inFile = null;
		byte[] tmpLong = new byte[4];
		byte[] tmpInt = new byte[2];

		long wavChunkSize = 0, wavSubChunk1Size = 0, wavDataSize = 0;
		long wavByteRate = 0, wavSamplingRate = 0;
		int wavFormat = 0, wavChannels = 0, wavBlockAlign = 0, wavBitsPerSample = 0;
		byte[] wavData;

		try {
			inFile = new DataInputStream(new FileInputStream(filePath));

			String chunkID = "" + (char) inFile.readByte() + (char) inFile.readByte()
					+ (char) inFile.readByte() + (char) inFile.readByte();

			inFile.read(tmpLong);
			wavChunkSize = DataConversionUtil.byteArrayToLong(tmpLong);

			String format = "" + (char) inFile.readByte() + (char) inFile.readByte()
					+ (char) inFile.readByte() + (char) inFile.readByte();

			String subChunk1ID = "" + (char) inFile.readByte() + (char) inFile.readByte()
					+ (char) inFile.readByte() + (char) inFile.readByte();

			inFile.read(tmpLong);
			wavSubChunk1Size = DataConversionUtil.byteArrayToLong(tmpLong);

			inFile.read(tmpInt);
			wavFormat = DataConversionUtil.byteArrayToInt(tmpInt);

			inFile.read(tmpInt);
			wavChannels = DataConversionUtil.byteArrayToInt(tmpInt);

			inFile.read(tmpLong);
			wavSamplingRate = DataConversionUtil.byteArrayToLong(tmpLong);

			inFile.read(tmpLong);
			wavByteRate = DataConversionUtil.byteArrayToLong(tmpLong);

			inFile.read(tmpInt);
			wavBlockAlign = DataConversionUtil.byteArrayToInt(tmpInt);

			inFile.read(tmpInt);
			wavBitsPerSample = DataConversionUtil.byteArrayToInt(tmpInt);

			String dataChunkID = "" + (char) inFile.readByte() + (char) inFile.readByte()
					+ (char) inFile.readByte() + (char) inFile.readByte();

			inFile.read(tmpLong);
			wavDataSize = DataConversionUtil.byteArrayToLong(tmpLong);

			wavData = new byte[(int) wavDataSize];
			inFile.read(wavData);

			inFile.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		WavUtil wavUtil = new WavUtil(wavFormat, wavChannels, wavSamplingRate, wavBitsPerSample,
				wavDataSize);
		wavUtil.audioData = wavData;
		return wavUtil;
	}
	
	public void saveWavFile(String fileName) {
		try {
			DataOutputStream outFile = new DataOutputStream(new FileOutputStream(fileName));

			outFile.writeBytes(riffString);
			outFile.write(DataConversionUtil.intToByteArray((int)chunkSize), 0, 4);
			outFile.writeBytes(formatString);
			outFile.writeBytes(subChunk1String);
			outFile.write(DataConversionUtil.intToByteArray((int) subChunk1Size), 0, 4);
			outFile.write(DataConversionUtil.shortToByteArray((short) format), 0, 2);
			outFile.write(DataConversionUtil.shortToByteArray((short) numChannels), 0, 2);
			outFile.write(DataConversionUtil.intToByteArray((int) samplingRate), 0, 4);
			outFile.write(DataConversionUtil.intToByteArray((int) byteRate), 0, 4);
			outFile.write(DataConversionUtil.shortToByteArray((short) blockAlign), 0, 2);
			outFile.write(DataConversionUtil.shortToByteArray((short) bitsPerSample), 0, 2);
			outFile.writeBytes(subChunk2String);
			outFile.write(DataConversionUtil.intToByteArray((int) dataSize), 0, 4);
			outFile.write(audioData);
			outFile.flush();
			outFile.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
