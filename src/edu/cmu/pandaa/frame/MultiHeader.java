package edu.cmu.pandaa.frame;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/14/11
 * Time: 9:26 PM
 */
public class MultiHeader extends StreamHeader {
  final StreamHeader[] headers;
  private final Map<StreamHeader, Integer> hmap = new HashMap<StreamHeader, Integer>();

  public MultiHeader(StreamHeader[] headers) {
    super(makeId(headers), headers[0].startTime, headers[0].frameTime);
    this.headers = headers;
    for (int i = 0; i < headers.length; i++) {
      hmap.put(headers[i], i);
    }
  }

  private static String makeId(StreamHeader[] headers) {
    StringBuilder ids = new StringBuilder();
    int frameTime = headers[0].frameTime;

    for (StreamHeader h : headers) {
      ids.append(',');
      ids.append(h.id);
      if (h.frameTime !=frameTime) {
        throw new RuntimeException("frameTimes do not match");
      }
    }
    return ids.substring(1); // skip leading comma
  }

  public class MultiFrame extends StreamFrame {
    final StreamFrame[] frames;

    public MultiFrame() {
      frames = new StreamFrame[headers.length];
    }
  }

  public MultiFrame makeFrame() {
    return new MultiFrame();
  }

  public int position(StreamHeader h) {
    try {
      return hmap.get(h);
    } catch (Exception e) {
      return -1;
    }
  }
}
