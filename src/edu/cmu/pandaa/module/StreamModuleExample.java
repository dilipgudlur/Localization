package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.FrameStream;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/11/11
 * Time: 4:30 PM
 */

class StreamModuleExample implements StreamModule {
  FrameStream in, out;

  /* Example1: How this interface would be used to chain two processes together */
  public void go(StreamModule m1, StreamModule m2) throws Exception {
    StreamHeader header = in.getHeader();
    header = m1.init(header);
    header = m2.init(header);
    out.setHeader(header);

    StreamFrame frame;
    while ((frame = in.recvFrame()) != null) {
      frame = m1.process(frame);
      frame = m2.process(frame);
      out.sendFrame(frame);
    }

    m1.close();
    m2.close();
  }

  /* Example2: A run method that could be used to create a new thread to test just this class */
  public void run() {
    try {
      out.setHeader(init(in.getHeader()));
      while (true)
        out.sendFrame(process(in.recvFrame()));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
public StreamHeader init(StreamHeader inHeader) {
    if (!(inHeader instanceof RawAudioHeader))
      throw new RuntimeException("Wrong header type");

    // TODO: would actually do work here to compute new header
    return null;
  }

  @Override
public StreamFrame process(StreamFrame inFrame) {
    if (!(inFrame instanceof RawAudioFrame))
      throw new RuntimeException("Wrong frame type");

    // TODO: Would actually do work here to compute new frame
    return null;
  }

  @Override
public void close() {
  }
}
