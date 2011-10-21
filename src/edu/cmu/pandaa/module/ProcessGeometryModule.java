package edu.cmu.pandaa.module;

import edu.cmu.pandaa.stream.FrameStream;
import edu.cmu.pandaa.stream.GeometryFileStream;
import edu.cmu.pandaa.module.StreamModule;
import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.GeometryHeader.GeometryFrame;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import mdsj.*;

class ProcessGeometryModule implements StreamModule, Runnable{
	FrameStream inGeometryStream, outGeometryStream;
	GeometryHeader hOut;	

  public ProcessGeometryModule(FrameStream inGeometryStream, FrameStream outGeometryStream)
  {
	  this.inGeometryStream = inGeometryStream;
	  this.outGeometryStream = outGeometryStream;
  }
  
  public void run() {
	  try{
		  StreamHeader header = init(inGeometryStream.getHeader());
		  outGeometryStream.setHeader(header);
		  StreamFrame frameIn,frameOut;
		  while ((frameIn = inGeometryStream.recvFrame()) != null) {
			  frameOut = process(frameIn);
		      outGeometryStream.sendFrame(frameOut);
		  }
	  }catch(Exception e){
		  e.printStackTrace();
	  }
	  close();
  }

  public StreamHeader init(StreamHeader inHeader) {
    if (!(inHeader instanceof GeometryHeader))
      throw new RuntimeException("Wrong header type");
    
    /*compute new header*/
    GeometryHeader hIn = (GeometryHeader)inHeader ;
   	hOut = new GeometryHeader(hIn.deviceIds, hIn.startTime, hIn.frameTime);
    return hOut;
  }

  public StreamFrame process(StreamFrame inFrame) {
    if (!(inFrame instanceof GeometryFrame))
      throw new RuntimeException("Wrong frame type");
    
    GeometryFrame gfIn = (GeometryFrame) inFrame ;
    GeometryFrame gfOut = hOut.makeFrame(gfIn.seqNum, gfIn.geometry); //TODO:verify correctness of hOut    
    gfOut.geometry = MDSJ.classicalScaling(gfIn.geometry); // apply MDS
	return gfOut ;    
  }
  
  public static void main(String[] args) throws Exception
  {
	  GeometryFileStream gIn = new GeometryFileStream("gIn.txt");
	  GeometryFileStream gOut = new GeometryFileStream("gOut.txt");;
	  ProcessGeometryModule pgm = new ProcessGeometryModule(gIn,gOut);
	  Thread th = new Thread(pgm);
	  th.run();	  
  }
  
  public void close() {
  }
}