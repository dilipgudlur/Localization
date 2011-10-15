package edu.cmu.pandaa.shared.stream.header;

import java.io.Serializable;

public class GeometryHeader extends StreamHeader implements Serializable {
	String[] deviceIds;

  public GeometryHeader(String id, long startTime, int frameTime) {
    super(id, startTime, frameTime);
  }

	public class GeometryFrame extends StreamFrame implements Serializable {
		double[][] geometry;  
	}
}
