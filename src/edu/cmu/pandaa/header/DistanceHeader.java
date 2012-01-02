package edu.cmu.pandaa.header;

import java.util.List;
import java.util.logging.Level;

public class DistanceHeader extends StreamHeader {
  private final int rollingWindow;
  private final String[] deviceIds; // the devices providing feature/impulse data for distance calculation

  public DistanceHeader(DistanceHeader in) {
    super(in);
    rollingWindow = in.getRollingWindow();
    deviceIds = in.getDeviceIds();
  }

  public DistanceHeader(String id, long startTime, int frameTime) {
    this(id, startTime, frameTime, 1);
  }

  public DistanceHeader(String id, long startTime, int frameTime, String[] ids) {
    this(makeId(id, ids), startTime, frameTime);
  }

  public DistanceHeader(String id, long startTime, int frameTime, String[] ids, int rolling) {
    this(makeId(id, ids), startTime, frameTime, rolling);
  }

  public DistanceHeader(String id, long startTime, int frameTime, int rolling) {
    super(id, startTime, frameTime);
    rollingWindow = rolling;
    deviceIds = super.getIds();
  }

  public int getRollingWindow() {
    return rollingWindow;
  }

  public String[] getDeviceIds() {
    return deviceIds;
  }

  public class DistanceFrame extends StreamFrame {
    public double[] peakDeltas; // time delta for each peak (micro-seconds)
    public double[] peakMagnitudes; // magnitude information for each peak
    public double[] rawValues; // unaveraged values for peaks

    DistanceFrame(double[] deltas, double[] magnitudes, double[] raw) {
      peakDeltas = deltas;
      peakMagnitudes = magnitudes;
      rawValues = raw;
    }

    DistanceFrame(int seq, double[] deltas, double[] magnitudes, double[] raw) {
      super(seq);
      peakDeltas = deltas;
      peakMagnitudes = magnitudes;
      rawValues = raw;
    }

    public String getHeaderId(int index) {
      return getId(index);
    }
  }

  public String getId(int index) {
    return deviceIds[index];
  }

  public DistanceFrame makeFrame(double[] deltas, double[] magnitudes) {
    double[] empty = new double[deltas.length];
    return new DistanceFrame(deltas, magnitudes, empty);
  }

  public DistanceFrame makeFrame(int seqNum, double[] deltas, double[] magnitudes) {
    double[] empty = new double[deltas.length];
    return new DistanceFrame(seqNum, deltas, magnitudes, empty);
  }

  public DistanceFrame makeFrame(double[] deltas, double[] magnitudes, double[] raw) {
    return new DistanceFrame(deltas, magnitudes, raw);
  }

  public DistanceFrame makeFrame(int seq, double[] deltas, double[] magnitudes, double[] raw) {
    return new DistanceFrame(seq, deltas, magnitudes, raw);
  }

  public DistanceFrame makeFrame(List<Double> deltas, List<Double> magnitudes) {
    int size = deltas.size();
    double[] darray = new double[size];
    double[] marray = new double[size];
    double[] values = new double[size];
    for (int i = 0; i < size; i++) {
      darray[i] = deltas.get(i);
      marray[i] = magnitudes.get(i);
    }
    return new DistanceFrame(darray, marray, values);
  }
}
