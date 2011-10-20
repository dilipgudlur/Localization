package edu.cmu.pandaa.header;

import java.io.Serializable;


public class ImpulseHeader extends StreamHeader implements Serializable {

  public ImpulseHeader(String id, long startTime, int frameTime) {
    super(id, startTime, frameTime);
  }

  public class ImpulseFrame extends StreamFrame implements Serializable {
    public int[] peakOffsets;  // nanosecond of the peak in one frame
    public byte[] peakMagnitudes; // magnitude information for each peak

    public ImpulseFrame(int seq, int[] peaks, byte[] magnitudes) {
      super(seq);
      peakOffsets = peaks;
      peakMagnitudes = magnitudes;
    }

    public ImpulseFrame(int[] peaks, byte[] magnitudes) {
      peakOffsets = peaks;
      peakMagnitudes = magnitudes;
    }
  }

  public ImpulseFrame makeFrame(int[] peaks, byte[] mags) {
    return new ImpulseFrame(peaks, mags);
  }

  public ImpulseFrame makeFrame(int seq, int[] peaks, byte[] mags) {
    return new ImpulseFrame(seq, peaks, mags);
  }
}
