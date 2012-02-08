package edu.cmu.pandaa.framework;

import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.module.ConsolidateModule;
import edu.cmu.pandaa.module.FeatureStreamModule;
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
  FeatureStreamModule impulse = new FeatureStreamModule(); //new DummyModule(new ImpulseHeader("dummyImpulse", now, frameTime));

  FileStream trace;

  @Override
  public StreamHeader init(StreamHeader inHeader) throws Exception {
    if (!(inHeader instanceof RawAudioHeader))  {
      throw new IllegalArgumentException("Requires RawAudioHeader");
    }

    impulse.augmentedAudio(App.TRACE_DIR + inHeader.id + "-%d-augment.wav");
    StreamHeader header = impulse.init(inHeader);
    if (!(header instanceof ImpulseHeader)) {
      throw new IllegalArgumentException("Output should be ImpulseHeader");
    }

    trace = new ImpulseFileStream(App.TRACE_DIR + inHeader.id + ".txt", true);
    trace.setHeader(header);

    return header;
  }

  @Override
  public StreamFrame process(StreamFrame frame) throws Exception {
    if (frame == null) {
      return null;
    }
    frame = impulse.process(frame);
    trace.sendFrame(frame);
    return frame;
  }

  @Override
  public void close() {
    trace.close();
    impulse.close();
  }
}
