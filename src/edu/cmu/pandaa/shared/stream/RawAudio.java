package edu.cmu.pandaa.shared.stream;

import java.io.Serializable;

import edu.cmu.pandaa.client.shared.audio.AudioTimeStamp;
import edu.cmu.pandaa.shared.stream.GenericFrame.Frame;
import edu.cmu.pandaa.shared.stream.GenericFrame.Header;

public class RawAudio {
	public static class RawAudioFrame extends Frame implements Serializable {
		public Short[] audioData;

		public RawAudioFrame(int frameLength) {
			audioData = new Short[frameLength];
		}
	}

	public static class RawAudioHeader extends Header implements Serializable {

		public RawAudioHeader(int frameTime) {
			startTime = AudioTimeStamp.getCurrentTime();
			this.frameTime = frameTime;
		}
	}
}
