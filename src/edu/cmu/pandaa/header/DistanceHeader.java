package edu.cmu.pandaa.header;

public class DistanceHeader extends StreamHeader {
  public String[] deviceIds; // the devices providing feature/impulse data for distance calculation

  public DistanceHeader(String id, long startTime, int frameTime, String[] ids) {
    super(id, startTime, frameTime);
    deviceIds = ids;
  }

  public class DistanceFrame extends StreamFrame {
    public short[] peakDeltas; // time delta for each peak (in nanoseconds)
    public double[] peakMagnitudes; // magnitude information for each peak
    
    DistanceFrame(short[] deltas, double[] magnitudes) {
      peakDeltas = deltas;
      peakMagnitudes = magnitudes;
    }
  }
  
  public DistanceFrame makeFrame(short[] deltas, double[] magnitudes) {
    return new DistanceFrame(deltas, magnitudes);
  }
}
