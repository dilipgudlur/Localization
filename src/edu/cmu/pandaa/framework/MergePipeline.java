package edu.cmu.pandaa.framework;

import edu.cmu.pandaa.header.DistanceHeader;
import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.MultiHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.module.DistanceMatrixModule;
import edu.cmu.pandaa.module.GeometryMatrixModule;
import edu.cmu.pandaa.module.StreamModule;
import edu.cmu.pandaa.stream.FileStream;
import edu.cmu.pandaa.stream.GeometryFileStream;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/18/11
 * Time: 4:12 PM
 */

public class MergePipeline implements StreamModule {

  StreamModule matrix = new DistanceMatrixModule();
  StreamModule geometry = new GeometryMatrixModule();

  FileStream trace;

  @Override
  public StreamHeader init(StreamHeader inHeader) throws Exception {
    MultiHeader multiHeader = (MultiHeader) inHeader;
    if (!(multiHeader.getOne() instanceof DistanceHeader)) {
       throw new IllegalArgumentException("Merge pipe multiheader should contain ImpulseHeaders");
    }

    StreamHeader header = inHeader;
    header = matrix.init(header);
    header = geometry.init(header);

    if (!(header instanceof GeometryHeader)) {
      throw new IllegalArgumentException("Output should be GeometryHeader");
    }

    trace = new GeometryFileStream(App.TRACE_DIR + inHeader.id + ".txt", true, false);
    trace.setHeader(header);

    return header;
  }

  @Override
  public StreamFrame process(StreamFrame inFrame) throws Exception {
    if (inFrame == null) {
      return null;
    }
    StreamFrame frame = inFrame;
    frame = matrix.process(frame);
    trace.sendFrame(frame);
    frame = geometry.process(frame);

    return frame;
  }

  @Override
  public void close() {
    trace.close();
    matrix.close();
    geometry.close();
  }}
