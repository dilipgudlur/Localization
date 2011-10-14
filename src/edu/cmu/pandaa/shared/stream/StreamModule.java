package edu.cmu.pandaa.shared.stream;

import edu.cmu.pandaa.shared.stream.header.StreamHeader;
import edu.cmu.pandaa.shared.stream.header.StreamHeader.StreamFrame;

public interface StreamModule {
  StreamHeader init(StreamHeader inHeader);
  StreamFrame process(StreamFrame inFrame);
  void close();
}
