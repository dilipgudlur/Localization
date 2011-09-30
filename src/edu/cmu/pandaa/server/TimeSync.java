package edu.cmu.pandaa.server;

/*Stepc: class TimeSync keeps track of time shifts between audio streams from each device
 * Input: Incoming Timestamp for each audio stream
 * Ouput: Relative shifts between audio for each device*/
public class TimeSync {

	double[] timeShifts;
	
	public TimeSync(double[] timeStamps)
	{
		this.timeShifts = timeStamps ;		
	}
		
	public void getTimeShift()
	{
		//extracts time shifts from audio timestamps
		//saves the shifts in a file
	}
	
	/*takes time shifts as input for each device, the devices are in different time domains
	 * and brings them all in one time domain*/
	public void alignAudio()
	{}

	
}
