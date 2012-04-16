package edu.cmu.pandaa.stream;

import edu.cmu.pandaa.header.DistanceHeader;
import edu.cmu.pandaa.header.DistanceHeader.DistanceFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/14/11
 * Time: 4:25 PM
 */

public class DistanceFileStream extends FileStream {
  private DistanceHeader header;
  double sum, sumDiffSq, sumCnt;
  private final double AVG_WEIGHT = 0.95;

  public DistanceFileStream() throws Exception {
  }

  public DistanceFileStream(String filename) throws Exception {
    super(filename);
  }

  public DistanceFileStream(String filename, boolean overwrite) throws Exception {
    super(filename, overwrite);
  }

  @Override
  public void setHeader(StreamHeader h) throws Exception {
    super.setHeader(h);
    header = (DistanceHeader) h;
    writeValue("window", header.getRollingWindow());
  }

  @Override
  public void sendFrame(StreamFrame f) throws Exception {
    DistanceFrame frame = (DistanceFrame) f;
    if (f == null || frame.peakDeltas == null || frame.peakDeltas.length == 0) {
      return;
    }
    super.sendFrame(f);
    writeArray("dist");
    for (int i = 0;i < frame.peakDeltas.length; i++) {
      writeArrayObject();
      writeValue("delta", frame.peakDeltas[i]);
      writeValue("mag", frame.peakMagnitudes[i]);
      writeValue("raw", frame.rawValues[i]);

      double statVal = frame.peakDeltas[i];
      sum = sum * AVG_WEIGHT + statVal;
      sumCnt = sumCnt * AVG_WEIGHT + 1;
      double diff = (sum / sumCnt) - statVal;
      sumDiffSq = sumDiffSq * AVG_WEIGHT + diff*diff;
    }
    writeEndArray();
  }

  public void close() {
    try {
      super.sendFrame(header.makeFrame(-1));
      double avg = sum / sumCnt;
      writeValue("avg", avg);
      writeValue("stdev", Math.sqrt(sumDiffSq/(sumCnt-1)));
      super.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public DistanceHeader getHeader() throws Exception {
    StreamHeader prototype = super.getHeader();
    header = new DistanceHeader(prototype, consumeInt());
    return header;
  }

  @Override
  public DistanceHeader.DistanceFrame recvFrame() throws Exception {
    StreamFrame prototype = super.recvFrame();
    if (prototype == null) {
      return null;
    }
    ArrayList<Double> offsets = new ArrayList<Double>();
    ArrayList<Double> mags = new ArrayList<Double>();
    ArrayList<Double> raw = new ArrayList<Double>();
    while (hasMoreData()) {
      offsets.add(consumeDouble());
      mags.add(consumeDouble());
      raw.add(consumeDouble());
    }
    return header.makeFrame(prototype, offsets, mags, raw);
  }

  public static void main(String[] args) throws Exception {
    String filename = "test.txt";

    double[] data1 = { 1, 2, 3 };
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

