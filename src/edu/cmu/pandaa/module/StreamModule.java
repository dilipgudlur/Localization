package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;

public interface StreamModule {
  StreamHeader init(StreamHeader inHeader) throws Exception;
  StreamFrame process(StreamFrame inFrame) throws Exception;
  void close();
}