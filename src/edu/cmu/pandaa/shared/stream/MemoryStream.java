package edu.cmu.pandaa.shared.stream;

import edu.cmu.pandaa.shared.stream.header.StreamHeader;
import edu.cmu.pandaa.shared.stream.header.StreamHeader.StreamFrame;

// a memory buffer
public class MemoryStream implements FrameStream {

  private StreamHeader headerBuffer;
  private StreamFrame frameBuffer;

  public void setHeader(StreamHeader h) {
    headerBuffer = h;
    notify();     // if receiver is waiting for header, wake up
  }

  public StreamHeader getHeader() {
    if (headerBuffer == null) {
      try {
        wait();   // sleep until there's a header
      } 
      catch (InterruptedException e) { 
        e.printStackTrace();
      }
    }
    return headerBuffer;
  }

  public void sendFrame(StreamFrame f) throws Exception {
    if (frameBuffer == null) {
      frameBuffer = f;
      notify();   // if receiver is sleeping, wake up 
    } 
    else {
      throw new Exception("Frame Buffer full");
    }
  }

  public StreamFrame recvFrame() {
    if (frameBuffer == null) {
      try {
        wait();
      } 
      catch (InterruptedException e) { 
        e.printStackTrace();
      }
    }
    
    StreamFrame f = frameBuffer;
    frameBuffer = null;
    return f;
  }
}