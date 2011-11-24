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

public class FeatureStreamModule implements StreamModule {
  private double usPerSample;
  private final int slowWindow = 1000;
  private final int fastWindow = 50;
  private final double jerk = 4;
  private final double base = 50;
  private ImpulseHeader header;
  boolean prevPeak = true; // start with peak supression turned on

  public FeatureStreamModule() {
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
    FeatureStreamModule ism = new FeatureStreamModule();
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
    //for (int i = 0; i < audio.audioData.length; i++) {
    //  audio.audioData[i] = (short) (audio.audioData[i] / 2);
    //}
    for (int i = 0; i < impulses.peakOffsets.length; i++) {
      audio.audioData[timeToSampleOffset(impulses.peakOffsets[i])+2] = Short.MAX_VALUE;
    }
    // audio.audioData[0] = Short.MAX_VALUE;
  }

  @Override
  public StreamHeader init(StreamHeader inHeader) {
    if (!(inHeader instanceof RawAudioHeader))
      throw new RuntimeException("Wrong header type");
    RawAudioHeader rah = (RawAudioHeader) inHeader;
    rah.initFilters(30, 0);
    int sampleRate = (int) rah.getSamplingRate();
    usPerSample = Math.pow(10,6) / (double) sampleRate; // us per sample
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
    short[] data = raf.getAudioData();

    for (int i = 0; i < data.length;i ++) {
      double slow = slowFrame[i];
      double fast = fastFrame[i];

      if (fast > (slow*jerk + base)) {
        if (!prevPeak) {
         // System.out.println("" + fast + " " + slow);
          peakOffsets.add(sampleToTimeOffset(i));
          peakMagnitudes.add((short) fast);
        }
        prevPeak = true;
      } else {
        prevPeak = false;
      }

      double tmp = (i%2 == 0) ? -fast : slow;
      data[i] = (short) tmp;
    }

    return header.new ImpulseFrame(peakOffsets, peakMagnitudes);
  }

  private double[] short2double(short[] frame) {
    double[] frameD = new double[frame.length];
    for (int i = 0; i < frame.length; i++) {
      frameD[i] = java.lang.Math.abs((double) frame[i]) / 32768.0;
    }
    return frameD;
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