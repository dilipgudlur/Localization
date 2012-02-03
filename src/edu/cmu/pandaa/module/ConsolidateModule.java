package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.DistanceHeader;
import edu.cmu.pandaa.header.DistanceHeader.DistanceFrame;
import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.GeometryHeader.GeometryFrame;
import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.ImpulseHeader.ImpulseFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.DistanceFileStream;
import edu.cmu.pandaa.stream.FileStream;
import edu.cmu.pandaa.stream.GeometryFileStream;
import edu.cmu.pandaa.stream.ImpulseFileStream;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/21/11
 * Time: 8:56 AM
 */

public class ConsolidateModule implements StreamModule {
  final int combine, rolling, weight, loops;
  final StreamFrame[] frames;
  StreamHeader header;
  Factory factory;
  int frameCnt;
  FileStream out, in;
  final char consolidateType;

  public ConsolidateModule(char type, int combine, int rolling, int weight, int loops) {
    this.combine = combine;
    this.rolling = rolling;
    this.weight = weight;
    this.loops = loops;
    this.consolidateType = type;
    if (combine < 1 || rolling < 1) {
      throw new IllegalArgumentException("Arguments both need to be >0");
    }
    frames = new StreamFrame[combine];
  }

  public StreamHeader init(StreamHeader inHeader) {
    String nid = inHeader.id + "." + combine + "-" + rolling;
    int frameTime = inHeader.frameTime * rolling;
    long startTime = inHeader.startTime;

    switch (consolidateType) {
      case 'i':
        header = new ImpulseHeader(nid, startTime, frameTime * rolling, rolling);
        factory = new impulseFactory();
        break;
      case 'd':
        DistanceHeader dHeader = (DistanceHeader) inHeader;
        header = new DistanceHeader(nid, startTime, frameTime * rolling, dHeader.getDeviceIds(), rolling);
        factory = new distanceFactory();
        break;
      case 'm':
        GeometryHeader gHeader = (GeometryHeader) inHeader;
        header = new GeometryHeader(nid, startTime, frameTime * rolling, gHeader.rows, gHeader.cols);
        factory = new geometryFactory((GeometryHeader) header);
        break;
      default:
        throw new IllegalArgumentException("Don't know how to consolidate " + consolidateType);
    }
    return header;
  }

  public synchronized StreamFrame process(StreamFrame inFrame) {
    if (inFrame == null)
      return null;

    frames[frameCnt % combine] = inFrame;
    frameCnt++;
    return (frameCnt >= combine) && ((frameCnt-combine) % rolling == 0) ? factory.makeFrame() : null;
  }

  private StreamFrame getFrame(int offset) {
    return frames[(frameCnt + offset)%combine];
  }

  public void close() {
    if (out != null) {
      out.close();
      out = null;
    }
    if (in != null) {
      in.close();
      in = null;
    }
  }

  interface Factory {
    StreamFrame makeFrame();
  }

  class distanceFactory implements Factory {
    public StreamFrame makeFrame() {
      double distanceSum = 0;
      double magnitudeSum = 0;
      for (int i = 0; i < combine; i++) {
        DistanceFrame frame = (DistanceFrame) getFrame(i);
        if (frame == null)
          break;
        for (int j = 0;j < frame.peakDeltas.length;j++) {
          distanceSum += frame.peakDeltas[j] * frame.peakMagnitudes[j];
          magnitudeSum += frame.peakMagnitudes[j];
        }
      }

      double deltas[] = { };
      double mags[] = { };
      if (magnitudeSum > 0) {
        mags = new double[] { magnitudeSum };
        deltas = new double[] { distanceSum / magnitudeSum };
      }
      return ((DistanceHeader) header).makeFrame(deltas, mags);
    }
  }

  class geometryFactory implements Factory {
    double[][] geometry;
    int rows, cols;

    public geometryFactory(GeometryHeader gHeader) {
      rows = gHeader.rows;
      cols = gHeader.cols;
      geometry = new double[rows][cols];
      for (int i = 0;i < rows; i++)
        for (int j = 0; j < cols; j++) {
          geometry[i][j] = Double.NaN;
        }
    }

