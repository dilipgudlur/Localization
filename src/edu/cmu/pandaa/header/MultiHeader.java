package edu.cmu.pandaa.header;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/14/11
 * Time: 9:26 PM
 */
public class MultiHeader extends StreamHeader {
  private final Map<StreamHeader, Integer> hmap = new HashMap<StreamHeader, Integer>();
  public final StreamHeader first;

  public MultiHeader(String id, StreamHeader header) {
    super(id, header.startTime, header.frameTime);
    first = header;
    addHeader(header);
  }

  public MultiHeader(String id, StreamHeader[] headers) {
    super(id, headers[0].startTime, headers[0].frameTime);
    first = headers[0];
    for (int i = 0;i < headers.length;i++) {
      addHeader(headers[i]);
    }
  }

  public synchronized void addHeader(StreamHeader header) {
    if (header.getClass() != first.getClass()) {
      throw new IllegalArgumentException("StreamHeaders should match for multi-header");
    }
    hmap.put(header, hmap.size());
    notifyAll();
  }

  public synchronized void waitForHeaders(int num) throws InterruptedException {
    while (hmap.size() != num) {
      wait();
    }
  }

  protected String getMetaId() {
    StreamHeader[] headers = getHeaders();
    Set<String> set = new HashSet<String>();
    for (int i = 0;i < headers.length;i++) {
      String[] ids = headers[i].getIds();
      for (int j = 0;j < ids.length;j++) {
        set.add(ids[j]);
      }
    }
    return makeId(id, set.toArray(new String[0]));
  }

  public boolean contains(StreamHeader h) {
    return hmap.containsKey(h);
  }

  public StreamHeader[] getHeaders(StreamHeader[] target) {
    for (StreamHeader key : hmap.keySet())
      target[hmap.get(key)] = key;
    return target;
  }

  public StreamHeader[] getHeaders() {
    StreamHeader[] sh = new StreamHeader[hmap.size()];
    return getHeaders(sh);
  }

  public int size() {
    return hmap.size();
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

    public StreamFrame getFrame(StreamHeader h) {
      return frames[hmap.get(h)];
    }

    public StreamFrame getFrame(int index) {
      return frames[index];
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
