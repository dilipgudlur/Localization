package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.DistanceHeader;
import edu.cmu.pandaa.header.DistanceHeader.DistanceFrame;
import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.ImpulseHeader.ImpulseFrame;
import edu.cmu.pandaa.header.MultiHeader;
import edu.cmu.pandaa.header.MultiHeader.MultiFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;

public class TDOACorrelationModule implements StreamModule {
  DistanceHeader header;
  
  public StreamHeader init(StreamHeader inHeader) {
    MultiHeader multiHeader = (MultiHeader) inHeader;
    if (!(multiHeader.getOne() instanceof ImpulseHeader)) {
       throw new IllegalArgumentException("Input multiheader should contain ImpulseHeaders");
    }
    
    StreamHeader[] impulseHeaders = multiHeader.getHeaders();
    if (impulseHeaders.length != 2) {
      throw new IllegalArgumentException("Input multiheader should contain two elements");
    }
    
    if (impulseHeaders[0].frameTime != impulseHeaders[1].frameTime) {
      throw new IllegalArgumentException("Frame duration must be equal for both input frames");
    }
    
    String[] deviceIds = new String[] {impulseHeaders[0].id, impulseHeaders[1].id};
    
    header = new DistanceHeader(inHeader.id, impulseHeaders[0].startTime, impulseHeaders[0].frameTime, deviceIds);
    return header;
  }
  
  public StreamFrame process(StreamFrame inFrame) {
    StreamFrame[] frames = ((MultiFrame) inFrame).getFrames();
    if (!(frames[0] instanceof ImpulseFrame)) {
      throw new IllegalArgumentException("Input multiframe should contain ImpulseFrames");
    }
    if (frames.length != 2) {
      throw new IllegalArgumentException("Input multiframe should contain two elements");
    }
    
    // compute only for first peak in each frame for now
    short[] peakDeltas = new short[] {(short) (((ImpulseFrame) frames[0]).peakOffsets[0] - ((ImpulseFrame) frames[1]).peakOffsets[0])}; 
    double[] peakMagnitudes = new double[] {(((ImpulseFrame) frames[0]).peakMagnitudes[0] + ((ImpulseFrame) frames[1]).peakMagnitudes[0]) / 2};
    
    return header.makeFrame(peakDeltas, peakMagnitudes);
  }
  
  public void close() {
    
  }
  
  public static void main(String[] args) {
    ImpulseHeader i1Header = new ImpulseHeader("i1", System.currentTimeMillis(), 100);
    ImpulseHeader i2Header = new ImpulseHeader("i2", System.currentTimeMillis() + 10, 100);
    
    MultiHeader inHeader = new MultiHeader("additup", i1Header);
    inHeader.addHeader(i2Header);
    
    TDOACorrelationModule tdoa = new TDOACorrelationModule();
    tdoa.init(inHeader);
    
    ImpulseFrame i1Frame = i1Header.makeFrame(new int[] {10}, new short[] {4000});
    ImpulseFrame i2Frame = i2Header.makeFrame(new int[] {15}, new short[] {2000});
    
    MultiFrame inFrame = inHeader.makeFrame();
    inFrame.setFrame(i1Frame);
    inFrame.setFrame(i2Frame);
    
    DistanceFrame outFrame = (DistanceFrame) tdoa.process(inFrame);
    
    System.out.println("DistanceFrame output: frame #" + outFrame.seqNum + ", deltas " + outFrame.peakDeltas[0] + ", magnitudes " + outFrame.peakMagnitudes[0]);
  }
}