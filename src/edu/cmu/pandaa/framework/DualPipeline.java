package edu.cmu.pandaa.framework;

import edu.cmu.pandaa.header.*;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.module.*;
import edu.cmu.pandaa.stream.CalibrationManager;
import edu.cmu.pandaa.stream.DistanceFileStream;
import edu.cmu.pandaa.stream.FileStream;

import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 10/18/11
 * Time: 4:11 PM
 */

public class DualPipeline implements StreamModule {
  final Set<StreamHeader> devices;
  static final double calFactor = 1.0;

  /* First step is to take in a multiFrame consisting of impulseframes and turn it into time differences */
  TDOACrossModule tdoa = new TDOACrossModule();

  /* Then take the output and smooth and average over time */
  StreamModule distance = new DistanceFilter(100);

  FileStream trace;

  public DualPipeline(Set<StreamHeader> devides) {
    this.devices = devides; // do not synchronize on this in the constructor, since it's locked by the caller
  }

  @Override
  public StreamHeader init(StreamHeader inHeader) throws Exception {
    MultiHeader multiHeader = (MultiHeader) inHeader;
    if (!(multiHeader.getOne() instanceof ImpulseHeader)) {
      throw new IllegalArgumentException("Dual pipe multiheader should contain ImpulseHeaders");
    }

    multiHeader.waitForHeaders(2);

    StreamHeader header = inHeader;

    tdoa.setCalibrationFile(new CalibrationManager(null, true, multiHeader.getHeaders()[0].id,
            multiHeader.getHeaders()[1].id, calFactor));
    header = tdoa.init(header);
    header = distance.init(header);

    if (!(header instanceof DistanceHeader)) {
      throw new IllegalArgumentException("Output should be DistanceHeader");
    }

    trace = new DistanceFileStream(App.TRACE_DIR + header.id + ".txt", true);
    trace.setHeader(header);

    return header;
  }

  @Override
  public StreamFrame process(StreamFrame inFrame) throws Exception {
    if (inFrame == null) {
      return null;
    }
    StreamFrame frame = inFrame;
    frame = tdoa.process(frame);
    frame = distance.process(frame);
    trace.sendFrame(frame);
    return frame;
  }

  @Override
  public void close() {
    trace.close();
    tdoa.close();
    distance.close();
  }
}
