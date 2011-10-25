package edu.cmu.pandaa.header;

import java.io.Serializable;


public class ImpulseHeader extends StreamHeader implements Serializable {
  public int rollingWindow = 1;

  public ImpulseHeader(StreamHeader src) {
    super(src);
  }

  public ImpulseHeader(String id, long startTime, int frameTime) {
    super(id, startTime, frameTime);
  }

  public class ImpulseFrame extends StreamFrame implements Serializable {
    public int[] peakOffsets;  // nanosecond of the peak in one frame
    public short[] peakMagnitudes; // magnitude information for each peak

    public ImpulseFrame(int seq, int[] peaks, short[] magnitudes) {
      super(seq);
      peakOffsets = peaks;
      peakMagnitudes = magnitudes;
    }

    public ImpulseFrame(int[] peaks, short[] magnitudes) {
      peakOffsets = peaks;
      peakMagnitudes = magnitudes;
    }
  }

  public ImpulseFrame makeFrame(int[] peaks, short[] mags) {
    return new ImpulseFrame(peaks, mags);
  }

  public ImpulseFrame makeFrame(int seq, int[] peaks, short[] mags) {
    return new ImpulseFrame(seq, peaks, mags);
  }
}
