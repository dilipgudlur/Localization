package com.google.cmusv.pandaa.stream;

import com.google.cmusv.pandaa.stream.FrameStream.LocalFrameStream;

// a memory buffer
public class MemoryStream implements LocalFrameStream {

  private Header headerBuffer;
  private Frame frameBuffer;

  public void setHeader(Header h) {
    headerBuffer = h;
  }

  public Header getHeader() {
    return headerBuffer;
  }

  public synchronized void sendFrame(Frame m) throws Exception {
    if (frameBuffer == null) {
      frameBuffer = m;
      notify(); // if receiver is sleeping, wake up 
    } 
    else {
      throw new Exception("Frame Buffer full");
    }
  }

  public synchronized Frame recvFrame() {
    if (frameBuffer == null) {
      try {
        wait();
      } 
      catch (InterruptedException e) { 
        /* some logging */
      }
    }
    
    Frame f = frameBuffer;
    frameBuffer = null;
    return f;
  }
}