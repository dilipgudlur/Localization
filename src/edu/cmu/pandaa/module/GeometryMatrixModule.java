package edu.cmu.pandaa.module;

import edu.cmu.pandaa.stream.FrameStream;
import edu.cmu.pandaa.stream.GeometryFileStream;
import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.GeometryHeader.GeometryFrame;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import mdsj.*;

class GeometryMatrixModule implements StreamModule{
  //FrameStream inGeometryStream, outGeometryStream;
  GeometryHeader hOut;

  public GeometryMatrixModule()
  {
  }

  public void runModule(FrameStream inGeometryStream, FrameStream outGeometryStream) throws Exception {
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
    outGeometryStream.close();
    //close();
  }

  public StreamHeader init(StreamHeader inHeader) {
    if (!(inHeader instanceof GeometryHeader))
      throw new RuntimeException("Wrong header type");

    /*compute new header*/
    GeometryHeader hIn = (GeometryHeader)inHeader ;
    hOut = new GeometryHeader(hIn.deviceIds, hIn.startTime, hIn.frameTime, hIn.rows, 2);
    return hOut;
  }

  public StreamFrame process(StreamFrame inFrame) {
    if (!(inFrame instanceof GeometryFrame))
      throw new RuntimeException("Wrong frame type");
    GeometryFrame gfIn = (GeometryFrame) inFrame ;
    double[][] geom = MDSJ.classicalScaling(gfIn.geometry); // apply MDS
    geom = adjustAxes(geom);
    GeometryFrame gfOut = hOut.makeFrame(gfIn.seqNum, geom);
    return gfOut ;
  }
  
  public double[][] adjustAxes(double[][] tempGeometry)
  {
	  int rows = tempGeometry.length;
	  int cols = tempGeometry[0].length;	
	  double[][] tempGeometry2 = new double[cols][rows];
	  
	  if(tempGeometry[0][0] < 0){ //x coordinate of 1st device is -ve
		  for (int i = 0;i < cols; i++)  //invert x coordinates of all devices
			  tempGeometry[0][i] = -tempGeometry[0][i];
	  }
	  if(tempGeometry[1][0] < 0){ //x coordinate of 1st device is -ve
		  for (int i = 0;i < cols; i++) //invert x coordinates of all devices
			  tempGeometry[1][i] = -tempGeometry[1][i];
	  }
	  
	  for(int i=0;i<rows;i++){
		  for(int j=0;j<cols;j++){
			 tempGeometry2[j][i] = tempGeometry[i][j];
		  }
	  }
	  return tempGeometry2;
  }

  public static void main(String[] args) throws Exception
  {
    int arg = 0;
    String outArg = args[arg++];
    String inArg = args[arg++];
    if (args.length > arg || args.length < arg) {
      throw new IllegalArgumentException("Invalid number of arguments");
    }

    System.out.println("GeometryMatrix: " + outArg + " " + inArg);
    GeometryFileStream gOut = new GeometryFileStream(outArg, true);
    GeometryFileStream gIn = new GeometryFileStream(inArg);

    try {
      GeometryMatrixModule pgm = new GeometryMatrixModule();
      pgm.runModule(gIn,gOut);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void close() {
  }
}