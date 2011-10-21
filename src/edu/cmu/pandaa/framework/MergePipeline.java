package edu.cmu.pandaa.framework;

import edu.cmu.pandaa.header.DistanceHeader;
import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.MultiHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.module.DummyModule;
import edu.cmu.pandaa.module.StreamModule;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/18/11
 * Time: 4:12 PM
 */

public class MergePipeline implements StreamModule {

  @Override
  public StreamHeader init(StreamHeader inHeader) {
    MultiHeader mh = (MultiHeader) inHeader;
    return mh;
  }

  @Override
  public StreamHeader.StreamFrame process(StreamFrame inFrame) {
    if (inFrame == null) {
      return null;
    }
    return inFrame;
  }

  @Override
  public void close() {
  }
}
