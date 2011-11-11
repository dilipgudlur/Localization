package edu.cmu.pandaa.header;

import java.io.Serializable;

import edu.cmu.pandaa.utils.DataConversionUtil;

/* 
 * RawAudioFormat to capture the WAV file format
 * Summary of data fields in WAV file
 * The canonical WAVE format starts with the RIFF header:
 *
 * 0         4   ChunkID          RIFF
 * 4         4   ChunkSize        36 + SubChunk2Size
 * 8         4   Format           WAVE
 * 
 * The "WAVE" format consists of two subchunks: "fmt " and "data":
 * The "fmt " subchunk describes the sound data's format:
 * 12        4   Subchunk1ID      "fmt "
 * 16        4   Subchunk1Size    16 for PCM. 
 * 20        2   AudioFormat      PCM = 1 
 * 22        2   NumChannels      Mono = 1, Stereo = 2, etc.
 * 24        4   SampleRate       8000, 44100, etc.
 * 28        4   ByteRate         == SampleRate * NumChannels * BitsPerSample/8
 * 32        2   BlockAlign       == NumChannels * BitsPerSample/8
 * 34        2   BitsPerSample    8 bits = 8, 16 bits = 16, etc.
 * 
 * The "data" subchunk contains the size of the data and the actual sound:
 * 36        4   Subchunk2ID      "data"
 * 40        4   Subchunk2Size    == NumSamples * NumChannels * BitsPerSample/8
 * 44        *   Data             The actual sound data.
 */

public class RawAudioHeader extends StreamHeader implements Serializable {

	long samplingRate;
	long numChannels;
	int audioFormat;
	int bitsPerSample;
	long dataSize;

	public static final int DEFAULT_FRAMETIME = 100;
	public static final int WAV_FILE_HEADER_LENGTH = 44;

	public RawAudioHeader(String id, long startTime, int frameTime) {
		super(id, startTime, frameTime);
		// ideally fill in fields with some reasonable default -- this constructor
		// is for simple testing
	}

	public RawAudioHeader(String id, long startTime, int frameTime, int audioFormat,
			long numChannels, long samplingRate, int bitsPerSample, long subChunk2Size) {
		super(id, startTime, frameTime);
		this.samplingRate = samplingRate;
		this.numChannels = numChannels;
		this.audioFormat = audioFormat;
		this.bitsPerSample = bitsPerSample;
		this.dataSize = subChunk2Size;
	}

	public long getSamplingRate() {
		return samplingRate;
	}

	public long getNumChannels() {
		return numChannels;
	}

	public int getAudioFormat() {
		return audioFormat;
	}

	public int getBitsPerSample() {
		return bitsPerSample;
	}

	public long getSubChunk2Size() {
		return dataSize;
	}

	public String toString() {
		return new String("Device ID: " + id + "\nSampling rate: " + samplingRate + "\nChannels: "
				+ numChannels + "\nAudio Format: " + audioFormat + "\nBits per sample: " + bitsPerSample
				+ "\nData size: " + dataSize);
	}

	public class RawAudioFrame extends StreamFrame implements Serializable {
		public short[] audioData;

		public RawAudioFrame(int frameLength) {
			audioData = new short[frameLength];
		}

		public short[] getAudioData() {
			return audioData;
		}

		public String toString() {
			String result = " Length = " + audioData.length + " Data: ";
			for (int i = 0; i < 100; i++) {
				result += DataConversionUtil.shortToUnsignedShortVal(audioData[i]) + " ";
			}
			return result;
		}
	}

	public RawAudioFrame makeFrame(int frameLength) {
		return new RawAudioFrame(frameLength);
	}

	public RawAudioFrame makeFrame() {
		return new RawAudioFrame((int) (frameTime * samplingRate / 1000));
	}
}
