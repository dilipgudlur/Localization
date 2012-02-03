package edu.cmu.pandaa.stream;

import java.nio.channels.IllegalBlockingModeException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import edu.cmu.pandaa.header.MultiHeader;
import edu.cmu.pandaa.header.MultiHeader.MultiFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/14/11
 * Time: 9:33 PM
 */

public class MultiFrameStream implements FrameStream {
  public  final String id;
  private MultiHeader outHeader;
  private Map<StreamHeader, LinkedList<StreamFrame>> frames = new HashMap<StreamHeader, LinkedList<StreamFrame>>();
  private boolean isOpen = true;
  public boolean noblock = false;
  private long lastTime;

  public MultiFrameStream(String id) throws Exception {
    this.id = id;
  }

  // set/write the header
  @Override
  public synchronized void setHeader(StreamHeader h) throws Exception {
    if (outHeader == null) {
      if (isOpen) {
        outHeader = new MultiHeader(id, h);
        notifyAll();
      }
    } else {
      outHeader.addHeader(h);
    }
  }

  // send a frame of data
  @Override
  public synchronized void sendFrame(StreamFrame m) throws Exception {
    if (m == null)
      return;
    StreamHeader h = m.getHeader();
    if (!frames.containsKey(h)) {
      frames.put(h, new LinkedList<StreamFrame>());
    }
    frames.get(h).addLast(m);
    notifyAll();
  }

  @Override
  public synchronized MultiHeader getHeader() throws Exception {
    while (outHeader == null && isOpen) {
      wait();
    }
    return outHeader;
  }

  @Override
  public synchronized MultiFrame recvFrame() throws Exception {
    long minTime;
    boolean incomplete;

    do {
      incomplete = false;
      minTime = -1;
      for (StreamHeader in : frames.keySet()) {
        StreamFrame f = frames.get(in).peekFirst();
        if (f == null) {
          incomplete = true;
        } else {
          long time = f.getStartTime();
          if ((minTime < 0)||(time < minTime)) {
            minTime = time;
          } else if ((time - minTime) % in.frameTime != 0) {
            throw new RuntimeException("Timebases are not aligned");
          }
        }
      }
      if (incomplete) {
        if (!isOpen) {
          return null;
        }
        if (noblock) {
          throw new IllegalBlockingModeException();
        }
        wait();
      }
    } while (incomplete);

    int seqNum = (int) ((minTime - outHeader.startTime)/outHeader.frameTime);
    MultiFrame frame = outHeader.makeFrame(seqNum);
    for (StreamHeader in : frames.keySet()) {
      StreamFrame f = frames.get(in).peekFirst();
      if (f != null) {
        long time = f.getStartTime();
        if (time == minTime) {
          frame.setFrame(frames.get(in).pollFirst());
        }
      }
    }

    notifyAll();
    return frame;
  }

  @Override
  public synchronized void close() {
    isOpen = false;
    notifyAll();
  }

  public static void main(String[] args) throws Exception {
    StreamHeader[] streams = new StreamHeader[10];
    MultiFrameStream dm = new MultiFrameStream("test");

    for (int i = 0; i < streams.length; i++) {
      streams[i] = new StreamHeader("id" + i, System.currentTimeMillis(), 100);
      dm.setHeader(streams[i]);
      Thread.sleep(1); // make start times different, simulating reality
    }

    MultiHeader mh = dm.getHeader();
    System.out.println(mh.toString());

    for (int j = 0; j < 10; j++) {
      for (int i = 0; i < streams.length; i++) {
        dm.sendFrame(streams[i].makeFrame());
      }

      MultiFrame sf = dm.recvFrame();
      System.out.println(sf.toString());
    }

    dm.close();
  }
}
