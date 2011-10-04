package edu.cmu.pandaa.shared.stream;

// a memory buffer
public class MemoryStream implements FrameStream {

  private Header headerBuffer;
  private Frame frameBuffer;

  public void setHeader(Header h) {
    headerBuffer = h;
    notify();   // if receiver is waiting for header, wake up
  }

  public Header getHeader() {
    if (headerBuffer == null) {
      try {
        wait();   // sleep until there's a header
      } 
      catch (InterruptedException e) { e.printStackTrace(); }
    }
    return headerBuffer;
  }

  public synchronized void sendFrame(Frame m) throws Exception {
    if (frameBuffer == null) {
      frameBuffer = m;
      notify();   // if receiver is sleeping, wake up 
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
      catch (InterruptedException e) { e.printStackTrace(); }
    }
    
    Frame f = frameBuffer;
    frameBuffer = null;
    return f;
  }
}