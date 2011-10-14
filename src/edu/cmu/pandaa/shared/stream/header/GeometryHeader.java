package edu.cmu.pandaa.shared.stream.header;

import java.io.Serializable;

public class GeometryHeader extends StreamHeader implements Serializable{
	String[] deviceIds ;
	
	public static class GeometryFrame extends StreamFrame implements Serializable {
		double[][] geometry;  
	}
}
