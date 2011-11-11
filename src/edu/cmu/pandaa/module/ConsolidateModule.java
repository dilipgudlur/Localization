package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.DistanceHeader;
import edu.cmu.pandaa.header.DistanceHeader.DistanceFrame;
import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.ImpulseHeader.ImpulseFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.DistanceFileStream;
import edu.cmu.pandaa.stream.FileStream;
import edu.cmu.pandaa.stream.ImpulseFileStream;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/21/11
 * Time: 8:56 AM
 */

public class ConsolidateModule implements StreamModule {
  final int combine, rolling;
  final StreamFrame[] frames;
  StreamHeader header;
  Factory factory;
  int frameCnt;
  FileStream out, in;

  public ConsolidateModule(int combine, int rolling) {
    this.combine = combine;
    this.rolling = rolling;
    if (combine < 1 || rolling < 1) {
      throw new IllegalArgumentException("Arguments both need to be >0");
    }
    frames = new StreamFrame[combine];
  }

  public StreamHeader init(StreamHeader inHeader) {
    String nid = inHeader.id + "." + combine + "-" + rolling;
    int frameTime = inHeader.frameTime * rolling;
    long startTime = inHeader.startTime;
    if (inHeader instanceof ImpulseHeader) {
      header = new ImpulseHeader(nid, startTime, frameTime * rolling, rolling);
      factory = new impulseFactory();
    } else if (inHeader instanceof DistanceHeader) {
      DistanceHeader dHeader = (DistanceHeader) inHeader;
      header = new DistanceHeader(nid, startTime, frameTime * rolling, rolling, dHeader.deviceIds);
      factory = new distanceFactory();
    } else {
      throw new IllegalArgumentException("Don't know how to consolidate " + inHeader.getClass().getSimpleName());
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
    out.close();
    in.close();
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

      double deltas[] = { distanceSum / magnitudeSum };
      double mags[] = { magnitudeSum };
      return ((DistanceHeader) header).makeFrame(deltas, mags);
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
        // because of circular buffer, adding 1 to index gives us the oldest frame
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

  public void go() throws Exception {
    out.setHeader(init(in.getHeader()));
    StreamFrame frame;
    while ((frame = in.recvFrame()) != null) {
      out.sendFrame(process(frame));
    }
    close();
  }

  public void open_streams(String type, String outName, String inName) throws Exception {
    switch(type.charAt(0)) {
      case 'd':
        out = new DistanceFileStream(outName, true);
        in = new DistanceFileStream(inName);
        break;
      case 'i':
        out = new ImpulseFileStream(outName, true);
        in = new ImpulseFileStream(inName);
        break;
      default:
        throw new IllegalArgumentException("Consolidate type not recognized: " + type);
    }
  }

  public static void main(String[] args) throws Exception {
    int arg = 0;
    String type = args[arg++];
    String[] opts = args[arg++].split("-");
    String outName = args[arg++];
    String inName = args[arg++];
    if (args.length > arg) {
      throw new IllegalArgumentException("Too many input arguments");
    }
    System.out.println("Consolidate " + type + ": " + inName + " to " + outName);
    int combine = Integer.parseInt(opts[0]);
    int rolling = Integer.parseInt(opts[1]);
    ConsolidateModule consolidate = new ConsolidateModule(combine, rolling);
    consolidate.open_streams(type, outName, inName);
    consolidate.go();
  }
}
