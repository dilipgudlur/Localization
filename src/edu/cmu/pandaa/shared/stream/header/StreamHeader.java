package edu.cmu.pandaa.shared.stream.header;

import java.io.Serializable;

public class StreamHeader implements Serializable {
	public String id; // device ID (hostname, IP address, whatever)
	public long startTime; // client start time, ala System.currentTimeMillis()
	public long frameTime; // duration of each frame, measured in ms

	public static class StreamFrame implements Serializable {
		public int seqNum; // automatically set by FrameStream implementation
	}
}