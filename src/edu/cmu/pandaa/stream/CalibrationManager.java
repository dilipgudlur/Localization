package edu.cmu.pandaa.stream;

import edu.cmu.pandaa.header.DistanceHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;

import java.io.RandomAccessFile;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 12/25/11        Calibration
 * Time: 8:45 AM
 */

public class CalibrationManager {
  private final int calMethod;
  private double calibration;
  private final String id1, id2;
  private double sumWeight, sumWeightSq, sumDiff;
  private int sumCount;
  private DistanceFileStream cOut;
  private DistanceHeader cHead;
  private final boolean swap;

  public CalibrationManager(String id1, String id2, int calMethod) throws Exception {
    this.calMethod = calMethod;
    this.id1 = id1;
    this.id2 = id2;
    this.swap = (id1.compareTo(id2) >= 0);
  }

  public void updateCalibration(double rawDiff, double weight, StreamFrame refFrame) throws Exception {
    double diff = rawDiff;
    if (!Double.isNaN(calibration)) {
      diff += calibration;
    }

    sumWeight += weight;
    sumWeightSq += weight * weight;
    sumCount++;
    sumDiff += diff * weight;

    calibration = sumDiff / sumWeight;

    if (cOut != null) {
      double[] calA = { calibration };
      double[] wtA = { diff };
      double[] rwA = { weight };
      cOut.sendFrame(cHead.makeFrame(refFrame.seqNum, calA, wtA, rwA));
    }
  }

  public double getCalibration() {
    return calibration;
  }

  public void writeCalibration(String fname, StreamHeader prototype) throws Exception {
    String[] ids = { id1, id2 };
    cHead = new DistanceHeader("calibration", prototype.startTime, prototype.frameTime, ids);
    cOut = new DistanceFileStream(fname, true);
    cOut.setHeader(cHead);
  }
}
