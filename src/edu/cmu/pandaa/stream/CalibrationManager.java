package edu.cmu.pandaa.stream;

import java.io.BufferedReader;
import java.io.FileReader;
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

  public CalibrationManager(String id1, String id2, int calMethod) throws Exception {
    this.calMethod = calMethod;
    this.id1 = id1;
    this.id2 = id2;
    if (id1.compareTo(id2) >= 0) {
      throw new IllegalArgumentException("Calibration IDs are in the wrong order or equal: " + id1 + " >= " + id2);
    }
  }

  public boolean updateCalibration(double diff, double weight) {
    diff += calibration;

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

    return valid || calMethod == 0;
  }

  public double getCalibration() {
    return calibration;
  }

  public void writeCalibration(String fname) throws Exception {
    RandomAccessFile raf = new RandomAccessFile(fname, "rw");
    raf.seek(raf.length());
    String cal = id1 + "," + id2 + " " + calibration + " " + sumDiff + " " + sumWeight + " " + sumWeightSq + " " + sumCount + "\n";
    raf.write(cal.getBytes());
    raf.close();
  }
}
