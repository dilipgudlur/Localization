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

public class CalibrationFile {
  String fname;
  public double calibration, max, average, stddev;

  public CalibrationFile(String fname) throws Exception {
    this.fname = fname;
  }

  public void writeCalibration(String id1, String id2, double calibration,double max,
                               double avg, double var) throws Exception {
    RandomAccessFile raf = new RandomAccessFile("calibration.txt", "rw");
    raf.seek(raf.length());
    String cal = id1 + "," + id2 + " " + calibration + " " + max + " " + avg + " " + var +"\n";
    raf.write(cal.getBytes());
  }

  public void readCalibration(String id, String id2) throws Exception {
    BufferedReader br = new BufferedReader(new FileReader(fname));
    double diff = 0;
    String lstart = id + ",";
    String lsecond = "," + id2;
    String lboth = id + "," + id2;
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
  }

  public void close() throws Exception {
  }
}
