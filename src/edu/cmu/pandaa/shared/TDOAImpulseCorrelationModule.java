package edu.cmu.pandaa.shared;

import edu.cmu.pandaa.shared.stream.FrameStream;
import edu.cmu.pandaa.shared.stream.StreamModule;
import edu.cmu.pandaa.shared.stream.header.DistanceHeader;
import edu.cmu.pandaa.shared.stream.header.DistanceHeader.DistanceFrame;
import edu.cmu.pandaa.shared.stream.header.FeatureHeader;
import edu.cmu.pandaa.shared.stream.header.FeatureHeader.FeatureFrame;
import edu.cmu.pandaa.shared.stream.header.StreamHeader;
import edu.cmu.pandaa.shared.stream.header.StreamHeader.StreamFrame;

class TDOAImpulseCorrelationModule implements StreamModule {
  FrameStream inFeatureFrameStream1;
  FrameStream inFeatureFrameStream2;
  FrameStream outDistanceFrameStream;
  
  TDOAImpulseCorrelationModule(FrameStream inFeatureFrameStream1, FrameStream inFeatureFrameStream2, FrameStream outDistanceFrameStream) {
    this.inFeatureFrameStream1 = inFeatureFrameStream1;
    this.inFeatureFrameStream2 = inFeatureFrameStream2;
    this.outDistanceFrameStream = outDistanceFrameStream;
  }
  
  public void run() throws Exception {
    StreamHeader header = init(inFeatureFrameStream1.getHeader());
    outDistanceFrameStream.setHeader(header);
    
    StreamFrame frameIn1, frameIn2, frameOut;
    while ((frameIn1 = inFeatureFrameStream1.recvFrame()) != null && (frameIn2 = inFeatureFrameStream2.recvFrame()) != null) {
      frameOut = process(frameIn1, frameIn2);
      outDistanceFrameStream.sendFrame(frameOut);
    }

    close();
  }
  
  public StreamHeader init(StreamHeader inHeader) {
    if (!(inHeader instanceof FeatureHeader))
      throw new RuntimeException("Wrong header type");

    // TODO: would actually do work here to compute new header
    return new DistanceHeader();
  }

  public StreamFrame process(StreamFrame inFrame1, StreamFrame inFrame2) {
    if (!(inFrame1 instanceof FeatureFrame) || !(inFrame2 instanceof FeatureFrame))
      throw new RuntimeException("Wrong frame type");

    // TODO: Would actually do work here to compute new frame
    return new DistanceFrame();
  }

  public void close() {
  }
}