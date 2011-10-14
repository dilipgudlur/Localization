package edu.cmu.pandaa.client.shared.audio;

import edu.cmu.pandaa.shared.stream.FeatureData.FeatureFrame;
import edu.cmu.pandaa.shared.stream.FrameStream;
import edu.cmu.pandaa.shared.stream.RawAudio.RawAudioFrame;

class ExtractFeatures implements Runnable {
	FrameStream in, out;
	FeatureFrame featureFrame;
	double threshold; // threshold for amplitude
	double max = 20;
	int totalSampleBeenProcessed = 0;
	int bytesPerSample = 4;
	int sampleRate = 16000;
	int timeFrame = 100; // ms
	int frameSample = sampleRate / 1000 * timeFrame;
	int frameCount = 0;
	int gjumped = 0; // the unit of gJumped is frameSample

	private ExtractFeatures(FrameStream in, FrameStream out) {
		this.in = in;
		this.out = out;
		this.setThreshold(10);
	}

	public void run() {

		// Write Header to FrameStream
		RawAudioFrame af;
		out.setHeader(in.getHeader());

		// read raw audio from the original audio file
		while ((af = (RawAudioFrame) in.recvFrame()) != null) {
			short[] frame = af.audioData;
			frameCount++;
			try {
				featureFrame = new FeatureFrame();
				featureFrame = processAudio(frame);
				if (featureFrame != null) {
					// featureFrame.featureData = bufferData;
					out.sendFrame(featureFrame);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/*
	 * this function processes the original audio in buffer1, discard silence,
	 * and save the impulses into buffer2. The length of data that is saved into
	 * buffer2 is returned.
	 */

	public FeatureFrame processAudio(short[] buffer1) throws Exception {
		int index = 0;
		FeatureFrame ff = new FeatureFrame();

			double maxHeight = maxHeight(buffer1, 0, frameSample);
			if (maxHeight > threshold) {
				for (int i = 0; i < frameSample; i++) {
					double value = java.lang.Math.abs((double) buffer1[i]) / 65536.0;
					if (value > threshold) {
						ff.peaks[index] = buffer1[i];
						ff.offset[index] = totalSampleBeenProcessed;
						index++;
					}
					totalSampleBeenProcessed++;
					// store the number of total samples that have been
					// processed
				}
				//gjumped = 0;
			} else {
				//gjumped++; // jumping counter
				totalSampleBeenProcessed += frameSample;
				return null;
			}
			return ff;
		} 


	public double maxHeight(short[] buffer1, int start_index, int len) {
		double max = 0.0;
		int i = 0;
		while (i < len) {
			double value = java.lang.Math
					.abs((double) buffer1[start_index + i]) / 65536.0;
			if (value > max) {
				max = value;
			}
			i++;
		}
		adaptThreshold(max);
		return max;
	}

	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double thr) {
		// TODO: set adaptive threshold
		threshold = thr;
	}

	public void adaptThreshold(double maxH) {
		if (maxH > max) {
			setThreshold(0.5 * maxH);
			max = maxH;
		}

	}
}