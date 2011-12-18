package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.DistanceHeader;
import edu.cmu.pandaa.header.DistanceHeader.DistanceFrame;
import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.GeometryHeader.GeometryFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.DistanceFileStream;
import edu.cmu.pandaa.stream.FrameStream;
import edu.cmu.pandaa.stream.GeometryFileStream;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 11/25/11
 * Time: 8:25 AM
 */

public class DistanceFilter implements StreamModule {
  double average = 0, magnitude = 1, weight;
  DistanceHeader h, inHeader;
  public static final double speedOfSound = 340.29; // m/s at sea level
  FrameStream posStream;
  GeometryFrame posFrame;
  int numDevices = 4;  // default unless we know otherwise
  int d1index = -1, d2index = -1;

  public DistanceFilter(double weight) {
    this.weight = weight;
  }

  private double distanceBetween(int i, int j) {
    double dx, dy;

    if (posFrame == null) {
      double a1 = Math.PI*2*i/numDevices;
      double a2 = Math.PI*2*j/numDevices;
      dx = Math.sin(a1) - Math.sin(a2);
      dy = Math.cos(a1) - Math.cos(a2);
    } else {
      dx = posFrame.geometry[0][i] - posFrame.geometry[0][j];
      dy = posFrame.geometry[1][i] - posFrame.geometry[1][j];
    }

    return Math.sqrt(dx*dx + dy*dy);
  }

  private double distanceAdjustment(int i, int j) {
    double exp = 0;
    double d1 = distanceBetween(i, j);
    for (int k = 0; k < numDevices; k++) {
      double d2 = distanceBetween(i, k);
      double d3 = distanceBetween(k, j);
      exp += Math.abs(d2 - d3);
    }
    return d1*numDevices/exp;
  }

  private double getScale() throws Exception {
    double scale;
    if (posStream == null)
      scale = getScaleGuess();
    else {
      posFrame = (GeometryFrame) posStream.recvFrame();
      scale = distanceAdjustment(d1index, d2index);
      if ((scale < 1)||(scale > 5)|| Double.isNaN(scale) || Double.isInfinite(scale)) {
        posFrame = null;
        scale = getScaleGuess();
      }
    }

    scale *= speedOfSound / 1000.0; // convert from dt (us) to distance (mm)
    return scale;
  }

  private double getScaleGuess() {
    double scale = 0;

    for (int i = 0; i < numDevices; i++)
      for (int j = 0; j < numDevices; j++)
        if (i != j)
          scale += distanceAdjustment(i, j);  // TODO: use accurate gHeader
    scale /= numDevices*(numDevices - 1);

    return scale;
  }

  public DistanceHeader init(StreamHeader inHeader) throws Exception {
    h = new DistanceHeader((DistanceHeader) inHeader);
    this.inHeader = h;

    return h;
  }

  public void setPositionStream(FrameStream posStream) throws Exception {
    this.posStream = posStream;

    GeometryHeader gh = (GeometryHeader) posStream.getHeader();
    numDevices = gh.rows;

    String d1id = inHeader.getDeviceIds()[0];
    String d2id = inHeader.getDeviceIds()[1];
    String[] did = gh.getDeviceIds();
    for (int i = 0;i < numDevices;i++) {
      if (d1id.equals(did[i]))
        d1index = i;
      if (d2id.equals(did[i]))
        d2index = i;
    }

    if ((d1index < 0)||(d2index < 0)||(d1index == d2index))
      throw new IllegalArgumentException("Could not find matching deviceIds");

    if (gh.frameTime != inHeader.frameTime)
      throw new IllegalArgumentException("Frame time mismatch");
  }

  public DistanceFrame process(StreamFrame inFrame) throws Exception {
    DistanceFrame din = (DistanceFrame) inFrame;
    double scale = getScale();
    for (int i = 0;i < din.peakDeltas.length; i++) {
      double peak_magnitude = din.peakMagnitudes[i];
      double peak_tdoa = din.peakDeltas[i];

      double distance = Math.abs(peak_tdoa) * scale;

      average = (average * magnitude * weight + distance * peak_magnitude)/(weight * magnitude + peak_magnitude);
      magnitude = (magnitude * weight + peak_magnitude)/(weight + 1);
    }

    double[] deltas = { average };
    double[] magnitudes = { magnitude };

    return h.makeFrame(deltas, magnitudes);
  }

  public void close() {

  }

  public static void main(String[] args) throws Exception
  {
    int arg = 0;
    String distArg = null;
    String wString = args[arg++];
    String outArg = args[arg++];
    String inArg = args[arg++];
    if (args.length != arg)
      distArg = args[arg++];
    if (args.length != arg) {
      throw new IllegalArgumentException("Invalid number of arguments");
    }

    System.out.println("GeometryMatrix: " + outArg + " " + inArg);
    DistanceFileStream in = new DistanceFileStream(inArg);
    DistanceFileStream out = new DistanceFileStream(outArg, true);
    GeometryFileStream pos = distArg == null ? null : new GeometryFileStream(distArg);

    try {
      DistanceFilter df = new DistanceFilter(Double.parseDouble(wString));
      out.setHeader(df.init(in.getHeader()));
      if (pos != null)
        df.setPositionStream(pos);
      StreamFrame frameIn;
      while ((frameIn = in.recvFrame()) != null) {
        out.sendFrame(df.process(frameIn));
      }
      in.close();
      out.close();
      df.close();
    }catch(Exception e){
      e.printStackTrace();
    }
  }
}
