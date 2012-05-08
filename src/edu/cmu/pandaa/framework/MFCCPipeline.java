package edu.cmu.pandaa.framework;

import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.MatrixHeader;
import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.module.FeatureStreamModule;
import edu.cmu.pandaa.module.MFCCModule;
import edu.cmu.pandaa.module.StreamModule;
import edu.cmu.pandaa.stream.FileStream;
import edu.cmu.pandaa.stream.ImpulseFileStream;
import edu.cmu.pandaa.stream.MatrixFileStream;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/18/11
 * Time: 4:00 PM
 */

public class MFCCPipeline implements StreamModule {

  MFCCModule mfcc = new MFCCModule();

  FileStream trace;

  @Override
  public StreamHeader init(StreamHeader inHeader) throws Exception {
    if (!(inHeader instanceof RawAudioHeader))  {
      throw new IllegalArgumentException("Requires RawAudioHeader");
    }

    StreamHeader header = mfcc.init(inHeader);

    if (!(header instanceof MatrixHeader)) {
      throw new IllegalArgumentException("Output should be MatrixHeader");
    }

    trace = new MatrixFileStream(App.TRACE_DIR + inHeader.id + ".txt", true);
    trace.setHeader(header);

    return header;
  }

  @Override
  public StreamFrame process(StreamFrame frame) throws Exception {
    if (frame == null) {
      return null;
    }
    frame = mfcc.process(frame);
    trace.sendFrame(frame);
    return frame;
  }

  @Override
  public void close() {
    trace.close();
    mfcc.close();
  }
}
