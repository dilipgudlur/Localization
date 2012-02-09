package edu.cmu.pandaa.stream;

import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.ImpulseHeader.ImpulseFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/14/11
 * Time: 4:25 PM
 */

public class ImpulseFileStream extends FileStream {
  private ImpulseHeader header;

  public ImpulseFileStream() throws Exception {
    super();
  }

  public ImpulseFileStream(String filename) throws Exception {
    super(filename);
  }

  public ImpulseFileStream(String filename, boolean overwrite) throws Exception {
    super(filename, overwrite);
  }

  @Override
  public void setHeader(StreamHeader h) throws Exception {
    super.setHeader(h);
    ImpulseHeader header = (ImpulseHeader) h;
    writeValue("rows", header.rollingWindow);
  }

  @Override
  public void sendFrame(StreamFrame f) throws Exception {
    if (f == null) {
      return;
    }
    ImpulseFrame frame = (ImpulseFrame) f;
    String msg = "" + frame.seqNum;
    for (int i = 0;i < frame.peakOffsets.length; i++) {
      msg += " " + frame.peakMagnitudes[i] + " " + frame.peakOffsets[i];
    }
    writeString(msg);
  }

  @Override
  public ImpulseHeader getHeader() throws Exception {
    StreamHeader prototype = super.getHeader();
    header = new ImpulseHeader(prototype, consumeInt());
    return header;
  }

  @Override
  public ImpulseFrame recvFrame() throws Exception {
    String line = readLine();
    if (line == null || line.trim().equals(""))
      return null;
    try {
      String[] parts = line.split(" ");
      int size = (parts.length - 1)/2;
      int[] peaks = new int[size];
      int seqNum = 0;
      seqNum = Integer.parseInt(parts[0]);
      short[] mags = new short[size];
      int j = 1;
      for (int i = 0;i < size;i++) {
        mags[i] = Short.parseShort(parts[j++]);
        peaks[i] = Integer.parseInt(parts[j++]);
      }
      return header.makeFrame(seqNum, peaks, mags);
    } catch (NumberFormatException e) {
      System.out.println("Error parsing: " + line + " from " + fileName);
      throw e;
    }
  }

  public static void main(String[] args) throws Exception {
    String filename = "test.txt";

    int[] data1 = { 1, 2, 3 };
    short[] data2 = { 4, 5, 6 };
    ImpulseFileStream foo = new ImpulseFileStream(filename, true);
    ImpulseHeader header = new ImpulseHeader("w00t", System.currentTimeMillis(), 100);
    foo.setHeader(header);
    ImpulseFrame frame1 = header.makeFrame(data1, data2);
    foo.sendFrame(frame1);
    foo.sendFrame(header.makeFrame(data1, data2));
    foo.sendFrame(header.makeFrame(data1, data2));
    foo.close();

    Thread.sleep(100);  // make sure start times are different

    foo = new ImpulseFileStream(filename);
    ImpulseHeader header2 = foo.getHeader();
    ImpulseFrame frame2 = foo.recvFrame();
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

