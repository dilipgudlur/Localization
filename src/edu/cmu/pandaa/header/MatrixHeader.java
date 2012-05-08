package edu.cmu.pandaa.header;

import edu.cmu.pandaa.stream.FileStream;
import edu.cmu.pandaa.stream.GeometryFileStream;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MatrixHeader extends StreamHeader implements Serializable {
  public int rows, cols;
  private double[] prevX;
  Map<String,Integer> indexMap;

  public MatrixHeader(String[] deviceIds, long startTime, int frameTime, int rows, int cols) {
    super(makeId("x",deviceIds), startTime, frameTime);
    this.rows = rows;
    this.cols = cols;
    if (deviceIds.length != rows)
      throw new IllegalArgumentException("Mismatched array dimensions");
  }

  public MatrixHeader(StreamHeader prototype, int rows, int cols) {
    super(prototype);
    this.rows = rows;
    this.cols = cols;
  }

  public MatrixHeader(String id, long startTime, int frameTime, int rows, int cols) {
    super(id, startTime, frameTime);
    this.rows = rows;
    this.cols = cols;
  }

  public int indexOf(String id) {
    if (indexMap == null) {
      String[] ids = getIds();
      indexMap = new HashMap<String, Integer>(ids.length);
      for (int i = 0;i < ids.length;i++) {
        indexMap.put(ids[i], i);
      }
    }
    return indexMap.get(id);
  }

  public class MatrixFrame extends StreamFrame implements Serializable {
    public double[][] data;
    public int line;

    public MatrixFrame() {
    }

    public MatrixFrame(int seq, double[][] data) {
      super(seq);
      init(data);
    }

    public MatrixFrame(double[][] data) {
      init(data);
    }

    public MatrixFrame(StreamFrame prototype, double[][] data) {
      super(prototype);
      init(data);
    }

    private void init(double[][] data) {
      if (cols > 0 && data.length != cols)
        throw new IllegalArgumentException("Data cols does not match");
      if (rows > 0 && data.length > 0 && data[0].length != rows)
        throw new IllegalArgumentException("Data rows does not match");
      this.data = data;
    }
  }

  public MatrixFrame makeFrame() {
    return new MatrixFrame();
  }

  public MatrixFrame makeFrame(double[][] data) {
    return new MatrixFrame(data);
  }

  public MatrixFrame makeTransposedFrame(double[][] data) {
    double[][] ndata = new double[data[0].length][data.length];
    for (int i = 0;i < data.length; i++) {
      for (int j = 0;j < data[0].length; j++) {
        ndata[j][i] = data[i][j];
      }
    }
    return new MatrixFrame(ndata);
  }

  public MatrixFrame makeFrame(StreamFrame prototype, double[][] data) {
    return new MatrixFrame(prototype, data);
  }

  public MatrixFrame makeFrame(int seq, double[][] data) {
    return new MatrixFrame(seq, data);
  }

  public FileStream createOutput()  throws Exception {
    return new GeometryFileStream();
  }
}