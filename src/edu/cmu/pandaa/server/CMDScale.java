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

package edu.cmu.pandaa.server;

public class CMDScale {
	
	double[][] inputDistanceVector ;
	double[][] ouputDistanceVector ;
	
	public CMDScale(double[][] inputDistanceVector)
	{
		this.inputDistanceVector = inputDistanceVector ;		
	}
	
	public void applyCMDS()
	{   //use the library for CMDScaling
		//ouputDistanceVector = MDSJ.classicalScaling(inputDistanceVector);
		
	}

}
