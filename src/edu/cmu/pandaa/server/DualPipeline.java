package edu.cmu.pandaa.server;

import edu.cmu.pandaa.shared.stream.StreamModule;
import edu.cmu.pandaa.shared.stream.header.MultiHeader;
import edu.cmu.pandaa.shared.stream.header.StreamHeader;
import edu.cmu.pandaa.shared.stream.header.StreamHeader.StreamFrame;

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
