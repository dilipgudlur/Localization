package edu.cmu.pandaa.shared.stream.header;

public class DistanceHeader extends StreamHeader {
  public String[] deviceIds; // the devices providing feature/impulse data for distance calculation
  
  public class DistanceFrame extends StreamFrame {
    public short[] peakDeltas; // time delta for each peak occurring at the two devices
    public double[] peakMagnitudes; // magnitude information for each peak
  }
}