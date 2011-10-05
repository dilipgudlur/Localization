package edu.cmu.pandaa.shared.stream;

import java.io.Serializable;

import edu.cmu.pandaa.shared.stream.GenericFrame.Frame;
import edu.cmu.pandaa.shared.stream.GenericFrame.Header;

public class FeatureData {

	public static class FeatureFrame extends Frame implements Serializable {
		public int[] offset;
		public short[] peaks;
	}

	public static class FeatureHeader extends Header implements Serializable {

	}

}
