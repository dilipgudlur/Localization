package edu.cmu.pandaa.shared.stream;

import edu.cmu.pandaa.shared.stream.header.StreamHeader;
import edu.cmu.pandaa.shared.stream.header.StreamHeader.StreamFrame;

public interface FrameStream {
  public void setHeader(StreamHeader h);
  public StreamHeader getHeader();          // will block until there's a header (should be set first thing anyway)

  public StreamFrame recvFrame();           // will block until there's a frame available
  public void sendFrame(StreamFrame m) throws Exception;
}