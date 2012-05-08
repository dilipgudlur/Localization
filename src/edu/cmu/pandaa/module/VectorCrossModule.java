package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.DistanceHeader;
import edu.cmu.pandaa.header.DistanceHeader.DistanceFrame;
import edu.cmu.pandaa.header.MatrixHeader;
import edu.cmu.pandaa.header.MatrixHeader.MatrixFrame;
import edu.cmu.pandaa.header.MultiHeader;
import edu.cmu.pandaa.header.MultiHeader.MultiFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 4/16/12
 * Time: 5:29 PM
 */

public class VectorCrossModule implements StreamModule {
  DistanceHeader header;

  @Override
  public StreamHeader init(StreamHeader inHeader) throws Exception {
    MultiHeader mh = (MultiHeader) inHeader;
    header = new DistanceHeader(inHeader);
    return header;
  }

  @Override
  public StreamHeader.StreamFrame process(StreamFrame inFrame) throws Exception {
    MultiFrame mf = (MultiHeader.MultiFrame) inFrame;
    StreamFrame[] frames = mf.getFrames();
    if (frames.length != 2) {
      throw new RuntimeException("Should be two frames");
    }
    double[] offsets = getCorrelations((MatrixFrame) frames[0], (MatrixFrame) frames[1]);
    double[] magnitudes = offsets == null ? null : new double[offsets.length];
    if (magnitudes != null) {
      for (int i = 0;i < magnitudes.length;i++) {
        magnitudes[i] = 1.0;
      }
    }
    return header.makeFrame(offsets, magnitudes);
  }

  @Override
  public void close() {
  }

  private double[] getCorrelations(MatrixFrame a, MatrixFrame b) {
    if (a == null || b == null || a.data == null || b.data == null) {
      return null;
    }
    if (a.data[0].length != b.data[0].length) {
      throw new RuntimeException("Matrix size discrepancy");
    }
    int len = a.data[0].length;
    double[] result = new double[len];
    for (int j = 0; j < len; j++) {
      double cross = 0;
      for (int i = 0; i < a.data.length; i++) {
        cross += a.data[i][j] * b.data[i][j];
      }
      result[j] = cross;
    }
    return result;
  }
}
