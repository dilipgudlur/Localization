package edu.cmu.pandaa.header;

import edu.cmu.pandaa.stream.FileStream;
import edu.cmu.pandaa.stream.GeometryFileStream;
import edu.cmu.pandaa.stream.ImpulseFileStream;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
