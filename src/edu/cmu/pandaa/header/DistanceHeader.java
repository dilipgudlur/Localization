package edu.cmu.pandaa.header;

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

    DistanceFrame(double[] deltas, double[] magnitudes) {
      peakDeltas = deltas;
      peakMagnitudes = magnitudes;
    }

    public String getHeaderId(int index) {
      return getId(index);
    }
  }

  public String getId(int index) {
    return deviceIds[index];
  }

  public DistanceFrame makeFrame(double[] deltas, double[] magnitudes) {
    return new DistanceFrame(deltas, magnitudes);
  }
}
