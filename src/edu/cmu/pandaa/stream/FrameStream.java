package edu.cmu.pandaa.stream;

import edu.cmu.pandaa.frame.StreamHeader;
import edu.cmu.pandaa.frame.StreamHeader.StreamFrame;

public interface FrameStream {
  // set/write the header
  public void setHeader(StreamHeader h) throws Exception;

  // send a frame of data
  public void sendFrame(StreamFrame m) throws Exception;

  // will block until there's a header (should be set first thing anyway)
  public StreamHeader getHeader() throws Exception;

  // will block until there's a frame available
  public StreamFrame recvFrame() throws Exception;

  // indicates we're done with the stream
  public void close() throws Exception;
}