package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.ImpulseHeader.ImpulseFrame;
import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.ImpulseFileStream;
import edu.cmu.pandaa.stream.RawAudioFileStream;

import java.util.ArrayList;

public class dbImpulseStreamModule implements StreamModule {
	private double usPerSample;
	private final int slowWindow = 1000;
	private final int fastWindow = 50;
	private final double jerk = 4;
	private final double base = 25;
	private ImpulseHeader header;
	private static int num = 0;
	private double rmsMax = Math.pow(2, 16) / 2;
	private double multiplier = 0.15;
	boolean prevPeak = false; // start with peak supression turned on
	private static double preRms = 0.0;
	private static double pre2Rms = 0.0;
	private static double microphoneRms = Double.MAX_VALUE;

	public dbImpulseStreamModule() {
	}

	public static void main(String[] args) throws Exception {
		int arg = 0, loops = 1;
		String impulseFilename = args[arg++];
		String audioFilename = args[arg++];
		if (args.length > arg || args.length < arg) {
			throw new IllegalArgumentException("Invalid number of arguments");
		}

		System.out.println("FeatureStream: " + impulseFilename + " "
				+ audioFilename);

		RawAudioFileStream rfs = new RawAudioFileStream(audioFilename);
		ImpulseFileStream foo = new ImpulseFileStream(impulseFilename, true);
		dbImpulseStreamModule ism = new dbImpulseStreamModule();
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
		// for (int i = 0; i < audio.audioData.length; i++) {
		// audio.audioData[i] = (short) (audio.audioData[i] / 2);
		// }
		for (int i = 0; i < impulses.peakOffsets.length; i++) {
			audio.audioData[timeToSampleOffset(impulses.peakOffsets[i])] = Short.MAX_VALUE;
		}
	}

	@Override
	public StreamHeader init(StreamHeader inHeader) {
		if (!(inHeader instanceof RawAudioHeader))
			throw new RuntimeException("Wrong header type");
		RawAudioHeader rah = (RawAudioHeader) inHeader;
		rah.initFilters(30, 0);
		int sampleRate = (int) rah.getSamplingRate();
		usPerSample = Math.pow(10, 6) / (double) sampleRate; // us per sample
		header = new ImpulseHeader(inHeader.id, inHeader.startTime,
				inHeader.frameTime);
		return header;
	}

	@Override
	public ImpulseFrame process(StreamFrame inFrame) {
		int peaks = 0;
		ArrayList<Integer> peakOffsets = new ArrayList<Integer>(1);
		ArrayList<Short> peakMagnitudes = new ArrayList<Short>(1);

		if (!(inFrame instanceof RawAudioFrame))
			throw new RuntimeException("Wrong frame type");

		RawAudioFrame raf = (RawAudioFrame) inFrame;
		double[] slowFrame = raf.smooth(slowWindow);
		double[] fastFrame = raf.smooth(fastWindow);
		double minSlowFrame = findMin(slowFrame);
		if (microphoneRms > minSlowFrame) {
			microphoneRms = minSlowFrame;
			// if(minSlowFrame<10)
			// microphoneRms=10;
		}
		int index_fast = -1;
		index_fast = maxDif(fastFrame, slowFrame, inFrame.seqNum == 0);
		short[] data = raf.getAudioData();
		if (!prevPeak && index_fast != -1) {
			peakOffsets.add(sampleToTimeOffset(index_fast));
			peakMagnitudes.add((short) fastFrame[index_fast]);
		}
		for (int i = 0; i < data.length; i++) {
			double slow = slowFrame[i];
			double fast = fastFrame[i];
			double tmp = (i % 2 == 0) ? -fast : slow;
			data[i] = (short) tmp;
		}
		return header.makeFrame(peakOffsets, peakMagnitudes);
	}

	private double rmsToDb(double p1, double p0) {
		double db = 10 * Math.log(p1 / p0);
		return db;
	}

	private double findMin(double[] slowFrame) {
		double min = Double.MAX_VALUE;
		for (int s_index = 0; s_index < slowFrame.length; s_index++) {
			if (slowFrame[s_index] < min)
				min = slowFrame[s_index];
		}
		return min;
	}

	private double findMax(double[] frame) {
		double max = 0.0;
		for (int s_index = 0; s_index < frame.length; s_index++) {
			if (frame[s_index] > max)
				max = frame[s_index];
		}
		return max;
	}

	private int maxDif(double[] fastFrame, double[] slowFrame, boolean first) {
		double[] fsDif = new double[slowFrame.length];
		for (int i = 0; i < slowFrame.length; i++)
			fsDif[i] = fastFrame[i] - slowFrame[i];
		double[] dif = new double[fastFrame.length];
		double max = 0.0;
		num = 0;
		int position = -1;
		int index1 = -1;
		ArrayList<Integer> index = new ArrayList<Integer>();
		ArrayList<Integer> flag = new ArrayList<Integer>();
		for (int j = first ? 500 : 0; j < fastFrame.length; j++) {
			// div[j - 2] = fastFrame[j - 1] - fastFrame[j - 2];

			if (rmsToDb(slowFrame[j], base) < 0) // Too quiet
			{
				if (rmsToDb(fsDif[j], jerk * base) > 0) {
					index.add(j);
					// System.out.println("S1: FastFrame: "+fastFrame[j]+" slowFrame: "+slowFrame[j]);
					flag.add(1);
				}

			} else if (rmsToDb(slowFrame[j], multiplier * rmsMax) > 0) // Too
																		// noisy
			{
				if (rmsToDb(fsDif[j], slowFrame[j] / 2) > 0) {
					index.add(j);
					// System.out.println("S2: FastFrame: "+fastFrame[j]+" slowFrame: "+slowFrame[j]);
					flag.add(2);
				}
			}

			else {
				if (rmsToDb(fsDif[j], slowFrame[j] * (jerk - 1)) > 0) {
					index.add(j);
					// System.out.println("S3: FastFrame: "+fastFrame[j]+" slowFrame: "+slowFrame[j]);
					flag.add(3);
				}

			}

		}

		for (int i = 0; i < index.size(); i++) {
			if (index.get(i) == 0) {
				dif[index.get(i)] = preRms - pre2Rms;
				if (dif[index.get(i)] > max) {
					max = dif[index.get(i)];
					index1 = index.get(i);
					position = i;
				}
			} else if (index.get(i) == 1) {
				dif[index.get(i)] = fastFrame[index.get(i) - 1] - preRms;
				if (dif[index.get(i)] > max) {
					max = dif[index.get(i)];
					index1 = index.get(i);
					position = i;
				}
			} else {
				dif[index.get(i) - 2] = fastFrame[index.get(i) - 1]
						- fastFrame[index.get(i) - 2];
				if (dif[index.get(i) - 2] > max) {
					max = dif[index.get(i) - 2];
					index1 = index.get(i);
					position = i;
				}
			}
			num++;

		}
		/*
		if (num > 0) {
			System.out.println(flag.get(position) + ": FastFrame: "
					+ fastFrame[index.get(position) - 1] + " slowFrame: "
					+ slowFrame[index.get(position) - 1]);
		}
		*/
		preRms = fastFrame[fastFrame.length - 1];
		pre2Rms = fastFrame[fastFrame.length - 2];
		return index1;
	}

	private int sampleToTimeOffset(int sample) {
		return (int) (sample * usPerSample);
	}

	private int timeToSampleOffset(int time) {
		return (int) ((double) time / usPerSample);
	}

	public void close() {
	}
}