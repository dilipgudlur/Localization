package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.DistanceHeader;
import edu.cmu.pandaa.header.DistanceHeader.DistanceFrame;
import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.ImpulseHeader.ImpulseFrame;
import edu.cmu.pandaa.header.MultiHeader;
import edu.cmu.pandaa.header.MultiHeader.MultiFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.DistanceFileStream;
import edu.cmu.pandaa.stream.FileStream;
import edu.cmu.pandaa.stream.ImpulseFileStream;
import edu.cmu.pandaa.stream.MultiFrameStream;

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

    /* Run through all the peaks that presumably occur at both devices.
     * Right now, we're assuming that impulses reach devices in the same 
     * order, within the same frame, and within less than 58ms (speed of
     * sound over 20m, a generous room size). Tweaking needed.
     */    
    int sharedPeakCount = (((ImpulseFrame) frames[0]).peakOffsets.length < ((ImpulseFrame) frames[1]).peakOffsets.length) 
        ? ((ImpulseFrame) frames[0]).peakOffsets.length 
        : ((ImpulseFrame) frames[1]).peakOffsets.length;
    
    double[] peakDeltas = new double[sharedPeakCount];
    double[] peakMagnitudes = new double[sharedPeakCount];
    for (int i = 0; i < sharedPeakCount; i++) {
      // subtract peak offsets
      peakDeltas[i] = ((ImpulseFrame) frames[0]).peakOffsets[i] - ((ImpulseFrame) frames[1]).peakOffsets[i];
      // average peak magnitudes
      peakMagnitudes[i] = (((ImpulseFrame) frames[0]).peakMagnitudes[i] + ((ImpulseFrame) frames[1]).peakMagnitudes[i]) / 2;
    }

    return header.makeFrame(peakDeltas, peakMagnitudes);
  }

  public void close() {

  }

  public static void main(String[] args) throws Exception {
    int arg = 0;
    String outf = args[arg++];
    String in1 = args[arg++];
    String in2 = args[arg++];
    if (arg != args.length)
      throw new IllegalArgumentException("Excess arguments");

    System.out.println("Correlate " + in1 + " & " + in2 + " to " + outf);
    FileStream ifs1 = new ImpulseFileStream(in1);
    FileStream ifs2 = new ImpulseFileStream(in2);

    MultiFrameStream mfs = new MultiFrameStream("tdoa");
    mfs.setHeader(ifs1.getHeader());
    mfs.setHeader(ifs2.getHeader());

    FileStream ofs = new DistanceFileStream(outf, true);

    TDOACorrelationModule tdoa = new TDOACorrelationModule();
    ofs.setHeader(tdoa.init(mfs.getHeader()));

    try {
      while (true) {
        mfs.sendFrame(ifs1.recvFrame());
        mfs.sendFrame(ifs2.recvFrame());
        ofs.sendFrame(tdoa.process(mfs.recvFrame()));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    ofs.close();
  }

  public static void main2(String[] args) {
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