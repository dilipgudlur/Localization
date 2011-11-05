package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.DistanceHeader;
import edu.cmu.pandaa.header.DistanceHeader.DistanceFrame;
import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.ImpulseHeader.ImpulseFrame;
import edu.cmu.pandaa.header.MultiHeader;
import edu.cmu.pandaa.header.MultiHeader.MultiFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.DistanceFileStream;
import edu.cmu.pandaa.stream.FileStream;
import edu.cmu.pandaa.stream.GeometryFileStream;
import edu.cmu.pandaa.stream.ImpulseFileStream;
import edu.cmu.pandaa.stream.MultiFrameStream;


public class ProcessPairwiseDistancesModule implements StreamModule {

	GeometryHeader gheader;

	  public StreamHeader init(StreamHeader inHeader) {
	    MultiHeader multiHeader = (MultiHeader) inHeader;
	    if (!(multiHeader.getOne() instanceof DistanceHeader)) {
	      throw new IllegalArgumentException("Input multiheader should contain Distance Headers");
	    }

	    StreamHeader[] distanceHeaders = multiHeader.getHeaders();
	    if (distanceHeaders.length != 3) {
	      throw new IllegalArgumentException("Input multiheader should contain 3 elements");
	    }

	    if (distanceHeaders[0].frameTime != distanceHeaders[1].frameTime) {
	      throw new IllegalArgumentException("Frame duration must be equal for both input frames");
	    }

	    /*generate device id
	     * iterate through all distance headers available
	     * find unique deviceIds and form the deviceId matrix*/
	    
	    /*String[] deviceIds = new String[] {distanceHeaders[0].id, impulseHeaders[1].id};

	    header = new DistanceHeader(inHeader.id, impulseHeaders[0].startTime, impulseHeaders[0].frameTime, deviceIds);*/
	    return null;
	  }

	  public StreamFrame process(StreamFrame inFrame) {
	   /* StreamFrame[] frames = ((MultiFrame) inFrame).getFrames();
	    if (!(frames[0] instanceof ImpulseFrame)) {
	      throw new IllegalArgumentException("Input multiframe should contain ImpulseFrames");
	    }
	    if (frames.length != 2) {
	      throw new IllegalArgumentException("Input multiframe should contain two elements");
	    }

	    // compute only for first peak in each frame for now
	    short[] peakDeltas = new short[] {(short) (((ImpulseFrame) frames[0]).peakOffsets[0] - ((ImpulseFrame) frames[1]).peakOffsets[0])};
	    double[] peakMagnitudes = new double[] {(((ImpulseFrame) frames[0]).peakMagnitudes[0] + ((ImpulseFrame) frames[1]).peakMagnitudes[0]) / 2};

	    return header.makeFrame(peakDeltas, peakMagnitudes);*/
		  return null;
	  }

	  public void close() {

	  }

	  public static void main(String[] args) throws Exception {
	    int arg = 0;
	    String outf = args[arg++];
	    String in1 = args[arg++];
	    String in2 = args[arg++];
	    String in3 = args[arg++];
	    if (arg != args.length)
	      throw new IllegalArgumentException("Excess arguments");
	    
	    /*testing for 3 pairwise distance files right now*/
	    System.out.println("Combine pairwise distances " + in1 + "," + in2 + " & " + " to "+ outf);
	    FileStream ifs1 = new DistanceFileStream(in1);
	    FileStream ifs2 = new DistanceFileStream(in2);
	    FileStream ifs3 = new DistanceFileStream(in3);

	    MultiFrameStream mfs = new MultiFrameStream("tdoa123");
	    mfs.setHeader(ifs1.getHeader());
	    mfs.setHeader(ifs2.getHeader());
	    mfs.setHeader(ifs3.getHeader());

	    FileStream ofs = new GeometryFileStream(outf, true);

	    ProcessPairwiseDistancesModule ppd = new ProcessPairwiseDistancesModule();
	    ofs.setHeader(ppd.init(mfs.getHeader()));

	    try {
	      while (true) {
	        mfs.sendFrame(ifs1.recvFrame());
	        mfs.sendFrame(ifs2.recvFrame());
	        mfs.sendFrame(ifs3.recvFrame());
	        ofs.sendFrame(ppd.process(mfs.recvFrame()));
	      }
	    } catch (Exception e) {
	      e.printStackTrace();
	    }
	    ofs.close();
	  }
/*
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
	  }*/
}
