package edu.cmu.pandaa.stream;

import edu.cmu.pandaa.header.DistanceHeader;
import edu.cmu.pandaa.header.DistanceHeader.DistanceFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/14/11
 * Time: 4:25 PM
 */

public class DistanceFileStream extends FileStream {
  private DistanceHeader header;

  public DistanceFileStream(String filename) throws Exception {
    super(filename);
  }

  public DistanceFileStream(String filename, boolean overwrite) throws Exception {
    super(filename, overwrite);
  }

  @Override
  public void setHeader(StreamHeader h) throws Exception {
    DistanceHeader header = (DistanceHeader) h;
    writeString(header.id + " " + header.startTime + " " + header.frameTime + " " + header.rollingWindow);
  }

  @Override
  public void sendFrame(StreamFrame f) throws Exception {
    if (f == null) {
      return;
    }
    DistanceFrame frame = (DistanceFrame) f;
    String msg = "" + frame.seqNum;
    for (int i = 0;i < frame.peakDeltas.length; i++) {
      msg += " " + frame.peakDeltas[i];
    }
    for (int i = 0;i < frame.peakMagnitudes.length; i++) {
      msg += " " + frame.peakMagnitudes[i];
    }
    writeString(msg);
  }

  @Override
  public DistanceHeader getHeader() throws Exception {
    String line = readLine();
    String[] parts = line.split(" ");
    String[] ids = null;
    header = new DistanceHeader(parts[0],Long.parseLong(parts[1]),Integer.parseInt(parts[2]),ids);
    header.rollingWindow = Integer.parseInt(parts[3]);
    return header;
  }

  @Override
  public DistanceHeader.DistanceFrame recvFrame() throws Exception {
    String line = readLine();
    if (line == null || line.trim().equals(""))
      return null;
    try {
      String[] parts = line.split(" ");
      int size = (parts.length - 1)/2;
      short[] peaks = new short[size];
      int seqNum = Integer.parseInt(parts[0]);
      double[] mags = new double[size];
      for (int i = 0;i < size;i++) {
        peaks[i] = Short.parseShort(parts[i + 1]);
      }
      for (int i = 0;i < size;i++) {
        mags[i] = Double.parseDouble(parts[i + size + 1]);
      }
      return header.makeFrame(peaks, mags);
    } catch (NumberFormatException e) {
      System.out.println("Error parsing: " + line + " from " + fileName);
      throw e;
    }
  }

  public static void main(String[] args) throws Exception {
    String filename = "test.txt";

    short[] data1 = { 1, 2, 3 };
    double[] data2 = { 4, 5, 6 };
    DistanceFileStream foo = new DistanceFileStream(filename, true);
    String[] ids = { "a", "b" };
    DistanceHeader header = new DistanceHeader("w00t", System.currentTimeMillis(), 100, ids);
    foo.setHeader(header);
    DistanceFrame frame1 = header.makeFrame(data1, data2);
    foo.sendFrame(frame1);
    foo.sendFrame(header.makeFrame(data1, data2));
    foo.sendFrame(header.makeFrame(data1, data2));
    foo.close();

    Thread.sleep(100);  // make sure start times are different

    foo = new DistanceFileStream(filename);
    DistanceHeader header2 = foo.getHeader();
    DistanceFrame frame2 = foo.recvFrame();
    frame2 = foo.recvFrame();
    frame2 = foo.recvFrame();
    foo.close();

    if (frame1.getHeader().startTime != frame2.getHeader().startTime) {
      System.err.println("Start time mismatch!");
    }
    if (frame1.seqNum != frame2.seqNum-2) {
      System.err.println("Sequence number mismatch!");
    }
  }
}

