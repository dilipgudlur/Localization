package edu.cmu.pandaa.module;

import edu.cmu.pandaa.frame.StreamHeader;
import edu.cmu.pandaa.frame.StreamHeader.StreamFrame;

public interface StreamModule {
  StreamHeader init(StreamHeader inHeader);
  StreamFrame process(StreamFrame inFrame);
  void close();
}
