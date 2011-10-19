package edu.cmu.pandaa.server;

import edu.cmu.pandaa.frame.GeometryHeader;
import edu.cmu.pandaa.frame.GeometryHeader.GeometryFrame;
import edu.cmu.pandaa.frame.StreamHeader;
import edu.cmu.pandaa.frame.StreamHeader.StreamFrame;
import edu.cmu.pandaa.module.StreamModule;
import edu.cmu.pandaa.stream.FrameStream;

class ProcessGeometryModule implements StreamModule{
  FrameStream inGeometryStream, outGeometryStream;

  public ProcessGeometryModule(FrameStream inGeometryStream, FrameStream outGeometryStream)
  {
	  this.inGeometryStream = inGeometryStream;
	  this.outGeometryStream = outGeometryStream;
  }
  
  public void run() throws Exception {
	  StreamHeader header = init(inGeometryStream.recvHeader());
	  outGeometryStream.sendHeader(header);
	  StreamFrame frameIn,frameOut;
	  while ((frameIn = inGeometryStream.recvFrame()) != null) {
		  frameOut = process(frameIn);
	      outGeometryStream.sendFrame(frameOut);
	  }
	  close();
  }

  public StreamHeader init(StreamHeader inHeader) {
    if (!(inHeader instanceof GeometryHeader))
      throw new RuntimeException("Wrong header type");
    
    // TODO: would actually do work here to compute new header
    //not sure how to go about this part
    return null;
  }

  public StreamFrame process(StreamFrame inFrame) {
    if (!(inFrame instanceof GeometryFrame))
      throw new RuntimeException("Wrong frame type");
    
    GeometryFrame gf = (GeometryFrame) inFrame ;    
    gf.geometry = MDSJ.classicalScaling(gf.geometry); // apply MDS
	return gf ;
    
  }

  public void close() {
  }
}
