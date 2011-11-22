package edu.cmu.pandaa.header;

import java.io.Serializable;
import java.util.ArrayList;

public class ImpulseHeader extends StreamHeader implements Serializable {
  public int rollingWindow = 1;

  public ImpulseHeader(StreamHeader src) {
    super(src);
  }

  public ImpulseHeader(String id, long startTime, int frameTime) {
    this(id, startTime, frameTime, 1);
  }

  public ImpulseHeader(String id, long startTime, int frameTime, int rolling) {
    super(id, startTime, frameTime);
    rollingWindow = rolling;
  }

  public class ImpulseFrame extends StreamFrame implements Serializable {
    public int[] peakOffsets;  // us of the peak in one frame
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

    public ImpulseFrame(ArrayList<Integer> peaks, ArrayList<Short> magnitudes) {
      int size = peaks.size();
      peakOffsets = new int[size];
      peakMagnitudes = new short[size];
      for (int i = 0;i < size; i++) {
        peakOffsets[i] = peaks.get(i);
        peakMagnitudes[i] = magnitudes.get(i);
      }
    }
  }

  public ImpulseFrame makeFrame(int[] peaks, short[] mags) {
    return new ImpulseFrame(peaks, mags);
  }

  public ImpulseFrame makeFrame(int seq, int[] peaks, short[] mags) {
    return new ImpulseFrame(seq, peaks, mags);
  }
}
