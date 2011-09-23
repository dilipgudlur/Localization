package src;
/* Class DecodeAudio to process the raw packet data from the channel and prepare the audio 
 * for peak detection
 * Inputs: 	RawAudio - raw audio channel data from each device
 * 			Handles - object to provide audio settings like thresholds, sampling rates etc
 * 			DeviceData - device specific data like id, port, tcpCount, audio samples
 * Output:	For each device, the updated DeviceData object shall contain the aligned audio for
 * 			each device*/


public class DecodeAudio implements Runnable{
	
	RawAudio raw;		//Class to retrieve int32 packet data with actual raw audio
	InitParameters handle;		// Class to manage settings for computation,thresholds,sampling rates etc
	MakeConnection connData;	//Class to manage device specific data obtained from network pertaining to ip, port, device id, tcpCount
	
	/*Constructor for current class*/
	public DecodeAudio(RawAudio raw, InitParameters handle, MakeConnection connData)
	{
		this.raw = raw;
		this.handle = handle;
		this.connData = connData;
	}
		
	/* function to detect the special symbol FFFFFFF7 in the feature frame and extraction of the 
	 * individual samples for each device*/
	public void processFeatureFrame()
	{}
	
	/*function to concatenate audio samples after detecting the special symbol */
	public void concatenateAudio()
	{}
	
	/*for each device, aligns the start and end of each concatenated audio stream, little ambiguous*/
	public void alignAudio()
	{}

	@Override
	public void run() {
		processFeatureFrame();
		concatenateAudio();
		alignAudio();		
	}
}

