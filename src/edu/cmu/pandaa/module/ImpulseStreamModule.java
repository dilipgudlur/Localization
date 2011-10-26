package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.ImpulseHeader.ImpulseFrame;
import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.FrameStream;
import edu.cmu.pandaa.stream.ImpulseFileStream;
import edu.cmu.pandaa.stream.RawAudioFileStream;

public class ImpulseStreamModule implements StreamModule {
	private double max = 0.00305;
	private int sampleRate = 16000;
	int nsPerSample = 10 ^ 9 / sampleRate; // nanosecond per sample
	private double threshold = max / 2; // threshold for amplitude

	int timeFrame = 100; // ms
	private ImpulseHeader header;

	public ImpulseStreamModule() {
	}

	public static void main(String[] args) throws Exception {
		try {
			ImpulseStreamModule ism = new ImpulseStreamModule();
			ism.run("test/sample_input-1.wav", "test/impulses-1.txt");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * Example2: A run method that could be used to create a new thread to test
	 * just this class
	 */
	public void run(String audioFilename, String impulseFilename) {
		try {

			ImpulseFileStream foo = new ImpulseFileStream(impulseFilename, true);

			RawAudioFileStream rfs = new RawAudioFileStream(audioFilename);

			ImpulseStreamModule ism = new ImpulseStreamModule();

			RawAudioHeader header = (RawAudioHeader) rfs.getHeader();
			ImpulseHeader iHeader = (ImpulseHeader) ism.init(header);
			foo.setHeader(iHeader);

			RawAudioFrame audioFrame = null;
			while ((audioFrame = (RawAudioFrame) rfs.recvFrame()) != null) {
				StreamFrame streamFrame = ism.process(audioFrame);
				foo.sendFrame(streamFrame);
			}

			/*
			 * ImpulseHeader header2 = foo.getHeader(); ImpulseFrame frame2 =
			 * foo.recvFrame(); frame2 = foo.recvFrame();
			 */
			foo.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public StreamHeader init(StreamHeader inHeader) {
		if (!(inHeader instanceof RawAudioHeader))
			throw new RuntimeException("Wrong header type");
		int tmp = (int) ((RawAudioHeader) inHeader).getSamplingRate();
		if (tmp != 0) {
			sampleRate = tmp;
		}
		nsPerSample = 10 ^ 9 / sampleRate; // nanosecond per sample
		header = new ImpulseHeader(inHeader.id, inHeader.startTime,
				inHeader.frameTime);
		return header;
	}

	@Override
	public StreamFrame process(StreamFrame inFrame) {
		if (!(inFrame instanceof RawAudioFrame))
			throw new RuntimeException("Wrong frame type");
		int index = 0;
		short[] frame = ((RawAudioFrame) inFrame).getAudioData();
		int peakNum = maxHeight(frame, 0, frame.length);
		short[] peakMagnitudes = new short[peakNum];
		int[] peakOffsets = new int[peakNum];
		if (peakNum > 0) {
			for (int i = 0; i < frame.length; i++) {
				double value = java.lang.Math.abs((double) frame[i]) / 65535.0;
				if (value > threshold) {
					peakMagnitudes[index] = frame[i];
					peakOffsets[index] = (inFrame.seqNum * frame.length + i)
							* nsPerSample;
					index++;
				}
			}
		}
		ImpulseFrame impulseFrame = header.new ImpulseFrame(peakOffsets,
				peakMagnitudes);
		return impulseFrame;
	}

	private int maxHeight(short[] frame, int start_index, long frameSample) {

		double max = 0.0;
		int i = 0;
		int peakNum = 0;
		while (i < frameSample) {
			double value = java.lang.Math.abs((double) frame[start_index + i]) / 65535.0;
			if (value >= threshold) {
				peakNum++;
			}
			if (value > max) {
				max = value;
			}
			i++;
		}
		if (max > this.max) {
			// adaptThreshold(max);
		}
		return peakNum;
	}

	private void adaptThreshold(double maxH) {
		setThreshold(0.5 * maxH);
		max = maxH;
	}

	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double thr) {
		threshold = thr;
	}

	public void close() {
		max = 0.00305;
		threshold = max / 2;
	}

}