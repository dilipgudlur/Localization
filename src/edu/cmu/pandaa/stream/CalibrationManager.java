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
  private double calibration;
  private final String id1, id2;
  private double sumWeight, sumDiff;
  private int sumCount;
  private DistanceFileStream cOut;
  private DistanceHeader cHead;
  private final boolean swap;
  private final double WEIGHT_DECAY = 0.97;

  public CalibrationManager(String id1, String id2, int calMethod) throws Exception {
    this.id1 = id1;
    this.id2 = id2;
    this.swap = (id1.compareTo(id2) >= 0);
  }

  public void updateCalibration(double diffPeak, double weight, StreamFrame refFrame) throws Exception {
    double rawDiff = diffPeak;
    if (!Double.isNaN(calibration)) {
      rawDiff += calibration;
    }

    if (sumWeight == 0) {
      sumWeight = weight;
    }
    sumWeight += weight;
    sumCount++;
    sumDiff += rawDiff * weight;

    calibration = sumDiff / sumWeight;

    sumWeight *= WEIGHT_DECAY;
    sumDiff *= WEIGHT_DECAY;

    if (cOut != null) {
      double[] calA = { calibration };
      double[] wtA = { rawDiff };
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
