package edu.cmu.pandaa.shared.stream.header;

import java.io.Serializable;

import edu.cmu.pandaa.client.shared.audio.AudioTimeStamp;

public class RawAudioHeader extends StreamHeader implements Serializable {

	public RawAudioHeader(int frameTime) {
		startTime = AudioTimeStamp.getCurrentTime();
		this.frameTime = frameTime;
	}

	public static class RawAudioFrame extends StreamFrame implements Serializable {
		public short[] audioData;

		public RawAudioFrame(int frameLength) {
			audioData = new short[frameLength];
		}
	}
}
