package edu.cmu.pandaa.depricated;

import edu.cmu.pandaa.header.MultiHeader;
import edu.cmu.pandaa.header.MultiHeader.MultiFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.module.StreamModule;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 1/21/12
 * Time: 8:15 AM
 */

public class MultiSyncModule implements StreamModule {
  private MultiHeader syncedHeader;
  private MultiFrame lastOut;
  private long lastTime;

  @Override
  public StreamHeader init(StreamHeader inHeader) throws Exception {
    syncedHeader = new MultiHeader(inHeader.id + "-sync", ((MultiHeader) inHeader).getHeaders());
    lastOut = syncedHeader.makeFrame();
    return syncedHeader;
  }

  @Override
  public StreamHeader.StreamFrame process(StreamFrame inFrame) throws Exception {
    MultiFrame multi = (MultiFrame) inFrame;
    StreamFrame[] frames = multi.getFrames();
    MultiFrame out = syncedHeader.makeFrame();
    boolean incomplete = false;

    for (int i = 0; i < frames.length; i++) {
      StreamFrame frame = frames[i];
      if (frame == null) {
        frame = lastOut.getFrame(i);
      }

      if (frame == null) {
        incomplete = true;
      } else {
        out.setFrame(frame);
      }
    }

    lastOut = out;

    return incomplete ? null : out;
  }

  @Override
  public void close() {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
