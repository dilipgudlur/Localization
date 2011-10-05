package edu.cmu.pandaa.shared.stream;

import java.io.Serializable;

public class GenericFrame {
	public static class Frame implements Serializable {
		public int seqNum; // automatically set by FrameStream implementation
	}

	public static class Header implements Serializable {
		public long startTime; // client start time, ala
								// System.currentTimeMillis()
		public long frameTime; // duration of each frame, measured in ms
	}
}
