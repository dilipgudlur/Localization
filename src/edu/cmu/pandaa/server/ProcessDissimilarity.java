package edu.cmu.pandaa.server;

import java.io.Serializable;
import mdsj.MDSJ;
import edu.cmu.pandaa.shared.stream.FrameStream;
import edu.cmu.pandaa.shared.stream.header.GeometryHeader;
import edu.cmu.pandaa.shared.stream.header.GeometryHeader.*;

public class ProcessDissimilarity implements Runnable{
	FrameStream in, out;
	private boolean isHeaderSet;
	GeometryFrame geometryFrame;
	//int frameCount = 0; //maybe optional
	private int frameTime;
	String[] deviceIds;
	
	private static final int FRAME_TIME = 100;
	
	public ProcessDissimilarity(FrameStream in, FrameStream out)
	{
		this.in = in;
		this.out = out;
		isHeaderSet = false ;
		frameTime = FRAME_TIME;
		this.setDeviceIds(...);
	}
	
	public void run()
	{
		GeometryFrame gf;
		
		if (!isHeaderSet) {
			isHeaderSet = true;
			//frameStream.setHeader(new RawAudioHeader(frameTime, this.getFrequency(), this.getChannelConfiguration(), this.getAudioEncoding()));
			out.setHeader(new GeometryHeader(this.getDeviceIds()));
		}
		
		while ((gf = (GeometryFrame) in.recvFrame()) != null) {
			double[][] frame = gf.geometry;//todo
			//frameCount++;
			try {
				//geometryFrame = new GeometryFrame(frame);
				geometryFrame = processDissimilarity(frame);
				if (geometryFrame != null) {
					out.sendFrame(geometryFrame);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public GeometryFrame processDissimilarity(double[][] dissimilarity)
	{
		/*apply the multidimensional scaling*/
		GeometryFrame gf = new GeometryFrame();		
		int n=dissimilarity[0].length;    // number of data objects
		gf.geometry = MDSJ.classicalScaling(dissimilarity); // apply MDS
		return gf;
		
	}
	
	public void setDeviceIds(String[] deviceIds)
	{
		this.deviceIds = deviceIds;
	}
	
	public String[] getDeviceIds()
	{
		return deviceIds;
	}
}
