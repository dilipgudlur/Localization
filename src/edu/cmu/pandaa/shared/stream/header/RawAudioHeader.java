package edu.cmu.pandaa.shared.stream.header;

import java.io.Serializable;

public class RawAudioHeader extends StreamHeader implements Serializable {

	public int samplingRate;
	public int channelConfiguration;
	public int audioEncoding;
	
	public RawAudioHeader(String id, long startTime, int frameTime, int samplingRate, int channelConf, int audioEncoding) {
    super(id, startTime, frameTime);
		this.samplingRate = samplingRate;
		this.channelConfiguration = channelConf;
		this.audioEncoding = audioEncoding;
	}

	public class RawAudioFrame extends StreamFrame implements Serializable {
		public short[] audioData;

		public RawAudioFrame(int frameLength) {
			audioData = new short[frameLength];
		}
	}
}
