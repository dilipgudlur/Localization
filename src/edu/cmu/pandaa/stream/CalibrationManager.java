package edu.cmu.pandaa.stream;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.RandomAccessFile;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 12/25/11
 * Time: 8:45 AM
 */

public class CalibrationManager {
  private String fname;
  private double calFactor;
  private double pairTotal, pairWeight, pairCount, pairWtSq;
  public double calibration, max, average, stddev, threshold;
  private final String id1, id2;

  public CalibrationManager(String fname, String id1, String id2, double calFactor) throws Exception {
    this.calFactor = calFactor;
    this.fname = fname;
    this.id1 = id1;
    this.id2 = id2;
    if (id1.compareTo(id2) >= 0) {
      throw new IllegalArgumentException("Calibration IDs are in the wrong order or equal: " + id1 + " >= " + id2);
    }
  }

  public void updateCalibration(double diff, double weight) {
    pairTotal += diff * weight;
    pairWeight += weight;
    pairWtSq += weight * weight;
    pairCount++;

    max = Math.max(max, weight);
    calibration = pairTotal / pairWeight;
    average = pairWeight / pairCount;
    stddev = Math.sqrt(pairWtSq/pairCount - average * average);
    threshold = average + stddev*2;
  }

  public void writeCalibration() throws Exception {
    RandomAccessFile raf = new RandomAccessFile(fname, "rw");
    raf.seek(raf.length());
    String cal = id1 + "," + id2 + " " + calibration + " " + max + " " + average + " " + stddev + "\n";
    raf.write(cal.getBytes());
    raf.close();
    fname = null; // prevent double-usage
  }

  public void readCalibration() throws Exception {
    BufferedReader br = new BufferedReader(new FileReader(fname));
    double diff = 0;
    String lstart = id1 + ",";
    String lsecond = "," + id2;
    String lboth = id1 + "," + id2;
    String line;
    int count = 0;
    while ((line = br.readLine()) != null) {
      String[] parts = line.split(" ");
      double cval = Double.parseDouble(parts[1]);
      if (parts[0].startsWith(lstart)) {
        count++;
        diff += cval;
      }
      if (parts[0].endsWith(lsecond)) {
        count++;
        diff -= cval;
      }
      if (parts[0].equals(lboth)) {
        max = Double.parseDouble(parts[2]);
        average = Double.parseDouble(parts[3]);
        stddev = Double.parseDouble(parts[4]);
      }
    }
    br.close();
    calibration = count > 0 ? diff / count : 0.0;
    fname = null; // prevent double-read of calibration file
  }
}
