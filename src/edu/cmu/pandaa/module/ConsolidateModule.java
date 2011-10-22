package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.ImpulseHeader.ImpulseFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.ImpulseFileStream;

import java.io.PipedOutputStream;
import java.lang.reflect.Array;

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

  public ConsolidateModule(int combine, int rolling) {
    this.combine = combine;
    this.rolling = rolling;
    if (combine < 1 || rolling < 1) {
      throw new IllegalArgumentException("Arguments both need to be >0");
    }
    frames = new StreamFrame[combine];
  }

  public StreamHeader init(StreamHeader inHeader) {
    if (inHeader instanceof ImpulseHeader) {
      header = new ImpulseHeader(inHeader.id + ".c" + combine + "-" + rolling, inHeader.startTime, inHeader.frameTime * rolling);
      ((ImpulseHeader) header).rollingWindow = rolling; // TODO: Should make part of the constructor
      factory = new impulseFactory();
    } else {
      throw new IllegalArgumentException("Don't know how to consolidate " + inHeader.getClass().getSimpleName());
    }
    return header;
  }

  public synchronized StreamFrame process(StreamFrame inFrame) {
    frames[frameCnt % combine] = inFrame;
    frameCnt++;
    return (frameCnt >= combine) && ((frameCnt-combine) % rolling == 0) ? factory.makeFrame() : null;
  }

  private StreamFrame getFrame(int offset) {
    return frames[(frameCnt + offset)%combine];
  }

  public void close() {

  }

  interface Factory {
    StreamFrame makeFrame();
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

  public static void main(String[] args) throws Exception {
    int arg = 0;
    String[] opts = args[arg++].split("-");
    ImpulseFileStream in = new ImpulseFileStream(args[arg++]);
    ImpulseFileStream out = new ImpulseFileStream(args[arg++], true);
    if (args.length > arg) {
      throw new IllegalArgumentException("Too many input arguments");
    }
    int combine = Integer.parseInt(opts[0]);
    int rolling = Integer.parseInt(opts[1]);
    StreamModule consolidate = new ConsolidateModule(combine, rolling);

    out.setHeader(consolidate.init(in.getHeader()));
    StreamFrame frame;
    while ((frame = in.recvFrame()) != null) {
      out.sendFrame(consolidate.process(frame));
    }
    consolidate.close();
    out.close();
  }
}
