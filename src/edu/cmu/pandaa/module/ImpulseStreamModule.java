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
	private double max = 1;
	private int sampleRate = 16000;
	private double threshold = max / 2; // threshold for amplitude
	private static int sampleProcessed; // store the number of total samples
										// that have
	// been processed
	private ImpulseHeader header;

	public ImpulseStreamModule() {
		super();
		sampleProcessed = 0;
	}

	/*
	 * Example2: A run method that could be used to create a new thread to test
	 * just this class
	 */
	public void run() {
		try {

			String filename = "testImpulse.txt";
			ImpulseFileStream foo = new ImpulseFileStream(filename, true);

			RawAudioFileStream rfs = new RawAudioFileStream(
					"sample_music_in_frames.wav");

			ImpulseStreamModule ism = new ImpulseStreamModule();

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

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public StreamHeader init(StreamHeader inHeader) {
		if (!(inHeader instanceof RawAudioHeader))
			throw new RuntimeException("Wrong header type");
		int tmp = (int) ((RawAudioHeader) inHeader).getSamplingRate();
		if (tmp != 0)
			sampleRate = tmp;
		ImpulseHeader inheader = (ImpulseHeader) inHeader;
		header = new ImpulseHeader(inheader.id, inheader.startTime,
				inheader.frameTime);
		return header;
	}

	@Override
	public StreamFrame process(StreamFrame inFrame) {
		if (!(inFrame instanceof RawAudioFrame))
			throw new RuntimeException("Wrong frame type");

		int timeFrame = 100; // ms
		int index = 0;
		int nsPerSample = 10 ^ 9 / sampleRate; // nanosecond per sample
		int frameSample = sampleRate / 1000 * timeFrame;
		short[] peakMagnitudes = new short[frameSample];
		int[] peakOffsets = new int[frameSample];
		short[] frame = ((RawAudioFrame) inFrame).getAudioData();

		double maxHeight = maxHeight(frame, 0, frameSample);
		if (maxHeight > threshold) {
			for (int i = 0; i < frameSample; i++) {
				double value = java.lang.Math.abs((double) frame[i]) / 65535.0;
				if (value > threshold) {
					peakMagnitudes[index] = frame[i];
					peakOffsets[index] = sampleProcessed * nsPerSample;
					index++;
				}
				sampleProcessed++;

			}
			ImpulseFrame impulseFrame = header.new ImpulseFrame(peakOffsets,
					peakMagnitudes);
			return impulseFrame;
		} else {
			sampleProcessed += frameSample;
			return null;
		}
	}

	private double maxHeight(short[] frame, int start_index, long frameSample) {

		double max = 0.0;
		int i = 0;
		while (i < frameSample) {
			double value = java.lang.Math.abs((double) frame[start_index + i]) / 65535.0; // TODO
			if (value > max) {
				max = value;
			}
			i++;
		}
		if (max > this.max) {
			adaptThreshold(max);
		}
		return max;
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
		max = 20;
		threshold = max / 2;
		setSampleProcessed(0);
	}

	public static void setSampleProcessed(int sample) {
		sampleProcessed = sample;
	}
}
