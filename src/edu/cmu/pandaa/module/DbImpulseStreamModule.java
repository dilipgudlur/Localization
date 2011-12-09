package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.ImpulseHeader.ImpulseFrame;
import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.ImpulseFileStream;
import edu.cmu.pandaa.stream.RawAudioFileStream;
import edu.cmu.pandaa.utils.ImpulseUtil;

import java.util.ArrayList;

public class DbImpulseStreamModule implements StreamModule {

	private ImpulseHeader header;
	//private static int num = 0;
	private static double preRms = 0.0;
	private static double pre2Rms = 0.0;
	ImpulseUtil impulseUtil = new ImpulseUtil();

	public DbImpulseStreamModule() {
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
		DbImpulseStreamModule ism = new DbImpulseStreamModule();
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
		impulseUtil.setUsPerSample((double) sampleRate);
		header = new ImpulseHeader(inHeader.id, inHeader.startTime,
				inHeader.frameTime);
		return header;
	}

	@Override
	public ImpulseFrame process(StreamFrame inFrame) {
		ArrayList<Integer> peakOffsets = new ArrayList<Integer>(1);
		ArrayList<Short> peakMagnitudes = new ArrayList<Short>(1);

		if (!(inFrame instanceof RawAudioFrame))
			throw new RuntimeException("Wrong frame type");

		RawAudioFrame raf = (RawAudioFrame) inFrame;
		double[] slowFrame = raf.smooth(impulseUtil.getSlowWindow());
		double[] fastFrame = raf.smooth(impulseUtil.getFastWindow());
		double minSlowFrame = findMin(slowFrame);
		ImpulseUtil.setMicrophoneRms(minSlowFrame);
		int index_fast = -1;
		index_fast = findPeakIndex(fastFrame, slowFrame, inFrame.seqNum == 0);
		short[] data = raf.getAudioData();
		if (index_fast != -1) {
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

	/**
	 * Find the index of the peak
	 * @param fastFrame
	 * @param slowFrame
	 * @param first
	 * @return index1
	 */
	private int findPeakIndex(double[] fastFrame, double[] slowFrame, boolean first) {
		double[] fsDif = new double[slowFrame.length];
		for (int i = 0; i < slowFrame.length; i++)
			fsDif[i] = fastFrame[i] - slowFrame[i];
		double[] dif = new double[fastFrame.length];
		double max = 0.0;
		//num = 0;
		int position = -1;
		int index1 = -1;
		ArrayList<Integer> index = new ArrayList<Integer>();
		ArrayList<String> flag = new ArrayList<String>();
		// Find the peak candidates based on the relative/absolute value of
		// fastFrame and slowFrame(RMS)
		for (int j = first ? 500 : 0; j < fastFrame.length; j++) {

			if (rmsToDb(slowFrame[j], impulseUtil.getBase()) < 0)
			// When the ambient is too quiet
			{
				if (rmsToDb(fsDif[j], impulseUtil.getQuietImpulseFloor()) > 0) {
					index.add(j);
					flag.add("Quiet");
				}

			} else if (rmsToDb(slowFrame[j], impulseUtil.getNoisyFloor()) > 0)
			// When the ambient is too noisy
			{
				if (rmsToDb(fsDif[j], slowFrame[j] / 2) > 0) {
					index.add(j);
					flag.add("Noise");
				}
			}

			else {
				if (rmsToDb(fsDif[j], slowFrame[j] * impulseUtil.getJerk()) > 0) {
					index.add(j);
					flag.add("Normal");
				}

			}

		}

		// Find the biggest difference between successive fastRMS among the
		// candidate peaks
		for (int i = 0; i < index.size(); i++) {
			if (index.get(i) == 0) {
				dif[index.get(i)] = preRms - pre2Rms;
				if (dif[index.get(i)] > max) {
					max = dif[index.get(i)];
					index1 = index.get(i);
					position = i;
					//num++;
				}
			} else if (index.get(i) == 1) {
				dif[index.get(i)] = fastFrame[index.get(i) - 1] - preRms;
				if (dif[index.get(i)] > max) {
					max = dif[index.get(i)];
					index1 = index.get(i);
					position = i;
					//num++;
				}
			} else {
				dif[index.get(i)] = fastFrame[index.get(i) - 1]
						- fastFrame[index.get(i) - 2];
				if (dif[index.get(i)] > max) {
					max = dif[index.get(i)];
					index1 = index.get(i);
					position = i;
					//num++;
				}
			}
			

		}

		/*
		  if (num > 0) { System.out.println(flag.get(position) +
		  ": FastFrame: " + fastFrame[index.get(position) - 1] + " slowFrame: "
		  + slowFrame[index.get(position) - 1]); }
		 */
		preRms = fastFrame[fastFrame.length - 1];
		pre2Rms = fastFrame[fastFrame.length - 2];
		return index1;
	}

	private int sampleToTimeOffset(int sample) {
		return (int) (sample * impulseUtil.getUsPerSample());
	}

	private int timeToSampleOffset(int time) {
		return (int) ((double) time / impulseUtil.getUsPerSample());
	}

	public void close() {
	}
}