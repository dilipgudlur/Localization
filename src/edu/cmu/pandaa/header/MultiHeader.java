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

  public MultiHeader(String id, StreamHeader header) {
    super(id, header.startTime, header.frameTime);
    addHeader(header);
  }

  public void addHeader(StreamHeader header) {
     hmap.put(header, hmap.size());
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
  }

  @Override
public MultiFrame makeFrame() {
    return new MultiFrame();
  }

  public boolean contains(StreamHeader h) {
    return hmap.containsKey(h);
  }
}
