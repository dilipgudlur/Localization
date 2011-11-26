package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.ImpulseHeader.ImpulseFrame;

import java.util.LinkedList;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 11/25/11
 * Time: 8:25 AM
 */

public class SignficanceFilter implements StreamModule {
  int window_ms;
  int save_frames;
  LinkedList<ImpulseFrame> frames = new LinkedList<ImpulseFrame>();

  public SignficanceFilter(int window_ms) {
    this.window_ms = window_ms;
  }

  public ImpulseHeader init(StreamHeader inHeader) {
    save_frames = window_ms / (inHeader.frameTime + window_ms - 1);
    return (ImpulseHeader) inHeader;
  }

  public ImpulseFrame process(StreamFrame inFrame) {
    ImpulseFrame iFrame = (ImpulseFrame) inFrame;

    if (inFrame == null) {
      return frames.removeFirst();
    }

    if (frames.size() < save_frames) {
      frames.addLast(iFrame);
    }

    return (ImpulseFrame) inFrame;
  }

  public void close() {

  }
}
