package edu.cmu.pandaa.header;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/14/11
 * Time: 9:26 PM
 */
public class MultiHeader extends StreamHeader {
  private final Map<StreamHeader, Integer> hmap = new HashMap<StreamHeader, Integer>();
  final StreamHeader first;

  public MultiHeader(String id, StreamHeader header) {
    super(id, header.startTime, header.frameTime);
    first = header;
    addHeader(header);
  }

  public void addHeader(StreamHeader header) {
    if (header.getClass() != first.getClass()) {
      throw new IllegalArgumentException("StreamHeaders should match for multi-header");
    }
    hmap.put(header, hmap.size());
  }

  public boolean contains(StreamHeader h) {
    return hmap.containsKey(h);
  }

  public StreamHeader[] getHeaders() {
    return hmap.keySet().toArray(new StreamHeader[0]);
  }

  public StreamHeader getOne() {
    return first;
  }

  public class MultiFrame extends StreamFrame {
    final StreamFrame[] frames;

    public MultiFrame() {
      frames = new StreamFrame[hmap.size()];
    }

    public void setFrame(StreamFrame f) {
      int pos = hmap.get(f.getHeader());
      frames[pos] = f;
    }

    public Set<StreamHeader> getHeaders() {
      return hmap.keySet();
    }

    public StreamFrame[] getFrames() {
      return frames;
    }
  }

  @Override
  public MultiFrame makeFrame() {
    return new MultiFrame();
  }
}
