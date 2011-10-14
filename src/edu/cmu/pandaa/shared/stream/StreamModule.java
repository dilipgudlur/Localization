package edu.cmu.pandaa.shared.stream;

import edu.cmu.pandaa.shared.stream.header.StreamHeader;
import edu.cmu.pandaa.shared.stream.header.StreamHeader.StreamFrame;

interface StreamModule {
  StreamHeader init(StreamHeader inHeader);
  StreamFrame process(StreamFrame inFrame);
  void close();
}
