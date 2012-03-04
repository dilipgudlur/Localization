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
  private double sumWeight, sumWeightSq, sumDiff, sumValid;
  private int sumCount;
  private DistanceFileStream cOut;
  private DistanceHeader cHead;

  public CalibrationManager(String id1, String id2, int calMethod) throws Exception {
    this.calMethod = calMethod;
    this.id1 = id1;
    this.id2 = id2;
    if (id1.compareTo(id2) >= 0) {
      throw new IllegalArgumentException("Calibration IDs are in the wrong order or equal: " + id1 + " >= " + id2);
    }
  }

  public boolean updateCalibration(double rawDiff, double weight, StreamFrame refFrame) throws Exception {
    double diff = rawDiff + calibration;

    sumWeight += weight;
    sumWeightSq += weight * weight;
    sumCount++;

    double avgWt = sumWeight / sumCount;
    double stdev = Math.sqrt(sumWeightSq/sumCount - avgWt * avgWt);
    double threshold = avgWt + stdev;

    boolean valid = weight >= threshold;
    if (valid) {
      sumDiff += diff * weight;
      sumValid += weight;
    }

    if (sumValid > 0) {
      calibration = sumDiff / sumValid;
    }

    if (valid && cOut != null) {
      double[] calA = { calibration };
      double[] wtA = { diff };
      double[] rwA = { weight };
      cOut.sendFrame(cHead.makeFrame(refFrame.seqNum, calA, wtA, rwA));
    }

    return valid || calMethod == 0;
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
