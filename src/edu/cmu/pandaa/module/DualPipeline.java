package edu.cmu.pandaa.module;

import edu.cmu.pandaa.frame.MultiHeader;
import edu.cmu.pandaa.frame.StreamHeader;
import edu.cmu.pandaa.frame.StreamHeader.StreamFrame;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/18/11
 * Time: 4:11 PM
 */

public class DualPipeline implements StreamModule {
  StreamHeader outHeader;
  public StreamHeader init(StreamHeader inHeader) {
    MultiHeader mh = (MultiHeader) inHeader;
    outHeader = new StreamHeader(mh.id + "-mix", inHeader.startTime, inHeader.frameTime);
    return outHeader;
  }

  public StreamHeader.StreamFrame process(StreamFrame inFrame) {
    if (inFrame == null) {
      return null;
    }
    return outHeader.makeFrame();
  }

  public void close() {
  }
}
