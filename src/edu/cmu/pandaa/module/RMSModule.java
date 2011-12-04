package edu.cmu.pandaa.module;

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
	StreamFrame sf;
	
  public void extractFrame(FrameStream inStream) throws Exception
  {
	  StreamHeader h = inStream.getHeader();
	  StreamFrame f = inStream.recvFrame();
	  if( f != null)
	  {
		setFrame(f);
	  }		
  }
	
  public void setFrame(StreamFrame f)
  {
	  this.sf = f;
  }
	
  public StreamFrame getFrame()
  {
	  return sf;
  }
	
  public void runModule(FrameStream inGS1,FrameStream inGS2,FrameStream outDS) throws Exception {
    try{
      StreamHeader header = init(inGS1.getHeader());	 
	  outDS.setHeader(header);
	  extractFrame(inGS2);
	  StreamFrame f1,frameOut;	      
	  while ((f1 = inGS1.recvFrame()) != null) {
	    frameOut = process(f1);
	    outDS.sendFrame(frameOut);
	  }
	}catch(Exception e){
		e.printStackTrace();
	}
	 outDS.close();
 }
	
  public StreamHeader init(StreamHeader i1) {
	if (!(i1 instanceof GeometryHeader))
	  throw new RuntimeException("Wrong header type");
	GeometryHeader hIn = (GeometryHeader)i1 ;
	String id = "";
	for(int i = 0; i < hIn.deviceIds.length; i++)
	{
	  id += hIn.deviceIds[i]; 
	} 
	dOut = new DistanceHeader(id, hIn.startTime, hIn.frameTime, hIn.deviceIds);
	return dOut;
  }
	
  public StreamFrame process(StreamFrame f1) {
	if (!(f1 instanceof GeometryFrame))
	  throw new RuntimeException("Wrong frame type");
	GeometryFrame gf1 = (GeometryFrame) f1 ; //actual
	GeometryFrame gf2 = (GeometryFrame) sf ; //estimated
	double[][] geoActual = gf1.geometry;
	double[][] geoEstimated = gf2.geometry;
	GeometryMatrixModule g = new GeometryMatrixModule();
	g.adjustAxes(geoEstimated); //for config file
	double[] x = {0.0},y = {0.0};
	double[] distanceActual = new double[geoActual[0].length];
	double[] distanceEstimated = new double[geoEstimated[0].length];
	
	for(int j = 0; j < geoEstimated[0].length; j++){
	  if(j == geoEstimated[0].length - 1)
	    distanceEstimated[j] =  Math.sqrt(Math.pow((geoEstimated[0][j] - geoEstimated[0][0]),2) + Math.pow((geoEstimated[1][j] - geoEstimated[1][0]),2));
	  else
		distanceEstimated[j] =  Math.sqrt(Math.pow((geoEstimated[0][j+1] - geoEstimated[0][j]),2) + Math.pow((geoEstimated[1][j+1] - geoEstimated[1][j]),2));	
	}
	  
	for(int j = 0; j < geoActual[0].length; j++){
	  if(j == geoActual[0].length - 1)
		distanceActual[j] =  Math.sqrt(Math.pow((geoActual[0][j] - geoActual[0][0]),2) + Math.pow((geoActual[1][j] - geoActual[1][0]),2));
	  else
		distanceActual[j] =  Math.sqrt(Math.pow((geoActual[0][j+1] - geoActual[0][j]),2) + Math.pow((geoActual[1][j+1] - geoActual[1][j]),2));	
	}
	
	double rms = 0;
	for(int i = 0; i < distanceActual.length; i++)
	  	rms += Math.pow((distanceActual[i] - distanceEstimated[i]),2);
	rms = Math.sqrt(rms / distanceActual.length);
	x[0] = rms;
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
    GeometryFileStream rIn1 = new GeometryFileStream(inArg1); //geometryOut
    GeometryFileStream rIn2 = new GeometryFileStream(inArg2); //config file
    DistanceFileStream rOut = new DistanceFileStream(outArg, true); //rmsOut
    
    try {
        RMSModule rms = new RMSModule();
        rms.runModule(rIn1,rIn2,rOut);
      } catch (Exception e) {
        e.printStackTrace();
      }
    
	}
	@Override
  public void close() {
		// TODO Auto-generated method stub
		
  }
}
