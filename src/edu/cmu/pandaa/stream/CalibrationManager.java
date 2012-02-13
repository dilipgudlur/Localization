package edu.cmu.pandaa.stream;

import javax.rmi.CORBA.StubDelegate;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.util.concurrent.CountDownLatch;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 12/25/11
 * Time: 8:45 AM
 */

public class CalibrationManager {
  private String fname;
  private final int calMethod;
  private double pairTotal, pairWeight, pairCount, pairWtSq;
  public double calibration, avgWt, stddev, threshold;
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

  private void calcThreshold() {
    threshold = avgWt + stddev/2;
  }

  private void updateComponents() {
    calibration = pairTotal / pairWeight;
    avgWt = pairWeight / pairCount;
    stddev = Math.sqrt(pairWtSq/pairCount - avgWt * avgWt);
    calcThreshold();
  }

  public void updateCalibration(double diff, double weight) {
    pairTotal += diff * weight;
    pairWeight += weight;
    pairWtSq += weight * weight;
    pairCount++;

    if (dynamic) {
      updateComponents();
    }
  }

  public void writeCalibration() throws Exception {
    updateComponents();
    RandomAccessFile raf = new RandomAccessFile(fname, "rw");
    raf.seek(raf.length());
    String cal = id1 + "," + id2 + " " + calibration + " " + avgWt + " " + stddev + "\n";
    raf.write(cal.getBytes());
    raf.close();
    fname = null; // prevent double-usage
  }

  public void readCalibration() throws Exception {
    if (dynamic) {
      throw new IllegalStateException("Can't be dynamic and read calibration");
    }
    BufferedReader br = new BufferedReader(new FileReader(fname));
    double diff2 = 0, weight2 = 0, stdev2 = 0;
    int ccount = 0;
    String line;
    while ((line = br.readLine()) != null) {
      String[] parts = line.split(" ");
      String[] pieces = parts[0].split(",");
      double ccal = Double.parseDouble(parts[1]);
      double cavgWt = Double.parseDouble(parts[2]);
      double cstddev = Double.parseDouble(parts[3]);
      if (pieces[0].equals(id1) || pieces[1].equals(id2)) {
        ccount++;
        weight2 += cavgWt;
        diff2 += ccal * cavgWt;
        stdev2 += cstddev * cavgWt;
      }
      if (pieces[1].equals(id1) || pieces[0].equals(id2)) {
        ccount++;
        weight2 += cavgWt;
        diff2 -= ccal * cavgWt;
        stdev2 += cstddev * cavgWt;
      }
      if (pieces[0].equals(id1) && pieces[1].equals(id2)) {
        calibration = ccal;
        avgWt = cavgWt;
        stddev = cstddev;
      }
    }
    br.close();
    if (calMethod >= 2) {
      calibration = diff2 / weight2;
      stddev = stdev2 / weight2;
      avgWt = weight2 / ccount;
    }
    calcThreshold();
    fname = null; // prevent double-read of calibration file
  }
}
