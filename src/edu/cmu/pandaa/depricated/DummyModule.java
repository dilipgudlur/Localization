package edu.cmu.pandaa.depricated;


import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.module.StreamModule;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/20/11
 * Time: 7:05 PM
 */

public class DummyModule implements StreamModule {
  final StreamHeader header;

  public DummyModule() {
    this.header = null;
  }

  public DummyModule(StreamHeader header) {
    this.header = header;
  }

  public StreamHeader init(StreamHeader inHeader) {
    return header == null ? inHeader : header;
  }

  public StreamFrame process(StreamFrame inFrame) {
    return header == null ? inFrame : header.makeFrame();
  }

  public void close() {
  }
}
