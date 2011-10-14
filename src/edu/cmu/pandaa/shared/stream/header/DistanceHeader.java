package edu.cmu.pandaa.shared.stream.header;

public class DistanceHeader extends StreamHeader {
  
  public class DistanceFrame extends StreamFrame {
    public short[] peakDeltas;    // time delta for each peak occurring at the two devices 
  }
}