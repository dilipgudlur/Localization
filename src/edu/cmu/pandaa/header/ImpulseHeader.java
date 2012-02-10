package edu.cmu.pandaa.header;

import edu.cmu.pandaa.stream.FileStream;
import edu.cmu.pandaa.stream.GeometryFileStream;
import edu.cmu.pandaa.stream.ImpulseFileStream;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ImpulseHeader extends StreamHeader implements Serializable {
  public int rollingWindow = 1;

  public ImpulseHeader(StreamHeader src, int rollingWindow) {
    super(src);
    this.rollingWindow = rollingWindow;
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

    public ImpulseFrame prepend(ImpulseFrame a) {
      ImpulseFrame b = this;
      int[] peaks = new int[a.peakOffsets.length + b.peakOffsets.length];
      short[] mags = new short[a.peakMagnitudes.length + b.peakMagnitudes.length];
      for (int i = 0; i < a.peakOffsets.length; i++) {
        peaks[i] = a.peakOffsets[i] - frameTime * 1000; // convert from ms to us
      }
      System.arraycopy(a.peakMagnitudes, 0, mags, 0, a.peakMagnitudes.length);
      System.arraycopy(b.peakOffsets, 0, peaks, a.peakOffsets.length, b.peakOffsets.length);
      System.arraycopy(b.peakMagnitudes, 0, mags, a.peakOffsets.length, b.peakMagnitudes.length);
      return new ImpulseFrame(b.seqNum, peaks, mags);
    }
  }

  public ImpulseFrame makeFrame(int[] peaks, short[] mags) {
    return new ImpulseFrame(peaks, mags);
  }

  public ImpulseFrame makeFrame(int seq, int[] peaks, short[] mags) {
    return new ImpulseFrame(seq, peaks, mags);
  }

  public ImpulseFrame makeFrame(List<Integer> peaks, List<Short> magnitudes) {
    int size = peaks.size();
    int[] peakOffsets = new int[size];
    short[] peakMagnitudes = new short[size];
    for (int i = 0;i < size; i++) {
      peakOffsets[i] = peaks.get(i);
      peakMagnitudes[i] = magnitudes.get(i);
    }
    return new ImpulseFrame(peakOffsets, peakMagnitudes);
  }

  public FileStream createOutput()  throws Exception {
    return new ImpulseFileStream();
  }
}
