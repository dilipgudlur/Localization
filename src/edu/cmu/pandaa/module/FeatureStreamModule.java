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
import java.util.LinkedList;

public class FeatureStreamModule implements StreamModule {
  private double usPerSample;
  private ImpulseHeader header;
  private int prevPeak = 0; // start with peak supression turned on -- peak at index 0
  private double last_diff = 0;
  private int peakWindow_use;
  private double[] valueArray;
  private double peakValue;
  private LinkedList<RawAudioFrame> prevFrames = new LinkedList<RawAudioFrame>();
  private int saveFrames = 2;

  /* parameters we may want/need to tweak */
  static boolean annotate = true;
  static int derive = 0;  // non-zero to use 1st derivative
  static int slowWindow = 1024;
  static int fastWindow = 256;
  static int peakWindowMs = 20;  // peakDetection window in Ms

  public FeatureStreamModule() {
  }

  private void augmentAudio(RawAudioFrame audio, ImpulseFrame impulses, RawAudioFileStream rfso) throws Exception {
    //for (int i = 0; i < audio.audioData.length; i++) {
    //  audio.audioData[i] = (short) (audio.audioData[i] / 2);
    //}
    if (audio == null) {
      while (prevFrames.size() > 0)
        rfso.sendFrame(prevFrames.removeLast());
      return;
    }

    int dataSize = audio.audioData.length;
    prevFrames.addFirst(audio);

    for (int i = 0; i < impulses.peakOffsets.length; i++) {
      int offset = timeToSampleOffset(impulses.peakOffsets[i]);
      // TODO: get the right frame here, depending on the index
      int getFrame = (dataSize-offset)/dataSize;
      offset += getFrame * dataSize;
      if (getFrame >= prevFrames.size() && getFrame < saveFrames)
        continue;
      prevFrames.get(getFrame).audioData[offset] = Short.MAX_VALUE;
    }

    if (prevFrames.size() > saveFrames)
      rfso.sendFrame(prevFrames.removeLast());
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

    peakWindow_use = peakWindowMs * sampleRate / 1000;
    valueArray = new double[peakWindow_use];

    return header;
  }

  private double audioMap(double fast, double slow) {
    double diff = (fast - slow);
    if (diff > 0)
      diff = Math.log(diff)*2000;
    if (derive > 0) {
      double tmp = diff;
      diff = (diff - last_diff)*2.0;
      last_diff = tmp;
    }
    return diff;
  }

  private int findPeak(int offset, double value) {
    int mark = offset % peakWindow_use;
    valueArray[mark] = value;
    double max = 0;
    int max_i = -1;
    for (int i = 0;i < peakWindow_use;i++) {
      if (valueArray[i] > max) {
        max_i = i;
        max = valueArray[i];
      }
    }

    if (max_i > mark)
      max_i -= peakWindow_use;
    max_i = offset - mark + max_i;
    peakValue = max;
    return max_i;
  }

  @Override
  public ImpulseFrame process(StreamFrame inFrame) {
    ArrayList<Integer> peakOffsets = new ArrayList<Integer>(1);
    ArrayList<Short> peakMagnitudes = new ArrayList<Short>(1);

    if (!(inFrame instanceof RawAudioFrame))
      throw new RuntimeException("Wrong frame type");

    RawAudioFrame raf = (RawAudioFrame) inFrame;
    double[] slowFrame = raf.smooth(slowWindow);
    double[] fastFrame = raf.smooth(fastWindow);
    short[] data = raf.getAudioData();
    int windowOffset = raf.seqNum * data.length;

    for (int i = 0; i < data.length;i ++) {
      double slow = slowFrame[i];
      double fast = fastFrame[i];

      double value = audioMap(fast, slow);

      int offset = windowOffset + i;
      int pi = findPeak(offset, value) - windowOffset;

      if (pi != prevPeak && peakValue > 0
              && (i - pi) == peakWindow_use/2) {
        peakOffsets.add(sampleToTimeOffset(pi));
        peakMagnitudes.add((short) peakValue);
        prevPeak = pi;
      }

      data[i] = (short) value;
    }

    return header.new ImpulseFrame(peakOffsets, peakMagnitudes);
  }

  private int sampleToTimeOffset(int sample) {
    return (int) (sample * usPerSample);
  }

  private int timeToSampleOffset(int time) {
    return (int) ((double) time / usPerSample);
  }

  public void close() {
  }

  public static void main(String[] args) throws Exception {
    int arg = 0, dint = 0;
    String impulseFilename = args[arg++];

    for (int inIndex = arg; inIndex < args.length; inIndex++) {
      String audioFilename = args[inIndex];
      //for (slowWindow = 256; slowWindow < 2000; slowWindow *= 2)
      // for (fastWindow = slowWindow/8; fastWindow < slowWindow; fastWindow *= 2)
      //for (derive = 0; derive < 2; derive++)
      //for (peakWindowMs = 20; peakWindowMs < 100; peakWindowMs *= 2)
      {
        RawAudioFileStream rfs = new RawAudioFileStream(audioFilename);
        ImpulseFileStream foo = null;
        if (inIndex == arg) {
          foo = new ImpulseFileStream(impulseFilename, true);
        }
        FeatureStreamModule ism = new FeatureStreamModule();
        int dotIndex = audioFilename.lastIndexOf('.');
        String outFile = audioFilename.substring(0, dotIndex);
        if (derive > 0)
          outFile = outFile + "_d";
        else
          outFile = outFile + "_i";
        //outFile = outFile + "_" + slowWindow + "-" + fastWindow;
        outFile = outFile + "_" + peakWindowMs;
        outFile = outFile + ".wav";
        RawAudioFileStream rfso = new RawAudioFileStream(outFile, true);

        System.out.println("FeatureStream: " + impulseFilename + " " + audioFilename + " " + outFile);

        RawAudioHeader header = (RawAudioHeader) rfs.getHeader();
        rfso.setHeader(header);
        ImpulseHeader iHeader = (ImpulseHeader) ism.init(header);
        if (foo != null)
          foo.setHeader(iHeader);

        RawAudioFrame audioFrame;
        while ((audioFrame = (RawAudioFrame) rfs.recvFrame()) != null) {
          ImpulseFrame streamFrame = ism.process(audioFrame);
          if (foo != null)
            foo.sendFrame(streamFrame);

          if (annotate)
            ism.augmentAudio(audioFrame, streamFrame, rfso);
          else
            rfso.sendFrame(audioFrame);
        }

        if (annotate)
          ism.augmentAudio(null, null, rfso);

        rfso.close();
        if (foo != null)
          foo.close();
      }
    }
  }
}