    public StreamFrame makeFrame() {
      for (int f = 0; f < combine; f++) {
        GeometryFrame frame = (GeometryFrame) getFrame(f);
        if (frame == null)
          break;

        double[][] input = frame.geometry;
        for (int i = 0;i < rows; i++)
          for (int j = 0; j < cols; j++) {
            if (Double.isNaN(geometry[i][j]))
              geometry[i][j] = input[i][j];
            else if (!Double.isNaN(input[i][j]))
              geometry[i][j] = (geometry[i][j]*weight + input[i][j])/(weight + 1);
            else
              geometry[i][j] = geometry[i][j]; // NOP placeholder
          }

      }
      return ((GeometryHeader) header).makeFrame(geometry);
    }
  }

  class impulseFactory implements Factory {
    public StreamFrame makeFrame() {
      int size = 0;
      for (int i = 0; i < combine; i++) {
        ImpulseFrame frame = (ImpulseFrame) getFrame(i);
        size += frame != null ? frame.peakMagnitudes.length : 0;
      }
      int frameTime = frames[0].getHeader().frameTime;
      int[] offs = new int[size];
      short[] mags = new short[size];
      int pos = 0, shift = 0;
      for (int i = 0; i < combine; i++) {
        ImpulseFrame frame = (ImpulseFrame) getFrame(i);
        if (frame == null)
          break;
        int length = frame.peakMagnitudes.length;

        System.arraycopy(frame.peakOffsets, 0, offs, pos, length);
        System.arraycopy(frame.peakMagnitudes, 0, mags, pos, length);

        for (int j = 0; j < length; j++) {
          offs[pos + j] += shift;
        }
        pos += length;
        shift += frameTime;
      }

      return ((ImpulseHeader) header).makeFrame(offs, mags);
    }
  }

  public void go(String outName, String inName) throws Exception {
    StreamFrame frame;
    for (int i = 0;i < loops; i++) {
      boolean first = i == 0;
      open_streams(outName, inName, first);
      StreamHeader inH = in.getHeader();
      if (first) {
        out.setHeader(init(inH));
      }
      while ((frame = in.recvFrame()) != null) {
        out.sendFrame(process(frame));
      }
    }
    close();
  }

  @SuppressWarnings("unchecked")
  public void open_streams(String outName, String inName, boolean first) throws Exception {
    Class model;
    switch(consolidateType) {
      case 'd':
        model = DistanceFileStream.class;
        break;
      case 'i':
        model = ImpulseFileStream.class;
        break;
      case 'm':
        model = GeometryFileStream.class;
        break;
      default:
        throw new IllegalArgumentException("Consolidate type not recognized: " + consolidateType);
    }
    if (first) {
      out = (FileStream) (model.getDeclaredConstructor(String.class, Boolean.TYPE).newInstance(outName, true));
    } else {
      in.close();
    }
    in = (FileStream) (model.getDeclaredConstructor(String.class).newInstance(inName));
  }

  public static void main(String[] args) throws Exception {
    int arg = 0;
    String[] opts = args[arg++].split("-");
    String outName = args[arg++];
    String inName = args[arg++];
    if (args.length > arg) {
      throw new IllegalArgumentException("Too many input arguments");
    }
    System.out.println("Consolidate " + args[0] + ": " + outName + " " + inName);
    int nopts = opts.length;
    int opt = 0;
    char type = opts[opt++].charAt(0);
    int combine = nopts > opt ? Integer.parseInt(opts[opt++]) : 1;
    int rolling = nopts > opt ? Integer.parseInt(opts[opt++]) : 1;
    int average = nopts > opt ? Integer.parseInt(opts[opt++]) : 1;
    int loops = nopts > opt ? Integer.parseInt(opts[opt++]) : 1;
    if (nopts > 5)
      throw new IllegalArgumentException("Too many consolidate opts");
    ConsolidateModule consolidate = new ConsolidateModule(type, combine, rolling, average, loops);
    consolidate.go(outName, inName);
  }
}
