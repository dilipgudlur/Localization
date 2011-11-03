package edu.cmu.pandaa.stream;

import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/18/11
 * Time: 4:36 PM
 */

public class DummyStream implements FrameStream {
  final String id;
  final StreamHeader header;
  int count;

  public DummyStream(String id) {
    this.id = id;
    header = new StreamHeader(id, System.currentTimeMillis(), 100);
  }

  public DummyStream(StreamHeader header) {
    this.id = header.id;
    this.header = header;
  }

  // set/write the header
  @Override
  public void setHeader(StreamHeader h) throws Exception {
    System.out.println("Header out " + h.id + " type " + h.getClass().getSimpleName());
  }

  // send a frame of data
  @Override
  public void sendFrame(StreamFrame m) throws Exception {
    System.out.println("Frame out " + m.toString());
  }

  // will block until there's a header (should be set first thing anyway)
  @Override
  public StreamHeader getHeader() throws Exception {
    return header;
  }

  // will block until there's a frame available
  @Override
  public StreamHeader.StreamFrame recvFrame() throws Exception {
    if (count++ > 100) {
      return null;
    }
    Thread.sleep(header.frameTime);  // simulate actual data stream
    return header.makeFrame();
  }

  // indicates we're done with the stream
  @Override
  public void close() throws Exception {
    count = 100;
  }
}
