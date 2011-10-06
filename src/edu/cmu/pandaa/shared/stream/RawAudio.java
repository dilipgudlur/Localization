package edu.cmu.pandaa.shared.stream;

import java.io.Serializable;

import edu.cmu.pandaa.client.shared.audio.AudioTimeStamp;
import edu.cmu.pandaa.shared.stream.GenericFrame.Frame;
import edu.cmu.pandaa.shared.stream.GenericFrame.Header;

/* TAP: Name should have a meaningful suffic, like RawAudioChunks or something */
public class RawAudio {
	public static class RawAudioFrame extends Frame implements Serializable {
		public short[] audioData;

		public RawAudioFrame(int frameLength) {
			audioData = new short[frameLength];
		}
	}

	public static class RawAudioHeader extends Header implements Serializable {
	    /* TAP: What about sampling rate? Where are the data fields specific to raw audio? */

		public RawAudioHeader(int frameTime) {
		    /* TAP: these should not be initialized here since it's a propery of Header */
		    /* TAP: Also, shouldn't initialize to getCurrentTime, since it might be from a file or something, pass as arf */
			startTime = AudioTimeStamp.getCurrentTime();
			this.frameTime = frameTime;
		}
	}
}
