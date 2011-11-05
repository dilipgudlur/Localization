package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;

public interface StreamModule {
  StreamHeader init(StreamHeader inHeader);
  StreamFrame process(StreamFrame inFrame);
  void close();
}