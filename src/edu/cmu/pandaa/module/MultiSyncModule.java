package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.MultiHeader;
import edu.cmu.pandaa.header.MultiHeader.MultiFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 1/21/12
 * Time: 8:15 AM
 */

public class MultiSyncModule implements StreamModule {
  MultiHeader syncedPairHeader;
  MultiFrame lastOut;

  @Override
  public StreamHeader init(StreamHeader inHeader) throws Exception {
    syncedPairHeader = new MultiHeader(inHeader.id + "-sync", ((MultiHeader) inHeader).getHeaders());
    lastOut = syncedPairHeader.makeFrame();
    return syncedPairHeader;
  }

  @Override
  public StreamHeader.StreamFrame process(StreamFrame inFrame) throws Exception {
    MultiFrame multi = (MultiFrame) inFrame;
    StreamFrame[] frames = multi.getFrames();
    MultiFrame out = syncedPairHeader.makeFrame();
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
