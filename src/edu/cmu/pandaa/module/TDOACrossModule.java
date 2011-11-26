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

import java.util.*;

public class TDOACrossModule implements StreamModule {
  DistanceHeader header;
  final int maxAbsDistance = 30 * 1000;   // max plausible distance between peaks of the same event (micro-seconds)

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

  private static class Peak implements Comparable<Peak> {
    final int ai, bi;
    final int ao, bo;
    final double weight;
    public Peak(int ai, int ao, int bi, int bo, double weight) {
      this.ai = ai;
      this.bi = bi;
      this.ao = ao;
      this.bo = bo;
      this.weight = weight;
    }

    public int compareTo(Peak other) {
      return Double.compare(other.weight, weight);
    }
  }

  private double calcWeight(int ao, int am, int bo, int bm) {
    int dist = Math.abs(ao - bo); // difference in us
    if (dist > maxAbsDistance)
      return 0;
    return (double) am*bm*(maxAbsDistance - dist)/maxAbsDistance;
  }

  public StreamFrame process(StreamFrame inFrame) {
    StreamFrame[] frames = ((MultiFrame) inFrame).getFrames();
    if (!(frames[0] instanceof ImpulseFrame)) {
      throw new IllegalArgumentException("Input multiframe should contain ImpulseFrames");
    }
    if (frames.length != 2) {
      throw new IllegalArgumentException("Input multiframe should contain two elements");
    }

    ImpulseFrame aFrame = (ImpulseFrame) frames[0];
    ImpulseFrame bFrame = (ImpulseFrame) frames[1];
    int aSize = aFrame.peakOffsets.length;
    int bSize = bFrame.peakOffsets.length;

    List<Peak> peaks = new ArrayList<Peak>(aSize * bSize);
    for (int ai = 0; ai < aSize; ai++) {
      for (int bi = 0; bi < bSize; bi++) {
        double weight = calcWeight(aFrame.peakOffsets[ai], aFrame.peakMagnitudes[ai],
                bFrame.peakOffsets[bi], bFrame.peakMagnitudes[bi]);
        peaks.add(new Peak(
                ai, aFrame.peakOffsets[ai],
                bi, bFrame.peakOffsets[bi],
                weight));
      }
    }

    // Totally not the most efficient data-structure, but I don't care\
    boolean[] aMark = new boolean[aSize];
    boolean[] bMark = new boolean[bSize];
    List<Peak> output = new ArrayList<Peak>(Math.min(aSize, bSize));

    Peak[] peakArray = (Peak[]) peaks.toArray(new Peak[0]);
    Arrays.sort(peakArray);
    for (int i = 0;i < peakArray.length;i ++) {
      Peak p = peakArray[i];
      if (aMark[p.ai] || bMark[p.bi])
        continue;
      aMark[p.ai] = true;
      bMark[p.bi] = true;
      output.add(p);
    }

    double[] peakDeltas = new double[output.size()];
    double[] peakMagnitudes = new double[output.size()];

    for (int i = 0;i < output.size(); i++) {
      Peak p = output.get(i);
      peakDeltas[i] = p.ao - p.bo;
      peakMagnitudes[i] = p.weight;
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

    System.out.println("TDOACrossModule: " + outf + " " + in1 + " " + in2);
    FileStream ifs1 = new ImpulseFileStream(in1);
    FileStream ifs2 = new ImpulseFileStream(in2);

    MultiFrameStream mfs = new MultiFrameStream("tdoa");
    mfs.setHeader(ifs1.getHeader());
    mfs.setHeader(ifs2.getHeader());

    FileStream ofs = new DistanceFileStream(outf, true);

    TDOACrossModule tdoa = new TDOACrossModule();
    ofs.setHeader(tdoa.init(mfs.getHeader()));

    try {
      while (true) {
        mfs.sendFrame(ifs1.recvFrame());
        mfs.sendFrame(ifs2.recvFrame());
        if (!mfs.isReady())
          break;
        ofs.sendFrame(tdoa.process(mfs.recvFrame()));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    ofs.close();
  }
}
