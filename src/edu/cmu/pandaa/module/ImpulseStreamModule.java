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
import java.lang.Math;

public class ImpulseStreamModule implements StreamModule {
	private double max = 0.2;
	private double std = 0.0;
	private int sampleRate = 16000;
	int nsPerSample = 10 ^ 9 / sampleRate; // nanosecond per sample
	private double threshold = max / 2; // threshold for amplitude

	int timeFrame = 100; // ms
	private ImpulseHeader header;

	public ImpulseStreamModule() {
	}

	public static void main(String[] args) throws Exception {

		int arg = 0;
		String impulseFilename = args[arg++];
		String audioFilename = args[arg++];
		if (args.length > arg || args.length < arg) {
			throw new IllegalArgumentException("Invalid number of arguments");
		}

		System.out.println("Extract impulses: " + audioFilename + " to "
				+ impulseFilename);

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
	public ImpulseFrame process(StreamFrame inFrame) {
		if (!(inFrame instanceof RawAudioFrame))
			throw new RuntimeException("Wrong frame type");
		int index = 0;
		short[] frame = ((RawAudioFrame) inFrame).getAudioData();
		int peakNum = maxHeight(frame, 0, frame.length);
		short[] peakMagnitudes = new short[peakNum];
		int[] peakOffsets = new int[peakNum];
		if (peakNum > 0) {
			for (int i = 0; i < frame.length; i++) {
				double value = java.lang.Math.abs((double) frame[i]) / 32768.0;

				if (index < peakNum && value > threshold && isPeak(frame, i)) {

					peakMagnitudes[index] = frame[i];
					peakOffsets[index] = sampleToTimeOffset(i);
					index++;
				}
			}
		}

		ImpulseFrame impulseFrame = header.new ImpulseFrame(peakOffsets,
				peakMagnitudes);
		return impulseFrame;
	}

	private int sampleToTimeOffset(int sample) {
		return sample * nsPerSample;
	}

	private int timeToSampleOffset(int time) {
		return time / nsPerSample;
	}

	private int maxHeight(short[] frame, int start_index, long frameSample) {
		threshold = 0.1;
		double max = 0.0;
		int peakNum;
		boolean flag = false;
		while (true) {
			int i = 0;
			peakNum = 0;
			while (i < frameSample) {
				double value = java.lang.Math.abs((double) frame[start_index
						+ i]) / 32768.0;
				if (value > max)
					max = value;
				if (value >= threshold && isPeak(frame, start_index + i)
				// TODO: && std(frame, start_index + i)
				) {
					peakNum++;
					flag = true;
				}
				i++;
			}
			if (flag == true && peakNum == 0) {
				threshold = threshold / 1.001;
				peakNum = 10;
				break;
			}
			if (peakNum <= 10)
				break;
			else
				setThreshold(threshold * 1.001);
		}
		return peakNum;

	}

	private boolean std(short[] frame, int i) {
		if (i > 1 && i < frame.length - 1) {
			if (computeStd(frame, i, 5) > std)
				return true;
			else
				return false;
		}
		return false;
	}

	private double computeStd(short[] frame, int i, int len) {
		/* compute mean */
		double sum = 0;
		int j = 0;
		for (j = i - 2; j < i + 3; j++) {
			sum += frame[j];
		}
		double mean = sum / len;

		/* compute std */
		j = 0;
		sum = 0;
		for (j = i - 2; j < i + 3; j++) {
			sum += java.lang.Math.exp(frame[j] - mean);
		}
		double std = java.lang.Math.sqrt(sum / len);
		return std;
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
		max = 0.2;
		threshold = max / 2;
	}

}