package edu.cmu.pandaa.shared.stream;

import edu.cmu.pandaa.shared.stream.header.StreamHeader;
import edu.cmu.pandaa.shared.stream.header.StreamHeader.StreamFrame;

import java.io.IOException;

public interface FrameStream {
  // set/write the header
  public void sendHeader(StreamHeader h) throws Exception;

  // send a frame of data
  public void sendFrame(StreamFrame m) throws Exception;

  // will block until there's a header (should be set first thing anyway)
  public StreamHeader recvHeader() throws Exception;

  // will block until there's a frame available
  public StreamFrame recvFrame() throws Exception;

}