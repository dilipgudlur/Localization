package edu.cmu.pandaa.frame;

public class DistanceHeader extends StreamHeader {
  public String[] deviceIds; // the devices providing feature/impulse data for distance calculation

  public DistanceHeader(String id, long startTime, int frameTime) {
    super(id, startTime, frameTime);
  }

  public class DistanceFrame extends StreamFrame {
    public short[] peakDeltas; // time delta for each peak (in nanoseconds)
    public double[] peakMagnitudes; // magnitude information for each peak
  }
}