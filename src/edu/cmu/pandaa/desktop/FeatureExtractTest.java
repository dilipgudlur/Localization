package edu.cmu.pandaa.desktop;

import java.io.IOException;

import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.ImpulseHeader.ImpulseFrame;
import edu.cmu.pandaa.header.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.FrameStream;
import edu.cmu.pandaa.stream.ImpulseFileStream;
import edu.cmu.pandaa.stream.RawAudioFileStream;
import edu.cmu.pandaa.module.ImpulseStreamModule;

public class FeatureExtractTest {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		String filename = "testImpulse.txt";
		ImpulseFileStream foo = new ImpulseFileStream(filename, true);

		RawAudioFileStream rfs = new RawAudioFileStream(
				"sample_music_in_frames.wav");
		
		ImpulseStreamModule ism = new ImpulseStreamModule(rfs,foo);
		ImpulseStreamModule.setSampleProcessed(0);
		
		RawAudioHeader header = (RawAudioHeader) rfs.getHeader();
		ImpulseHeader iHeader = (ImpulseHeader) ism.init(header);
		foo.setHeader(iHeader);

		RawAudioFrame audioFrame = null;
		while ((audioFrame = (RawAudioFrame) rfs.recvFrame()) != null) {
			// impulseDetectionModuleObject.process(audioFrame)
			StreamFrame streamFrame = ism.process(audioFrame);
			if (streamFrame != null) {
				foo.sendFrame(streamFrame);
			}
		}

	    ImpulseHeader header2 = foo.getHeader();
	    ImpulseFrame frame2 = foo.recvFrame();
	    frame2 = foo.recvFrame();
	    foo.close();
	    
	}

}
