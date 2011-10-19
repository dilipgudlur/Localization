package edu.cmu.pandaa.stream;

import edu.cmu.pandaa.frame.MultiHeader;
import edu.cmu.pandaa.frame.MultiHeader.MultiFrame;
import edu.cmu.pandaa.frame.StreamHeader;
import edu.cmu.pandaa.frame.StreamHeader.StreamFrame;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/14/11
 * Time: 9:33 PM
 */

public class DemultiplexStream implements FrameStream {
  private final MultiHeader header;
  private MultiFrame frame;

  public DemultiplexStream(StreamHeader[] headers) {
    header = new MultiHeader(headers);
  }

  // set/write the header
  public void sendHeader(StreamHeader h) throws Exception {
    if (header.position(h) < 0) {
      throw new Exception("Header not part of collection");
    }
  }

  // send a frame of data
  public void sendFrame(StreamFrame m) throws Exception {
    if (frame == null) {
      frame = header.makeFrame();
    }
    // TODO: Logic here to actually figure out how to combine frames
  }

  // will block until there's a header (should be set first thing anyway)
  public MultiHeader recvHeader() throws Exception {
    return header;
  }

  // will block until there's a frame available
  public MultiFrame recvFrame() throws Exception {
    if (frame == null) {
      throw new Exception("Recv frame not ready");
    }
    MultiFrame ret = frame;
    frame = null;
    return ret;
  }

  public void close() {
  }

  public static void main(String[] args) throws Exception {
    StreamHeader[] headers = new StreamHeader[10];
    for (int i = 0; i < headers.length; i++) {
      headers[i] = new StreamHeader("id" + i, System.currentTimeMillis(), 100);
      Thread.sleep(1); // make start times different, simulating reality
    }

    DemultiplexStream dm = new DemultiplexStream(headers);

    for (StreamHeader h : headers) {
      dm.sendHeader(h);
    }
    MultiHeader mh = dm.recvHeader();
    System.out.println(mh.toString());

    for (int i = 0; i < 10; i++) {
      for (StreamHeader h : headers) {
        dm.sendFrame(h.makeFrame());
      }
      MultiFrame sf = dm.recvFrame();
      System.out.println(sf.toString());
    }

    dm.close();
  }
}
