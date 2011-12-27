package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.DistanceHeader;
import edu.cmu.pandaa.header.DistanceHeader.DistanceFrame;
import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.ImpulseHeader.ImpulseFrame;
import edu.cmu.pandaa.header.MultiHeader;
import edu.cmu.pandaa.header.MultiHeader.MultiFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.*;

import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.*;

public class TDOACrossModule implements StreamModule {
  DistanceHeader header;
  final int maxAbsDistance = 30 * 1000;   // max plausible distance between peaks of the same event (micro-seconds)
  double pairTotal, pairWeight;
  CalibrationFile cf;
  boolean calibrated;
  double calibration;

  public StreamHeader init(StreamHeader inHeader) throws Exception {
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
    if (deviceIds[0].compareTo(deviceIds[1]) > 0) {
      throw new RuntimeException("Frames out of order init");
    }

    header = new DistanceHeader(inHeader.id, impulseHeaders[0].startTime, impulseHeaders[0].frameTime, deviceIds);

    if (cf != null && calibrated) {
      double d1 = cf.readCalibration(deviceIds[0]);
      double d2 = cf.readCalibration(deviceIds[1]);
      calibration = d1 - d2;
      System.out.println("Calibration adjustment is " + calibration);
    }

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

  public void setCalibrationFile(String fname, boolean calibrated) throws Exception {
    cf = new CalibrationFile(fname);
    this.calibrated = calibrated;
  }

  private double calcWeight(int ao, int am, int bo, int bm) {
    int dist = Math.abs(ao - bo); // difference in us
    if (dist > maxAbsDistance)
      return 0;
    return (double) am*bm*(maxAbsDistance - dist)/maxAbsDistance;
  }

  public StreamFrame process(StreamFrame inFrame) {
    MultiFrame mf = (MultiFrame) inFrame;
    ImpulseFrame aFrame = (ImpulseFrame) mf.getFrame(0);
    ImpulseFrame bFrame = (ImpulseFrame) mf.getFrame(1);
    int aSize = aFrame.peakOffsets.length;
    int bSize = bFrame.peakOffsets.length;

    if (aFrame.getHeader().id.compareTo(bFrame.getHeader().id) > 0) {
      throw new RuntimeException("Frames out of order process");
    }

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
      int diff = p.ao - p.bo;
      peakDeltas[i] = diff - calibration;
      peakMagnitudes[i] = p.weight;
      pairTotal += diff * p.weight;
      pairWeight += p.weight;
    }

    return header.makeFrame(peakDeltas, peakMagnitudes);
  }

  public void close() {
    if (cf != null && !calibrated)
      try {
        String id0 = header.getId(0);
        String id1 = header.getId(1);
        if (id0.compareTo(id1) > 0) {
          throw new RuntimeException("Frames out of order close");
        }
        cf.writeCalibration(id0, id1, pairTotal / pairWeight);
        cf.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
  }

  public static void main(String[] args) throws Exception {
    int arg = 0;
    boolean calibrated = false;

    if (args[arg].equals("-c")) {
      arg++;
      calibrated = true;
    }

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

    TDOACrossModule tdoa = new TDOACrossModule();
    tdoa.setCalibrationFile("calibration.txt", calibrated);

    FileStream ofs = new DistanceFileStream(outf, true);
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
    tdoa.close();
    ofs.close();
  }
}
