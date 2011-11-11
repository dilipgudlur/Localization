package edu.cmu.pandaa.header;

import java.io.Serializable;

public class GeometryHeader extends StreamHeader implements Serializable {
  public String[] deviceIds;
  public int rows, cols;

  public GeometryHeader(String[] deviceIds, long startTime, int frameTime, int rows, int cols) {
    super(makeId(deviceIds), startTime, frameTime);
    this.deviceIds = deviceIds;
    this.rows = rows;
    this.cols = cols;
  }

  private static String makeId(String[] DeviceIds) {
    StringBuilder ids = new StringBuilder();
    for (int i=0; i<DeviceIds.length;i++) {
      ids.append(',');
      ids.append(DeviceIds[i]);
    }
    return ids.substring(1); // return a single string(pseudo-master ID)
  }

  public class GeometryFrame extends StreamFrame implements Serializable {
    public double[][] geometry;

    public GeometryFrame(int seq, double[][] geometry) {
      super(seq);
      init(geometry);
    }

    public GeometryFrame(double[][] geometry) {
      init(geometry);
    }

    private void init(double[][] geometry) {
      if (geometry.length != rows)
        throw new IllegalArgumentException("Gemoetry rows does not match");
      if (geometry.length > 0 && geometry[0].length != cols)
        throw new IllegalArgumentException("Geometry cols does not match");
      this.geometry = geometry;
    }
  }

  public GeometryFrame makeFrame(double[][] geometry) {
    return new GeometryFrame(geometry);
  }

  public GeometryFrame makeFrame(int seq, double[][] geometry) {
    return new GeometryFrame(seq, geometry);
  }
}