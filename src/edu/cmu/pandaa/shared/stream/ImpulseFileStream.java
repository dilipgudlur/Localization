package edu.cmu.pandaa.shared.stream;

import edu.cmu.pandaa.shared.stream.header.FeatureHeader;
import edu.cmu.pandaa.shared.stream.header.FeatureHeader.FeatureFrame;
import edu.cmu.pandaa.shared.stream.header.StreamHeader;
import edu.cmu.pandaa.shared.stream.header.StreamHeader.StreamFrame;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/14/11
 * Time: 4:25 PM
 */

public class ImpulseFileStream extends FileStream {
  private FeatureHeader header;

  public ImpulseFileStream(String filename) throws Exception {
    super(filename);
  }

  public ImpulseFileStream(String filename, boolean overwrite) throws Exception {
    super(filename, overwrite);
  }

  public void sendHeader(StreamHeader h) throws Exception {
    FeatureHeader header = (FeatureHeader) h;
    writeString(header.id + " " + header.startTime + " " + header.frameTime);
  }

  public void sendFrame(StreamFrame f) throws Exception {
    FeatureFrame frame = (FeatureFrame) f;
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

  public FeatureHeader recvHeader() throws Exception {
    String line = readLine();
    String[] parts = line.split(" ");
    header = new FeatureHeader(parts[0],Long.parseLong(parts[1]),Integer.parseInt(parts[2]));
    return header;
  }

  public FeatureFrame recvFrame() throws Exception {
    nextFile();  // I wouldn't actually recommend this for ImpulseFileStream, but doing it as a demonstraiton
    String line = readLine();
    String[] parts = line.split(" ");
    int size = (parts.length-1)/2;
    int seqNum = Integer.parseInt(parts[0]);
    int[] peaks = new int[size];
    short[] mags = new short[size];
    for (int i = 0;i < size;i++) {
       peaks[i] = Integer.parseInt(parts[i + 1]);
    }
    for (int i = 0;i < size;i++) {
       mags[i] = Short.parseShort(parts[i + size + 1]);
    }
    return header.makeFrame(seqNum, peaks, mags);
  }

  public static void main(String[] args) throws Exception {
    String filename = "test.txt";

    int[] data1 = { 1, 2, 3 };
    short[] data2 = { 4, 5, 6 };
    ImpulseFileStream foo = new ImpulseFileStream(filename, true);
    FeatureHeader header = new FeatureHeader("w00t", System.currentTimeMillis(), 100);
    foo.sendHeader(header);
    FeatureFrame frame1 = header.makeFrame(data1, data2);
    foo.sendFrame(frame1);
    foo.sendFrame(header.makeFrame(data1, data2));
    foo.sendFrame(header.makeFrame(data1, data2));
    foo.close();

    Thread.sleep(100);  // make sure start times are different

    foo = new ImpulseFileStream(filename);
    FeatureHeader header2 = foo.recvHeader();
    FeatureFrame frame2 = foo.recvFrame();
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

