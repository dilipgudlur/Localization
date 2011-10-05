package edu.cmu.pandaa.shared.stream;

import edu.cmu.pandaa.shared.stream.GenericFrame.Frame;
import edu.cmu.pandaa.shared.stream.GenericFrame.Header;

public interface FrameStream {
  public void setHeader(Header h);
  public Header getHeader();

  public Frame recvFrame(); // will block until ready
  public void sendFrame(Frame m);
}