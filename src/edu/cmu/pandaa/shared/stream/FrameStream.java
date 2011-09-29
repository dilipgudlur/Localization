package com.google.cmusv.pandaa.stream;

import java.io.Serializable;

public interface FrameStream {
  public void setHeader(Header h);
  public Header getHeader();

  public Frame recvFrame(); // will block until ready

  class Frame implements Serializable {
    public int seqNum; // automatically set by FrameStream implementation
  }
  
  class Header implements Serializable {
    public long startTime; // client start time, ala System.currentTimeMillis()
    public long frameTime; // duration of each frame, measured in ms
  }

  interface LocalFrameStream extends FrameStream {  
    public void sendFrame(Frame m) throws Exception; // should be non-blocking
  }
  
  interface NetworkFrameStream extends FrameStream {  
    public void sendFrame(Frame m, String address, int port);
  }
}