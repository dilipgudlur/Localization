package edu.cmu.pandaa.module;

import edu.cmu.pandaa.frame.StreamHeader;
import edu.cmu.pandaa.frame.StreamHeader.StreamFrame;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/18/11
 * Time: 4:12 PM
 */

public class MergePipeline implements StreamModule {
  StreamHeader outHeader;
  public StreamHeader init(StreamHeader inHeader) {
    outHeader = new StreamHeader(inHeader.id + "-comb", inHeader.startTime, inHeader.frameTime);
    return outHeader;
  }

  public StreamFrame process(StreamFrame inFrame) {
    if (inFrame == null) {
      return null;
    }
    return outHeader.makeFrame();
  }

  public void close() {
  }
}
