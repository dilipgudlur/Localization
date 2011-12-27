package edu.cmu.pandaa.stream;

import java.io.BufferedReader;
import java.io.File;
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

  public CalibrationFile(String fname) throws Exception {
    this.fname = fname;
  }

  public void writeCalibration(String id1, String id2, double calibration) throws Exception {
    RandomAccessFile raf = new RandomAccessFile("calibration.txt", "rw");
    raf.seek(raf.length());
    String cal = id1 + "," + id2 + " " + calibration + "\n";
    raf.write(cal.getBytes());
  }

  public double readCalibration(String id) throws Exception {
    if (!(new File(fname)).exists())
      return 0.0;

    BufferedReader br = new BufferedReader(new FileReader(fname));
    double diff = 0;
    String lstart = id + ",";
    String lsecond = "," + id + " ";
    String line;
    int count = 0;
    while ((line = br.readLine()) != null) {
      int space = line.indexOf(' ');
      double value = Double.parseDouble(line.substring(space+1));
      if (line.startsWith(lstart)) {
        count++;
        diff += value;
      } else if (line.indexOf(lsecond) >= 0) {
        count++;
        diff -= value;
      }
    }
    br.close();
    return count > 0 ? diff / count : 0.0;
  }

  public void close() throws Exception {
  }
}
