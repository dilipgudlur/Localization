package edu.cmu.pandaa.framework;

import edu.cmu.pandaa.header.DistanceHeader;
import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.MultiHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.module.DummyModule;
import edu.cmu.pandaa.module.StreamModule;
import edu.cmu.pandaa.stream.FileStream;
import edu.cmu.pandaa.stream.GeometryFileStream;

import javax.xml.crypto.dsig.TransformService;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/18/11
 * Time: 4:12 PM
 */

public class MergePipeline implements StreamModule {
  /* fields onloy used for constructing the DummyModule -- remove when we have real code */
  final long now = System.currentTimeMillis();
  final int frameTime = 100;
  String[] dummyIds = { "a", "b", "c" };

  /* First step is to take in a multiFrame consisting of impulseframes and turn it into time differences */
  StreamModule geometry = new DummyModule(new GeometryHeader(dummyIds, now, frameTime, 3, 3));

  /* Then we consolidate bunches of impulse frames into larger consolidated frames for processing */
  StreamModule smooth = new DummyModule(new GeometryHeader(dummyIds, now, frameTime, 3, 3));

  FileStream trace;

  @Override
  public StreamHeader init(StreamHeader inHeader) throws Exception {
    MultiHeader multiHeader = (MultiHeader) inHeader;
    if (!(multiHeader.getOne() instanceof DistanceHeader)) {
       throw new IllegalArgumentException("Merge pipe multiheader should contain ImpulseHeaders");
    }
    StreamHeader header = geometry.init(inHeader);
    header = smooth.init(header);
    if (!(header instanceof GeometryHeader)) {
      throw new IllegalArgumentException("Output should be GeometryHeader");
    }

    trace = new GeometryFileStream(inHeader.id + ".txt", true, true);
    trace.setHeader(header);

    return header;
  }

  @Override
  public StreamFrame process(StreamFrame inFrame) throws Exception {
    if (inFrame == null) {
      return null;
    }
    StreamFrame frame = geometry.process(inFrame);
    frame = smooth.process(frame);
    trace.sendFrame(frame);

    return frame;
  }

  @Override
  public void close() {
    trace.close();
  }}
