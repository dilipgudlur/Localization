package edu.cmu.pandaa.shared.stream;

import java.io.Serializable;

public interface FrameStream {
  public void setHeader(Header h);                  // non-blocking
  public Header getHeader();                        // will block until the header is set
  
  public void sendFrame(Frame m) throws Exception;  // non-blocking
  public Frame recvFrame();                         // will block until a frame is available

  class Frame implements Serializable {
    public int seqNum;      // automatically set by FrameStream implementation
  }
  
  class Header implements Serializable {
    public long startTime;  // client start time, ala System.currentTimeMillis()
    public long frameTime;  // duration of each frame, measured in ms
  }
}