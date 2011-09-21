
/*class to handle the raw packets for further processing/preparation to enable peak detection*/
/*Inputs : Integer array of packets from the channel; Audio Settings Object; Channel Data Object*/
/*Output: Aligned audio from each client ready for pairwise peak detection*/

public class DecodeAudio implements Runnable{

	RawAudio raw ; //Class to retrieve int32 packet data with actual raw audio
	Handles hndl;	// Class to manage settings for computation,thresholds,sampling rates etc
	DeviceData device;	//Class to manage device specific data obtained from network pertaining to ip, port, device id, tcpCount
		
	/*Constructor for current class*/
	public DecodeAudio(RawAudio raw, Handles hndl, DeviceData device)
	{
		this.raw = raw;  
		this.hndl = hndl;    
		this.device = device;  
	}
	
	/*detects a particular byte stream (FFFFFFF7) which is set to detect impulse in audio
	 * extraction of the sample value happens based on this byte stream*/	
	public void detectImpulse()
	{}							
	
	/*combines all the individual samples values from the feature frame to form a single audio
	 * stream*/
	public void concatenateAudio()
	{}
	
	/*Not sure what is being done here, but this might be aligning the start and end of the 
	 * individual audio streams from each device*/
	public void alignAudio()
	{}
	@Override
	public void run() {
		detectImpulse();
		concatenateAudio();
		alignAudio();
	}
}
