package edu.cmu.pandaa.header;

import edu.cmu.pandaa.stream.DistanceFileStream;
import edu.cmu.pandaa.stream.FileStream;
import edu.cmu.pandaa.stream.ImpulseFileStream;

import java.util.List;
import java.util.logging.Level;

public class DistanceHeader extends StreamHeader {
  private final int rollingWindow;
  private final String[] deviceIds; // the devices providing feature/impulse data for distance calculation

  public DistanceHeader(StreamHeader in) {
    super(in);
    rollingWindow = 1;
    deviceIds = super.getIds();
  }

  public DistanceHeader(StreamHeader in, int rollingWindow) {
    super(in);
    this.rollingWindow = rollingWindow;
    deviceIds = super.getIds();
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

    DistanceFrame(StreamFrame prototype, double[] deltas, double[] magnitudes, double[] raw) {
      super(prototype);
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
    double[] empty = deltas == null ? null : new double[deltas.length];
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

  public DistanceFrame makeFrame(List<Double> deltas, List<Double> magnitudes, List<Double> rawValues) {
    int size = deltas.size();
    double[] darray = new double[size];
    double[] marray = new double[size];
    double[] values = new double[size];
    for (int i = 0; i < size; i++) {
      darray[i] = deltas.get(i);
      marray[i] = magnitudes.get(i);
      values[i] = rawValues.get(i);
    }
    return new DistanceFrame(darray, marray, values);
  }

  public DistanceFrame makeFrame(StreamFrame prototype, List<Double> deltas, List<Double> magnitudes, List<Double> raw) {
    int size = deltas.size();
    double[] darray = new double[size];
    double[] marray = new double[size];
    double[] values = new double[size];
    for (int i = 0; i < size; i++) {
      darray[i] = deltas.get(i);
      marray[i] = magnitudes.get(i);
    }
    return new DistanceFrame(prototype, darray, marray, values);
  }

  public FileStream createOutput()  throws Exception {
    return new DistanceFileStream();
  }
}
