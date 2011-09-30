package edu.cmu.pandaa.server;
/*Step e&f: class TDOA performs cross correlation of audio streams. 
 * Input: time of occurrence of impulse/peak for each device
 * Output: pairwise distances between all the devices in the system*/

public class TDOA {

	double[] d1; //represents timestamp for a correlated peak for device1
	double[] d2; //represents timestamp for a correlated peak for device2
	double[] pairwiseDistance;
	
	public TDOA(double[] d1, double[] d2)
	{				
	}
	
	/*performs average of pairwise TDOA over a short time, each pair average is computed
	 * exclusively and kept separate*/
	public void averageTDOA()
	{}
	
	/*performs cross correlation using the the timestamps and amplitudes of peaks*/
	public void computeTDOA()
	{}
	
	/*gives pairwise distances using the time difference calulation*/
	public void computePairwiseDistances()
	{
		
	}
}
