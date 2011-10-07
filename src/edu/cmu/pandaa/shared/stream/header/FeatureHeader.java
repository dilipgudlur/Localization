package edu.cmu.pandaa.shared.stream.header;

import java.io.Serializable;


public class FeatureHeader extends StreamHeader implements Serializable {

	public static class FeatureFrame extends StreamFrame implements Serializable {
		public int[] offsets;  // the number of samples that have been processed so far
	    public short[] peaks;

	}
}