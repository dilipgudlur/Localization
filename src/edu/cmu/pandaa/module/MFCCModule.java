package edu.cmu.pandaa.module;

import comirva.audio.util.MFCC;
import edu.cmu.pandaa.header.MatrixHeader;
import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.MatrixFileStream;
import edu.cmu.pandaa.stream.RawAudioFileStream;

import java.util.LinkedList;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 4/14/12
 * Time: 8:15 AM
 */

public class MFCCModule implements StreamModule {
  MatrixHeader header;
  private final int coeficients = 24;
  MFCC mfcc;
  LinkedList<ShortArray> prevFrames = new LinkedList<ShortArray>();
  private int windowTime = 1000; // target window size, in ms.
  private int windowSize; // size in samples, must be 2^n
  double[] vectorMeans, vectorSqr;
  final double meanWeight = 20.0;

  public StreamHeader init(StreamHeader inHeader) throws Exception {
    RawAudioHeader rah = (RawAudioHeader) inHeader;
    header = new MatrixHeader(inHeader, -1, coeficients);

    double logSize = Math.round(Math.log(windowTime * rah.getSamplingRate() / 1000.0) / Math.log(2));
    windowSize = (int) Math.pow(2, logSize);
    mfcc = new MFCC(rah.getSamplingRate(), windowSize, coeficients, true);
    return header;
  }

  class ShortArray {
    public short[] data;
    public ShortArray(short[] data) {
      this.data = data;
    }
  }

  public StreamFrame process(StreamFrame inFrame) throws Exception {
    if (inFrame == null) {
      return null;
    }
    RawAudioFrame audioFrame = (RawAudioFrame) inFrame;
    short[] audioShort = audioFrame.getAudioData();

    int newDataLen = audioShort.length;
    int prevLen = 0;
    for (int p = 0;p < prevFrames.size();p++) {
      prevLen += prevFrames.get(p).data.length;
    }
    if (prevLen + newDataLen > (windowSize * 3/2)) {
      prevLen -= prevFrames.removeFirst().data.length;
    }
    int dataLen = newDataLen + prevLen;
    int truncLen = (dataLen % (windowSize / 2));
    int shortLen = dataLen - truncLen;
    double[] audioData = new double[shortLen];
    int c = 0;
    for (int p = 0; p < prevFrames.size();p++) {
      short[] prevData = prevFrames.get(p).data;
      int i = truncLen;
      truncLen -= Math.min(prevData.length, truncLen);
      for (; i < prevData.length; i++) {
        audioData[c++] = prevData[i];
      }
    }
    for (int i = 0; i < newDataLen; i++) {
      if (c < shortLen) {
        audioData[c++] = audioShort[i];
      }
    }
    if (c != audioData.length) {
      throw new RuntimeException("Array length does not add up");
    }
    prevFrames.addLast(new ShortArray(audioShort));

    if (shortLen == 0) {
      return header.makeFrame();
    }
    // Input requirements for MFCC are that the data is a multiple of windowSize/2
    double[][] mfccOut = mfcc.process(audioData);
    if (mfccOut.length == 0) {
      return header.makeFrame();
    }
    for (int i = 0;i < mfccOut.length;i++) {
      applyAveraging(mfccOut[i]);
    }
    return header.makeTransposedFrame(mfccOut);
  }

  private void applyAveraging(double[] vector) {
    if (vectorMeans == null) {
      vectorMeans = new double[vector.length];
      vectorSqr = new double[vector.length];
      for (int i = 0; i < vector.length;i ++) {
        vectorMeans[i] = vector[i];
        vectorSqr[i] = vector[i] * vector[i];
      }
    }

    for (int i = 0; i < vector.length;i ++) {
      vectorMeans[i] = (vectorMeans[i] * meanWeight + vector[i])/(meanWeight+1);
      vectorSqr[i] = (vectorSqr[i] * meanWeight + vector[i]*vector[i])/(meanWeight+1);
      vector[i] = (vector[i] - vectorMeans[i]);// / Math.sqrt(vectorSqr[i]);
    }
  }

  public void close() {
    header = null;
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      throw new IllegalArgumentException("usage: * input.wav+ ");
    }
    MFCCModule mfccm = new MFCCModule();
    for (int i = 0;i < args.length;i++) {
      String fname = args[i];
      System.out.println("Processing " + fname);
      RawAudioFileStream rawAudio = new RawAudioFileStream(fname);
      StreamHeader mh = mfccm.init(rawAudio.getHeader());
      MatrixFileStream out = new MatrixFileStream(fname + ".txt", true);
      out.setMultiLine(true);
      out.setHeader(mh);
      while (true) {
        StreamFrame f = mfccm.process(rawAudio.recvFrame());
        if (f == null) {
          break;
        }
        out.sendFrame(f);
      }
    }
    mfccm.close();
  }
}
