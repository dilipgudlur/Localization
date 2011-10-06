package edu.cmu.pandaa.shared.stream;

import java.io.Serializable;

/* TAP: wrapper class names are not consistent. Right now there is:
  FeatureData
  GenericFrame
  RawAudio
  -- they should all have the same suffix
  also, the inner classes should have parity with the wrapper class.
 */
public class GenericFrame {
	public static class Frame implements Serializable {
		public int seqNum; // automatically set by FrameStream implementation
	}

	public static class Header implements Serializable {
	    String id; // device ID (hostname, IP address, whatever)
		public long startTime; // client start time, ala System.currentTimeMillis()
		public long frameTime; // duration of each frame, measured in ms
	}
}
