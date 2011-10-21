package edu.cmu.pandaa.framework;

import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.module.DummyModule;
import edu.cmu.pandaa.module.StreamModule;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/18/11
 * Time: 4:00 PM
 */

public class SinglePipeline implements StreamModule {
  final long now = System.currentTimeMillis();
  final int frameTime = 100;

  /* First step is to take in RawAudioFrames and convert them to impulse frames */
  StreamModule impulse = new DummyModule(new ImpulseHeader("dummyImpulse", now, frameTime));

  /* Then we consolidate bunches of impulse frames into larger consolidated frames for processing */
  StreamModule consolidate = new DummyModule();

  @Override
  public StreamHeader init(StreamHeader inHeader) {
    if (!(inHeader instanceof RawAudioHeader)) {
      throw new IllegalArgumentException("Requires RawAudioHeader");
    }
    StreamHeader header = impulse.init(inHeader);
    header = consolidate.init(header);
    if (!(header instanceof ImpulseHeader)) {
      throw new IllegalArgumentException("Output should be ImpulseHeader");
    }
    return header;
  }

  @Override
  public StreamFrame process(StreamFrame inFrame) {
    if (inFrame == null) {
      return null;
    }
    StreamFrame frame = impulse.process(inFrame);
    frame = consolidate.process(frame);
    return frame;
  }

  @Override
  public void close() {
  }
}
