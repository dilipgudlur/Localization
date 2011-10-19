package edu.cmu.pandaa.shared.stream.header;

import java.io.Serializable;

import edu.cmu.pandaa.shared.stream.header.FeatureHeader.FeatureFrame;

public class GeometryHeader extends StreamHeader implements Serializable {
	public String[] deviceIds;

  public GeometryHeader(String id, long startTime, int frameTime) {
    super(id, startTime, frameTime);
  }

	public class GeometryFrame extends StreamFrame implements Serializable {
		public double[][] geometry;
		
		public GeometryFrame(int seq, double[][] geometry) {
		      super(seq);
		      this.geometry = geometry;
		}

	    public GeometryFrame(double[][] geometry) {
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
