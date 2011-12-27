package edu.cmu.pandaa.module;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import edu.cmu.pandaa.header.ImpulseHeader;
import edu.cmu.pandaa.header.ImpulseHeader.ImpulseFrame;
import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.RawAudioFileStream;

public class AudioSynchronizationModule implements StreamModule {
  private final static int tolerance = Short.MAX_VALUE/100;
  private boolean firstPeakImpulseFound;
  RawAudioHeader rawAudioHeader;
  private FeatureStreamModule feature;
  private double threshold = 0;
  private double deviance = 0;
  int tcount = 0;
  int averageNum = 10;

  @Override
  public StreamHeader init(StreamHeader inHeader) {
    if (!(inHeader instanceof RawAudioHeader))
      throw new RuntimeException("Wrong header type");
    firstPeakImpulseFound = false;
    rawAudioHeader = (RawAudioHeader) inHeader;
    feature = new FeatureStreamModule();
    feature.init(inHeader);
    return rawAudioHeader;
  }

  private void augmentedAudio(String fname) throws Exception {
    feature.augmentedAudio(fname);
    feature.rafs.setHeader(rawAudioHeader);
  }

  @Override
  public StreamFrame process(StreamFrame inFrame) {
    if (inFrame == null)
      return null;
    if (!(inFrame instanceof RawAudioFrame))
      throw new RuntimeException("Wrong frame type");
    RawAudioFrame audioFrame = (RawAudioFrame) inFrame;
    if (!firstPeakImpulseFound) {
      short[] audioData = audioFrame.audioData.clone();
      int peakIndex = -1;
      ImpulseFrame features = feature.process(inFrame);
      if (features != null && features.peakMagnitudes.length > 0) {
        peakIndex = feature.timeToSampleOffset(features.peakOffsets[0]);
      }

      if (peakIndex == -1) {
        audioData = null;
      } else {
        firstPeakImpulseFound = true;
        short[] nAudioData = new short[audioData.length - peakIndex];
        // TODO: Use array copy
        for (int i = 0; i < (audioData.length - peakIndex); i++)
          nAudioData[i] = audioData[i + peakIndex];
        audioData = nAudioData;
      }
      audioFrame.audioData = audioData;
    }
    return audioFrame;
  }

  @Override
  public void close() {
    feature.close();
  }

  /*
    * Arguments: 1- File Directory location 2- Input file prefix 3- Output file
    * prefix
    */
  public static void main(String args[]) {
    int arg = 0;
    String outFileName = args[arg++];
    String inFileName = args[arg++];
    if (args.length != arg)
      throw new RuntimeException("Invalid number of arguments: <outfile> <infile");

    RawAudioFileStream inFile, outFile;
    AudioSynchronizationModule syncModule;
    try {
      inFile = new RawAudioFileStream(inFileName);
      outFile = new RawAudioFileStream(outFileName, true);
      System.out.println("AudioSynchronization: " + outFileName + " from " + inFileName);
      syncModule = new AudioSynchronizationModule();
      outFile.setHeader(syncModule.init(inFile.getHeader()));
      syncModule.augmentedAudio(outFileName + "-sync.wav");
      RawAudioFrame frame = null;
      while ((frame = (RawAudioFrame) syncModule.process(inFile.recvFrame())) != null) {
        outFile.sendFrame(frame);
      }
      inFile.close();
      outFile.close();
      syncModule.close();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
