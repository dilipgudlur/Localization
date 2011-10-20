package edu.cmu.pandaa.stream;

import edu.cmu.pandaa.frame.StreamHeader;
import edu.cmu.pandaa.frame.StreamHeader.StreamFrame;

// a memory buffer
public class MemoryStream implements FrameStream {
  private boolean isActive = true;
  private StreamHeader headerBuffer;
  private StreamFrame frameBuffer;

  @Override
public synchronized void setHeader(StreamHeader h) {
    headerBuffer = h;
    notify();     // if receiver is waiting for header, wake up
  }

  @Override
public synchronized StreamHeader getHeader() {
    while (headerBuffer == null && isActive) {
      try {
        wait();   // sleep until there's a header
      }
      catch (InterruptedException e) {
        return null;
      }
    }
    return isActive ? headerBuffer : null;
  }

  @Override
public synchronized void sendFrame(StreamFrame f) {
    if (frameBuffer == null) {
      if (f == null) {
        throw new NullPointerException();
      }
      frameBuffer = f;
      notify();   // if receiver is sleeping, wake up 
    }
    else {
      throw new RuntimeException("Frame Buffer full");
    }
  }

  @Override
public synchronized StreamFrame recvFrame() throws Exception {
    while (frameBuffer == null && isActive) {
      wait();
    }

    StreamFrame f = frameBuffer;
    frameBuffer = null;
    return f;
  }

  @Override
public synchronized void close() {
    isActive = false;
    notify();
  }
}