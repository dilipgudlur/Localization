package edu.cmu.pandaa.module;

import mdsj.MDSJ;
import edu.cmu.pandaa.header.DistanceHeader;
import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.GeometryHeader.GeometryFrame;
import edu.cmu.pandaa.header.DistanceHeader.DistanceFrame;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.DistanceFileStream;
import edu.cmu.pandaa.stream.FrameStream;
import edu.cmu.pandaa.stream.GeometryFileStream;


public class RMSModule implements StreamModule{
	DistanceHeader dOut;
	
	public void runModule(FrameStream inGeometryStream, FrameStream outDistanceStream) throws Exception {
	    try{
	      StreamHeader header = init(inGeometryStream.getHeader());
	      outDistanceStream.setHeader(header);
	      StreamFrame frameIn,frameOut;
	      while ((frameIn = inGeometryStream.recvFrame()) != null) {
	        frameOut = process(frameIn);
	        outDistanceStream.sendFrame(frameOut);
	      }
	    }catch(Exception e){
	      e.printStackTrace();
	    }
	    outDistanceStream.close();
	  }
	
	public StreamHeader init(StreamHeader inHeader) {
	    if (!(inHeader instanceof GeometryHeader))
	      throw new RuntimeException("Wrong header type");

	    GeometryHeader hIn = (GeometryHeader)inHeader ;
	    String id = "";
	    for(int i = 0; i < hIn.deviceIds.length; i++)
	    {
	    	id += hIn.deviceIds[i]; 
	    } 
	    dOut = new DistanceHeader(id, hIn.startTime, hIn.frameTime, hIn.deviceIds);
	    return dOut;
	  }
	
	public StreamFrame process(StreamFrame inFrame) {
	    if (!(inFrame instanceof GeometryFrame))
	      throw new RuntimeException("Wrong frame type");
	    GeometryFrame gfIn = (GeometryFrame) inFrame ;
	    double[] x=null,y=null;
	    
	    DistanceFrame dfOut = dOut.makeFrame(x,y);
	    return dfOut ;
	  }

	
	public static void main(String[] args) throws Exception
	{
		int arg = 0;
	    String outArg = args[arg++];
	    String inArg1 = args[arg++];
	    String inArg2 = args[arg++];
	    if (args.length > arg || args.length < arg) {
	      throw new IllegalArgumentException("Invalid number of arguments");
	    }
		
	    System.out.println("RMS: " + outArg + " " + inArg1 + " " + inArg2);
	    
	    GeometryFileStream rIn1 = new GeometryFileStream(inArg1);
	    GeometryFileStream rIn2 = new GeometryFileStream(inArg2);
	    DistanceFileStream rOut = new DistanceFileStream(outArg, true);

	    
	}
	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

}
