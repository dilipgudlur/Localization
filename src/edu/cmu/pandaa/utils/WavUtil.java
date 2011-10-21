package edu.cmu.pandaa.utils;

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
	private final long 	DEFAULT_CHANNELS 		= 2; //STEREO
	private final long 	DEFAULT_SAMPLING_RATE 	= 8000;
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
	
	public void setData(byte[] data) {
		audioData = data;
	}

}
