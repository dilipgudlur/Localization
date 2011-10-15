package edu.cmu.pandaa.shared.stream.header;

import sun.text.normalizer.IntTrie;

import javax.sound.midi.Sequence;
import java.io.Serializable;


public class FeatureHeader extends StreamHeader implements Serializable {

  public FeatureHeader(String id, long startTime, int frameTime) {
    super(id, startTime, frameTime);
  }

  public class FeatureFrame extends StreamFrame implements Serializable {
    public int[] peakOffsets;  // nanosecond of the peak in one frame
    public short[] peakMagnitudes; // magnitude information for each peak

    public FeatureFrame(int seq, int[] peaks, short[] magnitudes) {
      super(seq);
      peakOffsets = peaks;
      peakMagnitudes = magnitudes;
    }

    public FeatureFrame(int[] peaks, short[] magnitudes) {
      peakOffsets = peaks;
      peakMagnitudes = magnitudes;
    }
  }

  public FeatureFrame makeFrame(int[] peaks, short[] mags) {
    return new FeatureFrame(peaks, mags);
  }

  public FeatureFrame makeFrame(int seq, int[] peaks, short[] mags) {
    return new FeatureFrame(seq, peaks, mags);
  }
}
