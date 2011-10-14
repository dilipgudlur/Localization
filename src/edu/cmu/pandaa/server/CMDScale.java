package edu.cmu.pandaa.server;
/*Step g: Classical MultiDimensional Scaling : 
 * takes an input matrix giving dissimilarities between pairs of items and 
 * outputs a coordinate matrix
 * Input: pairwise distances between devices, say there are 3 phones, A, B, C 
 * then input will be a matrix of Dab, Dbc, Dca
 * Output: Spatial coordinates (relative) to the origin or reference device
 * http://www.inf.uni-konstanz.de/algo/software/mdsj/ provides library for Classical MDS
 * 
 * 
 * [Y,eigvals] = cmdscale(D);
cmdscale produces two outputs. The first output, Y, is a matrix containing the reconstructed 
points. The second output, eigvals, is a vector containing the sorted eigenvalues of what is 
often referred to as the "scalar product matrix," which, in the simplest case, is equal to 
Y*Y'. The relative magnitudes of those eigenvalues indicate the relative contribution of the 
corresponding columns of Y in reproducing the original distance matrix D with the reconstructed
points.
 * */



import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import edu.cmu.pandaa.shared.stream.FileStream;
import edu.cmu.pandaa.shared.stream.FrameStream;
import edu.cmu.pandaa.shared.stream.header.StreamHeader;
import edu.cmu.pandaa.shared.stream.header.GeometryHeader.GeometryFrame;

public class CMDScale {
	
	//double[][] dissimilarity;
	//double[][] relCoordinates;
	int numFrames = 0;
	
	/*public CMDScale(double[][] dissimilarity) //not sure if this correct
	{
		this.dissimilarity = dissimilarity ;		
	}*/
	
	public static void main(String[] args)
	{
		/*populates GeometryFrame members*/
		String inFile = "D:/dissimilarity.txt";
		String outFile = "D:/relCoordinate.txt";
		FrameStream inputDissimilarityFile = new FileStream(inFile);
		FrameStream ouptutCoordinateFile = new FileStream(outFile);
		ProcessDissimilarity dissimilarity = new ProcessDissimilarity(inputDissimilarityFile, ouptutCoordinateFile);
		Thread th = new Thread(dissimilarity);
		th.start();
		
		/*StreamHeader h = ouptutCoordinateFile.getHeader();
		GeometryFrame f = null;
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(new File("D:\relCoordinates.txt")));
			while ((f = (GeometryFrame) ouptutCoordinateFile.recvFrame()) != null) {
				//numFrames++;
				//oos.writeLong(f.geometry);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

	}	
}
