package edu.cmu.pandaa.shared.stream.header;

import java.io.Serializable;


public class FeatureHeader extends StreamHeader implements Serializable {
	
	public static class FeatureFrame extends StreamFrame implements Serializable {
		public int[] peakOffsets;  // nanosecond of the peak in one frame
	    public short[] peakMagnitudes; // magnitude information for each peak

	}
}
