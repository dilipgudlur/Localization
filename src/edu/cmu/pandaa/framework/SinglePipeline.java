package edu.cmu.pandaa.framework;

import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.module.ConsolidateModule;
import edu.cmu.pandaa.module.ImpulseStreamModule;
import edu.cmu.pandaa.module.StreamModule;
import edu.cmu.pandaa.stream.FileStream;
import edu.cmu.pandaa.stream.ImpulseFileStream;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/18/11
 * Time: 4:00 PM
 */

public class SinglePipeline implements StreamModule {

  /* First step is to take in RawAudioFrames and convert them to impulse frames */
  StreamModule impulse = new ImpulseStreamModule(); //new DummyModule(new ImpulseHeader("dummyImpulse", now, frameTime));

  /* Then we consolidate bunches of impulse frames into larger consolidated frames for processing */
  StreamModule consolidate = new ConsolidateModule('i', 1, 1, 1, 1);

  FileStream trace;

  @Override
  public StreamHeader init(StreamHeader inHeader) throws Exception {
    if (!(inHeader instanceof RawAudioHeader)) {
      throw new IllegalArgumentException("Requires RawAudioHeader");
    }
    StreamHeader header = impulse.init(inHeader);
    header = consolidate.init(header);
    if (!(header instanceof ImpulseHeader)) {
      throw new IllegalArgumentException("Output should be ImpulseHeader");
    }

    trace = new ImpulseFileStream(inHeader.id + ".txt", true);
    trace.setHeader(header);

    return header;
  }

  @Override
  public StreamFrame process(StreamFrame inFrame) throws Exception {
    if (inFrame == null) {
      return null;
    }

    StreamFrame frame = impulse.process(inFrame);
    frame = consolidate.process(frame);

    trace.sendFrame(frame);

    return frame;
  }

  @Override
  public void close() {
    trace.close();
  }
}
