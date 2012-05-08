package edu.cmu.pandaa.stream;

import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.ImpulseHeader.ImpulseFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;

import java.util.ArrayList;

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
    super.sendFrame(f);
    ImpulseFrame frame = (ImpulseFrame) f;
    writeArray("impulses");
    for (int i = 0;i < frame.peakOffsets.length; i++) {
      writeArrayObject();
      writeValue("offset", frame.peakOffsets[i]);
      writeValue("mag", frame.peakMagnitudes[i]);
    }
    writeEndArray();
  }

  @Override
  public ImpulseHeader getHeader() throws Exception {
    StreamHeader prototype = super.getHeader();
    header = new ImpulseHeader(prototype, consumeInt());
    return header;
  }

  @Override
  public ImpulseFrame recvFrame() throws Exception {
    StreamFrame prototype = super.recvFrame();
    ArrayList<Integer> offsets = new ArrayList<Integer>();
    ArrayList<Short> mags = new ArrayList<Short>();
    while (hasMoreData()) {
      offsets.add(consumeInt());
      mags.add((short) consumeInt());
    }
    return header.makeFrame(prototype, offsets, mags);
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
      System.out.println("Start time mismatch!");
    }
    if (frame1.seqNum != frame2.seqNum-2) {
      System.out.println("Sequence number mismatch!");
    }
  }
}

