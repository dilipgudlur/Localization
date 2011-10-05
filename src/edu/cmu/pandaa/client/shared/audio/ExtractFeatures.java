package com.google.cmusv.pandaa.audio;

import java.io.Serializable;
import com.google.cmusv.pandaa.audio.AcquireAudio.RawAudioFrame;
import com.google.cmusv.pandaa.stream.FrameStream.Frame;
import com.google.cmusv.pandaa.stream.FrameStream.Header;
import com.google.cmusv.pandaa.stream.FrameStream.LocalFrameStream;

class ExtractFeatures implements Runnable {
	LocalFrameStream in, out;
	FeatureFrame featureFrame;
	double threshold;	// threshold for amplitude
	int totalSampleBeenProcessed = 0;
	int bytesPerSample = 4;
	int sampleRate = 16000;
	int timeFrame = 100;	// ms
	int frameSample = sampleRate / 1000 * timeFrame;
	int frameCount = 0;
	int gjumped = 0;	// the unit of gJumped is frameSample

	private ExtractFeatures(LocalFrameStream in, LocalFrameStream out) {
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
				short[] bufferData = processAudio(frame);
				if (bufferData != null)
				{
					featureFrame.featureData = bufferData;
					out.sendFrame(featureFrame);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	class FeatureFrame extends Frame implements Serializable {
		int[] offset;
		short[] peaks;
	}

	class FeatureHeader extends Header implements Serializable {
	}

	/*
	 * this function processes the original audio in buffer1, discard silence,
	 * and save the impulses into buffer2. The length of data that is saved into
	 * buffer2 is returned.
	 */

	public short[] processAudio(short[] buffer1) throws Exception {

		int len = buffer1.length; // Should equal frameSample
		short[] buffer2 = new short[len + 3]; // store the frames with impulses
		
		if (len == frameSample) {
			double maxHeight = maxHeight(buffer1, 0, frameSample);
			if (maxHeight > threshold) {
				short[] sampleNum = int2short(totalSampleBeenProcessed);
				buffer2[0] = (short) gjumped;	//store the gJumped
				buffer2[1] = sampleNum[0];
				buffer2[2] = sampleNum[1];
				//store the number of total samples that have been processed
				for (int i = 0; i < frameSample; i++) {
					buffer2[3 + i] = buffer1[i];	//store the audio with impulses
				}
				gjumped = 0;
			} else {
				gjumped++; // jumping counter
				totalSampleBeenProcessed += frameSample;
				return null;
			}
			totalSampleBeenProcessed += frameSample;
			return buffer2;
		} else {
			throw new Exception(
					"The frame does not have the correct number of samples");
		}
	}

	private short[] int2short(int integer) {
		short[] shortArray = new short[2];
		shortArray[0] = (short) (integer & 0xFF);
		shortArray[1] = (short) (integer >> 16 & 0xFF);
		return shortArray;
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
		return max;
	}

	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double thr) {
		// TODO: set adaptive threshold
		threshold = thr;
	}
}