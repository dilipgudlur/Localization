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
	double[] prevX;
	
	public void runModule(FrameStream inGS1,FrameStream inGS2, FrameStream outDS) throws Exception {
	    try{
	      StreamHeader header = init(inGS1.getHeader());
	      StreamHeader h = init(inGS2.getHeader());
	      outDS.setHeader(header);
	      StreamFrame f1,f2,frameOut;	      
	      while ((f1 = inGS1.recvFrame()) != null && (f2 = inGS2.recvFrame()) != null) {
	        frameOut = process(f1);
	        outDS.sendFrame(frameOut);
	      }
	    }catch(Exception e){
	      e.printStackTrace();
	    }
	    outDS.close();
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
	
	public StreamFrame process(StreamFrame f1, StreamFrame f2) {
	    if (!(f1 instanceof GeometryFrame) || !(f2 instanceof GeometryFrame))
	      throw new RuntimeException("Wrong frame type");
	    GeometryFrame gf1 = (GeometryFrame) f1 ;
	    GeometryFrame gf2 = (GeometryFrame) f2 ;
	    double[][] geoActual = gf1.geometry;
	    double[][] geoEstimated = gf2.geometry;
	    GeometryMatrixModule g = new GeometryMatrixModule();
	    g.adjustAxes(geoEstimated); //for config file
	    
	    double[] x = null,y = null;
	    
	    /*double[] distanceActual;
	    for(int i = 0; i < geoActual.length; i++){
	    	for(int j = 0; j < geoActual[i].length; j++){
	    		distanceActual[i] =  Math.pow((geoActual[i][j] - geoActual[i+1][j]),2);
	    	}
	    }*/
	        
	    /*double[] x; //constructing a double[] from double[][]
	    int size = geometry[0].length;
	    for(int i = 0; i < geometry.length; i++){
	    	for(int j = 0; j < geometry[i].length; j++){
	    		x[size*i + j] += geometry[i][j];
	    	}
	    }*/
	    
	    
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

	@Override
	public StreamFrame process(StreamFrame inFrame) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
