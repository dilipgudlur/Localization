package edu.cmu.pandaa.shared.stream.header;

import java.io.Serializable;


public class FeatureHeader extends StreamHeader implements Serializable {

	public static class FeatureFrame extends StreamFrame implements Serializable {
		public int[] offsets;  /* TAP: What units are these in? Needs a comment */
	    public short[] peaks;

	}
}