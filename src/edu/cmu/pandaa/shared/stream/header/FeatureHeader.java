package edu.cmu.pandaa.shared.stream.header;

import java.io.Serializable;


public class FeatureHeader extends StreamHeader implements Serializable {
	public int samplingRate;
	
	public static class FeatureFrame extends StreamFrame implements Serializable {
		public int[] offsets;  // nanosecond of the peak
	    public short[] peaks;

	}
}
