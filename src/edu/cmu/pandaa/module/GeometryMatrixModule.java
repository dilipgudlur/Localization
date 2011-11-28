package edu.cmu.pandaa.module;

import edu.cmu.pandaa.stream.FrameStream;
import edu.cmu.pandaa.stream.GeometryFileStream;
import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.GeometryHeader.GeometryFrame;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import mdsj.*;

class GeometryMatrixModule implements StreamModule{
  GeometryHeader hOut;
  double[] prevX;

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
    adjustAxes(geom);
    GeometryFrame gfOut = hOut.makeFrame(gfIn.seqNum, geom);
    return gfOut ;
  }

  public void adjustAxes(double[][] geom)
  {
    if (geom.length != 2)
      throw new IllegalArgumentException("should be 2 dimensions!");
    int cols = geom[0].length;

    // check for valid data
    for (int i = 0;i < 2; i++)
      for (int j = 0;j < cols; j++)
        if (Double.isNaN(geom[i][j]))
          return;

    // translate coordinates to the centroid
    for (int i = 0;i < 2; i++) {
      double sum = 0;
      for (int j = 0;j < cols; j++)
        sum += geom[i][j];
      sum = sum / cols;
      for (int j = 0;j < cols; j++)
        geom[i][j] -= sum;
    }

    // rotate device 0 so it's at the bottom center
    double ang = Math.atan2(-geom[0][0], -geom[1][0]);
    double sin = Math.sin(ang);
    double cos = Math.cos(ang);
    for (int j = 0;j < cols; j++) {
      double nx = geom[0][j]*cos - geom[1][j]*sin;
      double ny = geom[0][j]*sin + geom[1][j]*cos;
      geom[0][j] = nx;
      geom[1][j] = ny;
    }

    // first time through, we don't really know left from right
    // arbitrarily choose it so that device[1] is x>0
    if (prevX == null) {
      prevX = new double[cols];
      double mult = geom[0][1] > 0 ? 1 : -1;
      for (int j = 0;j < cols; j++) {
        prevX[j] = geom[0][j]*mult;
      }
    }

    // go through and see if we should flip or not flip
    double flip = 0, noflip = 0;
    for (int j = 0;j < cols; j++) {
      double dx = geom[0][j]-prevX[j];
      noflip += dx*dx;
      dx = geom[0][j]+prevX[j];
      flip += dx*dx;
    }

    // flip the x axis if it minimizes difference
    if (flip < noflip) {
      for (int j = 0;j < cols; j++) {
        geom[0][j] = -geom[0][j];
      }
    }

    // save the X values for flipping later frames
    for (int j = 0;j < cols; j++) {
      prevX[j] = geom[0][j];
    }
  }

  public void close() {
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
    GeometryFileStream gIn = new GeometryFileStream(inArg);
    GeometryFileStream gOut = new GeometryFileStream(outArg, true, true);

    try {
      GeometryMatrixModule pgm = new GeometryMatrixModule();
      pgm.runModule(gIn,gOut);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}