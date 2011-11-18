package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.ImpulseHeader.ImpulseFrame;
import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.ImpulseFileStream;
import edu.cmu.pandaa.stream.RawAudioFileStream;

import java.awt.image.LookupOp;

public class ImpulseStreamModule implements StreamModule {

	private int sampleRate, nsPerSample;
	private double thd = 0.8; // threshold for peak
	private double thdNoise = 0.3; // threshold for noise
	private ImpulseHeader header;
	int numSilence = 200; // find the silence period (200 samples long)
	int numFindPeak = 50; // find the highest peak among 50 samples
	static boolean previousSilence; // whether the previous frame is ending with
									// silence

	public ImpulseStreamModule() {
	}

	public static void main(String[] args) throws Exception {

		int arg = 0, loops = 1;
		String impulseFilename = args[arg++];
		String audioFilename = args[arg++];
		if (args.length > arg || args.length < arg) {
			throw new IllegalArgumentException("Invalid number of arguments");
		}

		System.out.println("ImpulseStream: " + impulseFilename + " "
				+ audioFilename);

		RawAudioFileStream rfs = new RawAudioFileStream(audioFilename);
		ImpulseFileStream foo = new ImpulseFileStream(impulseFilename, true);
		ImpulseStreamModule ism = new ImpulseStreamModule();
		RawAudioFileStream rfso = new RawAudioFileStream(impulseFilename
				+ ".wav", true);

		RawAudioHeader header = (RawAudioHeader) rfs.getHeader();
		rfso.setHeader(header);
		ImpulseHeader iHeader = (ImpulseHeader) ism.init(header);
		foo.setHeader(iHeader);

		RawAudioFrame audioFrame = null;
		while ((audioFrame = (RawAudioFrame) rfs.recvFrame()) != null) {
			ImpulseFrame streamFrame = ism.process(audioFrame);
			foo.sendFrame(streamFrame);
			ism.augmentAudio(audioFrame, streamFrame);
			rfso.sendFrame(audioFrame);
		}
		rfso.close();
		foo.close();
	}

	private void augmentAudio(RawAudioFrame audio, ImpulseFrame impulses) {
		for (int i = 0; i < audio.audioData.length; i++) {
			audio.audioData[i] = (short) (audio.audioData[i] / 2);
		}
		for (int i = 0; i < impulses.peakOffsets.length; i++) {
			audio.audioData[timeToSampleOffset(impulses.peakOffsets[i])] = Short.MAX_VALUE;
		}
	}

	@Override
	public StreamHeader init(StreamHeader inHeader) {
		if (!(inHeader instanceof RawAudioHeader))
			throw new RuntimeException("Wrong header type");
		int sr = (int) ((RawAudioHeader) inHeader).getSamplingRate();
		if (sr != 0) {
			sampleRate = sr;
		}
		nsPerSample = 10 ^ 9 / sampleRate; // nanosecond per sample
		header = new ImpulseHeader(inHeader.id, inHeader.startTime,
				inHeader.frameTime);
		return header;
	}

	@Override
	public ImpulseFrame process(StreamFrame inFrame) {
		if (!(inFrame instanceof RawAudioFrame))
			throw new RuntimeException("Wrong frame type");

		short[] frame = ((RawAudioFrame) inFrame).getAudioData();
		double[] frameD = short2double(frame);
		int numMaxPeaks = 20;
		int iPeaks = 0;
		int[] peakLocations = new int[numMaxPeaks];
		double MaxHeight;
		int HeightLocation;

		int ii = 0;
		double sum = 0.0;
		double mean = 0.0;
		if (previousSilence) {
			while (ii < frameD.length) {
				if (frameD[ii] > thd && isPeak(frame, ii)) {
					MaxHeight = frameD[ii];
					HeightLocation = ii;
					peakLocations[iPeaks++] = HeightLocation;
					break;
				}
				ii++;
			}
		}
		ii = numSilence;
		while (ii < frameD.length) {
			sum = 0.0;
			for (int jj = 0; jj < numSilence; jj++) {
				sum += frameD[ii - jj - 1];
			}
			mean = sum / numSilence;

			if (ii == frameD.length - 1) {
				previousSilence = (mean < thdNoise) ? true : false;
			}

			if (mean > thdNoise) {
				ii++;
			} else if (frameD[ii] < thd) {
				ii++;
			} else {
				MaxHeight = frameD[ii];
				HeightLocation = ii;
				for (int nextSample = 0; nextSample < numFindPeak; nextSample++) {
					if (nextSample + ii == frameD.length)
						break;
					else if (MaxHeight < frameD[nextSample + ii]
							&& isPeak(frame, nextSample + ii)) {
						MaxHeight = frameD[nextSample + ii];
						HeightLocation = nextSample + ii;
					}
				}
				peakLocations[iPeaks++] = HeightLocation;
				if (iPeaks >= numMaxPeaks)
					break;
				ii = HeightLocation + 1;
			}

		}

		short[] peakMagnitudes = new short[iPeaks];
		int[] peakOffsets = new int[iPeaks];

		for (int i = 0; i < iPeaks; i++) {
			peakMagnitudes[i] = frame[peakLocations[i]];
			peakOffsets[i] = sampleToTimeOffset(peakLocations[i]);
		}

		ImpulseFrame impulseFrame = header.new ImpulseFrame(peakOffsets,
				peakMagnitudes);
		return impulseFrame;
	}

	private double[] short2double(short[] frame) {
		double[] frameD = new double[frame.length];
		for (int i = 0; i < frame.length; i++) {
			frameD[i] = java.lang.Math.abs((double) frame[i]) / 32768.0;
		}
		return frameD;
	}

	private int sampleToTimeOffset(int sample) {
		return sample * nsPerSample;
	}

	private int timeToSampleOffset(int time) {
		return time / nsPerSample;
	}

	/*
	 * Check whether there are lower points around the sample.
	 */
	private boolean isPeak(short[] frame, int i) {
		if (i == frame.length - 1) {
			if (frame[i] > 0 && frame[i] > frame[i - 1]) {
				return true;
			}
			if (frame[i] < 0 && frame[i] < frame[i - 1]) {
				return true;
			}
			return false;
		} else if (i == 0) {
			if (frame[i] > 0 && frame[i] > frame[i + 1]) {
				return true;
			}
			if (frame[i] < 0 && frame[i] < frame[i + 1]) {
				return true;
			}
			return false;
		} else {
			if (frame[i] > 0 && frame[i] > frame[i - 1]
					&& frame[i] > frame[i + 1]) {
				return true;
			}
			if (frame[i] < 0 && frame[i] < frame[i - 1]
					&& frame[i] < frame[i + 1]) {
				return true;
			}
		}
		return false;
	}

	public void setThreshold(double thr, double thrN) {
		thd = thr;
		thdNoise = thrN;
	}

	public void close() {
	}

}