package edu.cmu.pandaa.shared.stream;

public class GeometryFrame extends FrameStream{
	
	double[][] relCoordinates; /*output of scaling algorithm*/
	String[] id;
	private File coordinatesFile; /*outputfile, read as spreadsheet or something*/
	
	public GeometryFrame(double[][] relCoordinates)
	{
		this.relCoordinates = relCoordinates ;		
		if(relCoordinates == null)
			throw new IllegalStateException("Invalid Input Diss");
	}
	
	public void generateCoordinatesFile()
	{
		/*writes the double[][] relCoordniates to coordinatesFile*/
	}

}
