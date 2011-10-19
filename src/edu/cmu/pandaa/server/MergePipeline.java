package edu.cmu.pandaa.server;

import edu.cmu.pandaa.shared.stream.StreamModule;
import edu.cmu.pandaa.shared.stream.header.StreamHeader;
import edu.cmu.pandaa.shared.stream.header.StreamHeader.StreamFrame;

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

  public StreamHeader.StreamFrame process(StreamFrame inFrame) {
    if (inFrame == null) {
      return null;
    }
    return outHeader.makeFrame();
  }

  public void close() {
  }
}
