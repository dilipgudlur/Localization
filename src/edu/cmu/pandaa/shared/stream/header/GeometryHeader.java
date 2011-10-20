package edu.cmu.pandaa.shared.stream.header;

import java.io.Serializable;

public class GeometryHeader extends StreamHeader implements Serializable {
	public String[] deviceIds;

  public GeometryHeader(String[] deviceIds, long startTime, int frameTime) {
    super(makeId(deviceIds), startTime, frameTime);
    this.deviceIds = deviceIds;
  }
  
  private static String makeId(String[] DeviceIds) {
	    StringBuilder ids = new StringBuilder();
	    for (int i=0; i<DeviceIds.length;i++) {
	      ids.append(DeviceIds[i]);
	      ids.append(',');
	    }
	    return ids.toString(); // return a single string(pseudo-master ID)
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
