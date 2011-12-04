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
  GeometryFrame actual;

  public void extractActual(GeometryFileStream inStream) throws Exception
  {
    inStream.getHeader(); // ignore return result -- we don't need it!
    actual = inStream.recvFrame();
    actual.adjustAxes();
  }

  public void runModule(GeometryFileStream inGS1, GeometryFileStream inGS2, FrameStream outDS) throws Exception {
    try{
      StreamHeader header = init(inGS1.getHeader());
      outDS.setHeader(header);
      extractActual(inGS2);
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
    GeometryFrame gf1 = (GeometryFrame) f1 ; // estimate
    double[][] geoEstimated = gf1.geometry;
    double[][] geoActual = actual.geometry;
    GeometryMatrixModule g = new GeometryMatrixModule();
    double[] x = {0.0},y = {0.0};
    double[] distanceVertices = new double[geoEstimated[0].length];
    for(int j = 0; j < geoEstimated[0].length; j++){
         
        distanceVertices[j] =  Math.sqrt(Math.pow((geoEstimated[0][j] - geoActual[0][j]),2) + Math.pow((geoEstimated[1][j] - geoActual[1][j]),2));
    }
    double rms = 0;
    for(int i = 0; i < geoEstimated[0].length; i++)
      rms += Math.pow(distanceVertices[i],2);
    rms = Math.sqrt(rms / geoEstimated[0].length);
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
