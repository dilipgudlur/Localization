package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.DistanceHeader;
import edu.cmu.pandaa.header.DistanceHeader.DistanceFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.DistanceFileStream;

import java.util.LinkedList;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 11/25/11
 * Time: 8:25 AM
 */

public class DistanceFilter implements StreamModule {
  double average = 0, magnitude = 1, weight;
  DistanceHeader h;
  public static final double speedOfSound = 340.29; // m/s at sea level
  final int numDevices = 3;
  double scale;

  public DistanceFilter(double weight) {
    this.weight = weight;
  }

  private double distanceBetween(int i, int j) {
    double a1 = Math.PI*2*i/numDevices;
    double a2 = Math.PI*2*j/numDevices;
    double dx = Math.sin(a1) - Math.sin(a2);
    double dy = Math.cos(a1) - Math.cos(a2);
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

  public DistanceHeader init(StreamHeader inHeader) {
    h = new DistanceHeader((DistanceHeader) inHeader);

    scale = 0;
    for (int i = 0; i < numDevices; i++)
      for (int j = 0; j < numDevices; j++)
        if (i != j)
          scale += distanceAdjustment(i, j);  // TODO: use accurate geometry
    scale /= numDevices*(numDevices - 1);
    scale *= speedOfSound / 1000.0; // convert from dt (us) to distance (mm)

    return h;
  }

  public DistanceFrame process(StreamFrame inFrame) {
    DistanceFrame din = (DistanceFrame) inFrame;

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
    String wString = args[arg++];
    String outArg = args[arg++];
    String inArg = args[arg++];
    if (args.length != arg) {
      throw new IllegalArgumentException("Invalid number of arguments");
    }

    System.out.println("GeometryMatrix: " + outArg + " " + inArg);
    DistanceFileStream in = new DistanceFileStream(inArg);
    DistanceFileStream out = new DistanceFileStream(outArg, true);

    try {
      DistanceFilter df = new DistanceFilter(Double.parseDouble(wString));
      out.setHeader(df.init(in.getHeader()));
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
