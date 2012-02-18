package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.ImpulseHeader.ImpulseFrame;
import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.ImpulseFileStream;
import edu.cmu.pandaa.stream.RawAudioFileStream;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.zip.DeflaterOutputStream;

public class FeatureStreamModule implements StreamModule {
  private double usPerSample;
  private ImpulseHeader header;
  private int prevPeak;
  private double last_diff = 0;
  private int peakWindowSamples;
  private double[] valueArray;
  private double peakValue;
  private LinkedList<Double> peakOffsets = new LinkedList<Double>();
  private LinkedList<Short> peakMagnitudes = new LinkedList<Short>();
  private LinkedList<RawAudioFrame> ras = new LinkedList<RawAudioFrame>();
  private int saveFrames = -1;
  private RawAudioFileStream rafs;
  private String augmentFile;

  /* parameters we may want/need to tweak */
  static int derive = 0;  // non-zero to use 1st derivative
  static int slowWindow = 1024;
  static int fastWindow = 256;
  static int peakWindowMs = 80;  // peakDetection window in Ms
  static long segmentTimeMs = 100 * 1000; // 100 second segments

  public FeatureStreamModule() {
  }

  private ImpulseFrame pushResult(RawAudioFrame in) {
    if (in == null && ras.size() == 0)
      return null;

    if (in != null) {
      in = in.clone();
      ras.addLast(in);
      if (in.seqNum < saveFrames) {
        return null;
      }
    }

    int frameTime = header.frameTime * 1000; // convert from ms to us
    int timeBase = header.nextSeq * frameTime;
    LinkedList<Integer> newOffsets = new LinkedList<Integer>();
    LinkedList<Short> newMagnitudes = new LinkedList<Short>();

    while (peakOffsets.size() > 0 && peakOffsets.get(0) < timeBase+frameTime) {
      double offset = peakOffsets.removeFirst();
      offset -= timeBase;
      newOffsets.addLast((int) offset);
      newMagnitudes.addLast(peakMagnitudes.removeFirst());
    }

    ImpulseFrame impulses = header.makeFrame(newOffsets, newMagnitudes);

    RawAudioFrame audioFrame = ras.removeFirst();
    if (ensureRafs(audioFrame) != null) {
      try {
        augmentAudio(audioFrame, impulses);
        rafs.sendFrame(audioFrame);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    return impulses;
  }

  private RawAudioFileStream ensureRafs(StreamFrame frame) {
    try {
      if (rafs != null) {
        if (rafs.getCurrentLength() >= segmentTimeMs) {
          rafs.close();
          rafs = null;
        }
      }
      if (rafs == null && augmentFile != null) {
        String outFile = String.format(augmentFile, frame.getStartTime());
        rafs = new RawAudioFileStream(outFile, true);
        rafs.setHeader(frame.getHeader());
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
    return rafs;
  }

  private void augmentAudio(RawAudioFrame audioFrame, ImpulseFrame impulses) throws Exception {
    short[] audioData = audioFrame.audioData;
    if (audioData == null)
      return;
    audioData[0] = Short.MIN_VALUE;
    for (int i = 0; i < impulses.peakOffsets.length; i++) {
      int offset = timeToSampleOffset(impulses.peakOffsets[i]);
      if (offset >= audioData.length)
        offset = audioData.length-1;
      if (offset < 0)
        offset = 0;
      audioData[offset] = Short.MAX_VALUE;
    }
  }

  @Override
  public StreamHeader init(StreamHeader inHeader) {
    if (!(inHeader instanceof RawAudioHeader))
      throw new RuntimeException("Wrong header type");
    RawAudioHeader rah = (RawAudioHeader) inHeader;
    rah.initFilters(30, 0);
    int sampleRate = (int) rah.getSamplingRate();
    usPerSample = 1000000.0 / (double) sampleRate; // us per sample
    header = new ImpulseHeader(inHeader.id, inHeader.startTime,
            inHeader.frameTime);

    peakWindowSamples = peakWindowMs * sampleRate / 1000;
    valueArray = new double[peakWindowSamples];
    prevPeak = peakWindowSamples; // supress initial synchronization peak

    saveFrames = (peakWindowMs + inHeader.frameTime - 1)/ inHeader.frameTime;

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
    int mark = offset % peakWindowSamples;
    valueArray[mark] = value;
    double max = 0;
    int max_i = -1;
    for (int i = 0;i < peakWindowSamples;i++) {
      if (valueArray[i] > max) {
        max_i = i;
        max = valueArray[i];
      }
    }

    if (max_i > mark)
      max_i -= peakWindowSamples;
    max_i = offset + max_i - mark;
    peakValue = max;
    return max_i;
  }

  @Override
  public ImpulseFrame process(StreamFrame inFrame) {
    if (inFrame == null) {
      return pushResult(null);
    }

    if (!(inFrame instanceof RawAudioFrame))
      throw new RuntimeException("Wrong frame type");
    RawAudioFrame raf = (RawAudioFrame) inFrame;

    double[] slowFrame = raf.smooth(slowWindow);
    double[] fastFrame = raf.smooth(fastWindow);
    short[] data = raf.getAudioData();
    int frameSampleStart = raf.seqNum * data.length;

    for (int i = 0; i < data.length;i ++) {
      double slow = slowFrame[i];
      double fast = fastFrame[i];

      double value = audioMap(fast, slow);

      int offset = frameSampleStart + i;
      int pi = findPeak(offset, value);

      if (pi > prevPeak && peakValue > 0) {
        if (pi == (offset - peakWindowSamples/2)) {
          peakOffsets.addLast(sampleToTimeOffset(pi));
          peakMagnitudes.addLast((short) peakValue);
          prevPeak = pi;
        }
      }

      if (rafs != null)
        data[i] = (short) value;
    }

    return pushResult(raf);
  }

  public double sampleToTimeOffset(int sample) {
    return sample * usPerSample;
  }

  public int timeToSampleOffset(double time) {
    return (int) (time / usPerSample);
  }

  public void augmentedAudio(String outFile) throws Exception {
    if (header != null) {
      throw new Exception("Need to call augmentAudio before init");
    }
    augmentFile = outFile;
  }

  public void close() {
    if (rafs != null) {
      rafs.close();
    }
  }

  public static void main(String[] args) throws Exception {
    int arg = 0, dint = 0, length = 0;
    String outFilename = args[arg++];
    String inFilename = args[arg++];
    String refFilename = null;
    if (arg < args.length) {
      length = Integer.parseInt(args[arg++]);
      refFilename = args[arg++];
    }
    if (args.length != arg) {
      throw new IllegalArgumentException("Extranious arguments");
    }

    //for (slowWindow = 256; slowWindow < 2000; slowWindow *= 2)
    // for (fastWindow = slowWindow/8; fastWindow < slowWindow; fastWindow *= 2)
    //for (derive = 0; derive < 2; derive++)
    //for (peakWindowMs = 20; peakWindowMs < 100; peakWindowMs *= 2)
    {
      RawAudioFileStream rfs = new RawAudioFileStream(inFilename, refFilename, length);
      ImpulseFileStream iout = new ImpulseFileStream(outFilename, true);
      FeatureStreamModule ism = new FeatureStreamModule();

      int dotIndex = inFilename.lastIndexOf('.');
      String outFile = inFilename.substring(0, dotIndex);
      dotIndex = outFile.lastIndexOf(File.separator);
      if (dotIndex >= 0)
        outFile = outFile.substring(dotIndex+1);
      int otherIndex = outFilename.lastIndexOf(File.separator);
      if (otherIndex >= 0)
        outFile = outFilename.substring(0, otherIndex+1) + outFile;

      //outFile += derive > 0 ? "_d" : "_i";
      //outFile = outFile + "_" + slowWindow + "-" + fastWindow;
      //outFile = outFile + "_" + peakWindowMs;
      outFile = outFile + "-impulse.wav";
      ism.augmentedAudio(outFile);

      System.out.println("FeatureStream: " + outFilename + " " + inFilename + " for " + length + " (" + rfs.loopCount + ")");

      RawAudioHeader header = (RawAudioHeader) rfs.getHeader();
      ImpulseHeader iHeader = (ImpulseHeader) ism.init(header);

      if (iout != null)
        iout.setHeader(iHeader);

      RawAudioFrame audioFrame;
      ImpulseFrame impulses = null;
      while (true) {
        audioFrame = (RawAudioFrame) rfs.recvFrame();

        // TODO: make this automaticaly stop generating the augmented frames somehow.
        impulses = ism.process(audioFrame);

        if (impulses == null && audioFrame == null)
          break;

        if (iout != null && impulses != null)
          iout.sendFrame(impulses);
      }

      ism.close();
      if (iout != null)
        iout.close();
    }
  }
}