package edu.cmu.pandaa.module;

import edu.cmu.pandaa.stream.DummyStream;
import edu.cmu.pandaa.stream.FrameStream;
import edu.cmu.pandaa.stream.GeometryFileStream;
import edu.cmu.pandaa.module.StreamModule;
import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.GeometryHeader.GeometryFrame;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import mdsj.*;

class ProcessGeometryModule implements StreamModule{
  //FrameStream inGeometryStream, outGeometryStream;
  GeometryHeader hOut;

  public ProcessGeometryModule()
  {
  }

  public void runModule(FrameStream inGeometryStream, FrameStream outGeometryStream) throws Exception {
    try{
      StreamHeader header = init(inGeometryStream.getHeader());
      outGeometryStream.setHeader(header);
      StreamFrame frameIn,frameOut;
      while ((frameIn = inGeometryStream.recvFrame()) != null) {
        //if(frameIn != null){
        frameOut = process(frameIn);
        outGeometryStream.sendFrame(frameOut);
      }
    }catch(Exception e){
      e.printStackTrace();
    }
    outGeometryStream.close();
    //close();
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
    int arg = 0;
    String inArg = args[arg++];
    String outArg = args[arg++];
    if (args.length > arg || args.length < arg) {
      throw new IllegalArgumentException("Invalid number of arguments");
    }

    System.out.println("Consolidate " + inArg + " to " + outArg);
    GeometryFileStream gOut = new GeometryFileStream(outArg, true);
    GeometryFileStream gIn = new GeometryFileStream(inArg);

    try {
      ProcessGeometryModule pgm = new ProcessGeometryModule();
      pgm.runModule(gIn,gOut);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void close() {
  }
}