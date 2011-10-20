package edu.cmu.pandaa.stream;

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

  public ImpulseFileStream(String filename) throws Exception {
    super(filename);
  }

  public ImpulseFileStream(String filename, boolean overwrite) throws Exception {
    super(filename, overwrite);
  }

  @Override
public void setHeader(StreamHeader h) throws Exception {
    ImpulseHeader header = (ImpulseHeader) h;
    writeString(header.id + " " + header.startTime + " " + header.frameTime);
  }

  @Override
public void sendFrame(StreamFrame f) throws Exception {
    ImpulseFrame frame = (ImpulseFrame) f;
    nextFile();  // I wouldn't actually recommend this for ImpulseFileStream, but doing it as a demonstraiton
    String msg = "" + frame.seqNum;
    for (int i = 0;i < frame.peakOffsets.length; i++) {
      msg += " " + frame.peakOffsets[i];
    }
    for (int i = 0;i < frame.peakMagnitudes.length; i++) {
      msg += " " + frame.peakMagnitudes[i];
    }
    writeString(msg);
  }

  @Override
public ImpulseHeader getHeader() throws Exception {
    String line = readLine();
    String[] parts = line.split(" ");
    header = new ImpulseHeader(parts[0],Long.parseLong(parts[1]),Integer.parseInt(parts[2]));
    return header;
  }

  @Override
public ImpulseFrame recvFrame() throws Exception {
    nextFile();  // I wouldn't actually recommend this for ImpulseFileStream, but doing it as a demonstraiton
    String line = readLine();
    String[] parts = line.split(" ");
    int size = (parts.length-1)/2;
    int seqNum = Integer.parseInt(parts[0]);
    int[] peaks = new int[size];
    byte[] mags = new byte[size];
    for (int i = 0;i < size;i++) {
       peaks[i] = Integer.parseInt(parts[i + 1]);
    }
    for (int i = 0;i < size;i++) {
       mags[i] = Byte.parseByte(parts[i + size + 1]);
    }
    return header.makeFrame(seqNum, peaks, mags);
  }

  public static void main(String[] args) throws Exception {
    String filename = "test.txt";

    int[] data1 = { 1, 2, 3 };
    byte[] data2 = { 4, 5, 6 };
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

