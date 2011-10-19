package edu.cmu.pandaa.shared.stream;

import edu.cmu.pandaa.shared.stream.header.StreamHeader;
import edu.cmu.pandaa.shared.stream.header.StreamHeader.StreamFrame;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/18/11
 * Time: 4:36 PM
 */

public class DummyStream implements FrameStream {
  final String id;
  StreamHeader header;
  int count;

  public DummyStream(String id) {
    this.id = id;
    header = new StreamHeader(id, System.currentTimeMillis(), 100);
  }

  // set/write the header
  public void setHeader(StreamHeader h) throws Exception {
    System.out.println("Header out " + h.id);

  }

  // send a frame of data
  public void sendFrame(StreamFrame m) throws Exception {
    System.out.println("Frame out " + m.getHeader().id + "-" + m.seqNum);
  }

  // will block until there's a header (should be set first thing anyway)
  public StreamHeader getHeader() throws Exception {
    return header;
  }

  // will block until there's a frame available
  public StreamHeader.StreamFrame recvFrame() throws Exception {
    if (count++ > 100) {
      header = null;
      return null;
    }
    Thread.sleep(header.frameTime);  // simulate actual data stream
    return header.makeFrame();
  }

  // indicates we're done with the stream
  public void close() throws Exception {
    header = null;
  }
}
