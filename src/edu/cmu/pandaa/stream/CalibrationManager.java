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
  private final int calMethod;
  private double pairTotal, pairWeight, pairCount, pairWtSq, pairMax;
  public double calibration, max, avgWt, stddev, threshold;
  private final String id1, id2;
  private final boolean dynamic;

  public CalibrationManager(String fname, boolean dynamic,
                            String id1, String id2, int calMethod) throws Exception {
    this.calMethod = calMethod;
    this.fname = fname;
    this.id1 = id1;
    this.id2 = id2;
    this.dynamic = dynamic;
    if (id1.compareTo(id2) >= 0) {
      throw new IllegalArgumentException("Calibration IDs are in the wrong order or equal: " + id1 + " >= " + id2);
    }
  }

  private void updateComponents() {
    max = pairMax;
    calibration = pairTotal / pairWeight;
    avgWt = pairWeight / pairCount;
    stddev = Math.sqrt(pairWtSq/pairCount - avgWt * avgWt);
    threshold = avgWt + stddev*2;
  }

  public void updateCalibration(double diff, double weight) {
    pairTotal += diff * weight;
    pairWeight += weight;
    pairWtSq += weight * weight;
    pairCount++;
    pairMax = Math.max(pairMax, weight);

    if (dynamic) {
      updateComponents();
    }
  }

  public void writeCalibration() throws Exception {
    updateComponents();
    RandomAccessFile raf = new RandomAccessFile(fname, "rw");
    raf.seek(raf.length());
    String cal = id1 + "," + id2 + " " + calibration + " " + max + " " + avgWt + " " + stddev + "\n";
    raf.write(cal.getBytes());
    raf.close();
    fname = null; // prevent double-usage
  }

  public void readCalibration() throws Exception {
    if (dynamic) {
      throw new IllegalStateException("Can't be dynamic and read calibration");
    }
    BufferedReader br = new BufferedReader(new FileReader(fname));
    double diff1 = 0, weight1 = 0;
    double diff2 = 0, weight2 = 0;
    String line;
    while ((line = br.readLine()) != null) {
      String[] parts = line.split(" ");
      String[] pieces = parts[0].split(",");
      double cval = Double.parseDouble(parts[1]);
      double cweight = Double.parseDouble(parts[3]);
      if (pieces[0].equals(id1) || pieces[1].equals(id2)) {
        weight2 += cweight;
        diff2 += cval * cweight;
      }
      if (pieces[1].equals(id1) || pieces[0].equals(id2)) {
        weight2 += cweight;
        diff2 -= cval * cweight;
      }
      if (pieces[0].equals(id1) && pieces[1].equals(id2)) {
        weight1 += cweight;
        diff1 += cval * cweight;
        max = Double.parseDouble(parts[2]);
        avgWt = Double.parseDouble(parts[3]);
        stddev = Double.parseDouble(parts[4]);
      }
    }
    br.close();
    double calibration1 = -(weight1 > 0 ? diff1 / weight1 : 0.0);
    double calibration2 = -(weight2 > 0 ? diff2 / weight2 : 0.0) * 3/2;
    calibration = calMethod <= 1 ? calibration1 : calibration2;
    fname = null; // prevent double-read of calibration file
  }
}
