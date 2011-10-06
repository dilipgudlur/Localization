package edu.cmu.pandaa.shared.stream;

import java.io.Serializable;

import edu.cmu.pandaa.shared.stream.GenericFrame.Frame;
import edu.cmu.pandaa.shared.stream.GenericFrame.Header;

/* TAP: See notes in other classes about names... just ensure parallel construction with other classes.
  Also, "Data" is way too generic -- everything is Data so that doesn't really add anything
 */
public class FeatureData {

	public static class FeatureFrame extends Frame implements Serializable {
	    public int[] offset;  /* TAP: What units are these in? Needs a comment */
	    public short[] peaks; /* TAP: either use "offsets" or "peak" -- plurality should match */
	}

	public static class FeatureHeader extends Header implements Serializable {

	}

}
