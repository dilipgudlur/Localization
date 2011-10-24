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
 * Time: 4:11 PM
 */

public class DualPipeline implements StreamModule {
  /* fileds only used for dummy distance header -- remove when we have real code */
  final long now = System.currentTimeMillis();
  final int frameTime = 100;

  /* First step is to take in a multiFrame consisting of impulseframes and turn it into time differences */
  StreamModule tdoa = new DummyModule(new DistanceHeader("dummyDistance", now, frameTime));

  /* Then we consolidate bunches of impulse frames into larger consolidated frames for processing */
  StreamModule smooth = new DummyModule();

  @Override
  public StreamHeader init(StreamHeader inHeader) {
    MultiHeader multiHeader = (MultiHeader) inHeader;
    if (!(multiHeader.getOne() instanceof ImpulseHeader)) {
       throw new IllegalArgumentException("Merge pipe multiheader should contain ImpulseHeaders");
    }
    StreamHeader header = tdoa.init(inHeader);
    header = smooth.init(header);
    if (!(header instanceof DistanceHeader)) {
      throw new IllegalArgumentException("Output should be ImpulseHeader");
    }
    return header;
  }

  @Override
  public StreamFrame process(StreamFrame inFrame) {
    if (inFrame == null) {
      return null;
    }
    StreamFrame frame = tdoa.process(inFrame);
    frame = smooth.process(frame);
    return frame;
  }

  @Override
  public void close() {
  }
}