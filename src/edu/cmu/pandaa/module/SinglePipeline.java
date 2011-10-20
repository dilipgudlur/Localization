package edu.cmu.pandaa.module;

import edu.cmu.pandaa.frame.StreamHeader;
import edu.cmu.pandaa.frame.StreamHeader.StreamFrame;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/18/11
 * Time: 4:00 PM
 */

public class SinglePipeline implements StreamModule {
  StreamHeader outHeader;
  @Override
public StreamHeader init(StreamHeader inHeader) {
    outHeader = new StreamHeader(inHeader.id + "-imp", inHeader.startTime, inHeader.frameTime);
    return outHeader;
  }

  @Override
public StreamFrame process(StreamFrame inFrame) {
    if (inFrame == null) {
      return null;
    }
    return outHeader.makeFrame();
  }

  @Override
public void close() {
  }
}
