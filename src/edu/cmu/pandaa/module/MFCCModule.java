package edu.cmu.pandaa.module;

import com.sun.javaws.ui.AutoDownloadPrompt;
import comirva.audio.util.MFCC;
import edu.cmu.pandaa.header.MatrixHeader;
import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.MatrixFileStream;
import edu.cmu.pandaa.stream.RawAudioFileStream;

import java.security.acl.LastOwnerException;
import java.util.UnknownFormatConversionException;

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
  short[][] prevData = new short[1][0];
  int windowSize = 2048; // windowSize must be 2^n

  public StreamHeader init(StreamHeader inHeader) throws Exception {
    RawAudioHeader rah = (RawAudioHeader) inHeader;
    header = new MatrixHeader(inHeader, -1, coeficients);
    mfcc = new MFCC(rah.getSamplingRate(), windowSize, coeficients, true);
    return header;
  }

  public StreamFrame process(StreamFrame inFrame) throws Exception {
    if (inFrame == null) {
      return null;
    }
    RawAudioFrame audioFrame = (RawAudioFrame) inFrame;
    short[] audioShort = audioFrame.getAudioData();

    int newDataLen = audioShort.length;
    int prevLen = 0;
    for (int p = 0;p < prevData.length;p++) {
      prevLen += prevData[p].length;
    }
    int dataLen = newDataLen + prevLen;
    int truncLen = (dataLen % (windowSize / 2));
    int shortLen = dataLen - truncLen;
    double[] audioData = new double[shortLen];
    int c = 0;
    boolean first = true;
    for (int p = 0;p < prevData.length;p++) {
      int i = (p == 0 || prevData[p-1].length == 0) ? truncLen : 0;
      for (; i < prevData[p].length; i++) {
        audioData[c++] = prevData[p][i];
      }
    }
    for (int i = 0; i < newDataLen; i++) {
      if (c < shortLen) {
        audioData[c++] = audioShort[i];
      }
    }

    for (int i = 0; i < prevData.length-1;i++) {
      prevData[i] = prevData[i+1];
    }
    prevData[prevData.length-1] = audioShort;

    // Input requirements for MFCC are that the data is a multiple of windowSize/2
    double[][] mfccOut = mfcc.process(audioData);
    int frames = newDataLen / windowSize * 2;
    if (frames == 0) {
      return header.makeFrame();
    }
    if (frames > mfccOut.length) {
      frames = mfccOut.length;
    }
    double[][] lastFrames = new double[frames][];
    for (int i = 0;i < frames;i++) {
      lastFrames[i] = mfccOut[i + mfccOut.length - frames];
    }
    return header.makeTransposedFrame(lastFrames);
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
