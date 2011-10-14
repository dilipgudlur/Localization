package edu.cmu.pandaa.shared.stream.header;

import java.io.Serializable;

import edu.cmu.pandaa.client.shared.audio.AudioTimeStamp;

public class RawAudioHeader extends StreamHeader implements Serializable {

	public int samplingRate;
	public int channelConfiguration;
	public int audioEncoding;
	
	public RawAudioHeader(long startTime, int frameTime, int samplingRate, int channelConf, int audioEncoding) {
		this.startTime = startTime;
		this.frameTime = frameTime;
		this.samplingRate = samplingRate;
		this.channelConfiguration = channelConf;
		this.audioEncoding = audioEncoding;
	}

	public static class RawAudioFrame extends StreamFrame implements Serializable {
		public short[] audioData;

		public RawAudioFrame(int frameLength) {
			audioData = new short[frameLength];
		}
	}
}
