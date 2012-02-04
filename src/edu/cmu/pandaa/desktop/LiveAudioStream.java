package edu.cmu.pandaa.desktop;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.FrameStream;
import edu.cmu.pandaa.stream.RawAudioFileStream;
import edu.cmu.pandaa.utils.DataConversionUtil;

public class LiveAudioStream implements FrameStream {
  ByteArrayOutputStream byteArrayOutputStream;
  public long startTime;
  public long[] usecTime = new long[4];
  CaptureThread captureThread;
  AudioCaptureState audioCaptureState = AudioCaptureState.BEFORE;

  public enum AudioCaptureState {
    BEFORE, RUNNING, STOPPED;
  };

  private final TargetDataLine targetDataLine;
  private final int audioEncoding, bitsPerSample;
  private final int numChannels, samplingRate;
  private final int frameLength;
  private final int captureTimeMs;
  private final String name;
  private int dataSize = -1;
  private RawAudioHeader header;

  private final static int DEFAULT_ENCODING = 1; // PCM
  private final static int DEFAULT_CHANNELS = 1; // MONO
  private final static int DEFAULT_SAMPLING_RATE = 22050;
  private final static int DEFAULT_BITS_PER_SAMPLE = 16;

  public LiveAudioStream(int encoding, int samplingRate, int bitsPerSample, int frameLen,
                         int captureTimeMs, TargetDataLine line, String name) {
    this.audioEncoding = encoding;
    this.samplingRate = samplingRate;
    this.bitsPerSample = bitsPerSample;
    this.frameLength = frameLen;
    this.numChannels = DEFAULT_CHANNELS;
    this.captureTimeMs = captureTimeMs;
    this.targetDataLine = line;
    this.name = name;
  }

  public LiveAudioStream(AudioFormat format, TargetDataLine line, int captureTime, String name) {
    this(DEFAULT_ENCODING, (int) format.getSampleRate(), format.getSampleSizeInBits(),
            format.getFrameSize(), captureTime, line, name);
  }

  public static List<TargetDataLine> findTargetDataLines(AudioFormat audioFormat) {
    Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
    DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
    List<TargetDataLine> lines = new LinkedList<TargetDataLine>();

    for (int cnt = 0; cnt < mixerInfo.length; cnt++) {
      try {
        Mixer mixer = AudioSystem.getMixer(mixerInfo[cnt]);
        TargetDataLine line = (TargetDataLine) mixer.getLine(dataLineInfo);
        line.open(audioFormat);
        lines.add(line);
      } catch (Exception e) {
        // skip this entry
      }
    }
    return lines;
  }

  @Override
  public void setHeader(StreamHeader h) throws Exception {
    throw new RuntimeException("setHeader: Writing to Live Audio Stream is not supported");
  }

  @Override
  public void sendFrame(StreamFrame m) throws Exception {
    throw new RuntimeException("sendFrame: Writing to Live Audio Stream is not supported");
  }

  @Override
  public StreamHeader getHeader() throws Exception {
    String comment = "stime:" + (usecTime[1] - usecTime[0]) + "," + (usecTime[2] - usecTime[1])
            + "," + (usecTime[3] - usecTime[2]);
    startTime = 0;
    header = new RawAudioHeader("DeviceId", startTime, frameLength, audioEncoding, numChannels,
            samplingRate, bitsPerSample, comment);
    return header;
  }

  @Override
  public StreamFrame recvFrame() throws Exception {
    byte[] audioData;

    while (true) {
      synchronized (byteArrayOutputStream) {
        audioData = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.reset();
        if (audioData.length == 0 && isRunning())
          byteArrayOutputStream.wait();
        else
          break;
      }
    }

    if (audioData.length == 0)
      return null;

    RawAudioFrame audioFrame = header.makeFrame();
    audioFrame.audioData = DataConversionUtil.byteArrayToShortArray(audioData);
    return audioFrame;
  }

  @Override
  public void close() throws Exception {
    synchronized (this) {
      stopAudioCapture();
      while (isRunning()) {
        wait();
      }
    }
    if (byteArrayOutputStream != null) {
      synchronized (byteArrayOutputStream) {
        byteArrayOutputStream.close();
      }
    }
    if (targetDataLine != null) {
      targetDataLine.close();
    }
  }

  private void stopAudioCapture() {
    synchronized (byteArrayOutputStream) {
      setStatus(AudioCaptureState.STOPPED);
      byteArrayOutputStream.notify();
    }
  }

  private synchronized void setStatus(AudioCaptureState newState) {
    audioCaptureState = newState;
    notifyAll();
  }

  private synchronized boolean isRunning() {
    return audioCaptureState == AudioCaptureState.RUNNING;
  }

  private void captureAudio() throws Exception {
    dataSize = (int) (frameLength * numChannels * (samplingRate / 1000) * 2);
    byteArrayOutputStream = new ByteArrayOutputStream();
    targetDataLine.start();

    usecTime[0] = System.nanoTime() / 1000;
    captureThread = new CaptureThread(targetDataLine, name);
    for (int i = 1; i < usecTime.length; i++) {
      captureThread.readData();
      usecTime[i] = System.nanoTime() / 1000;
    }
    setStatus(AudioCaptureState.RUNNING);
    captureThread.start();
  }

  // Inner class to capture data from microphone
  class CaptureThread extends Thread {
    byte dataBuffer[] = new byte[dataSize];
    TargetDataLine targetDataLine;

    public CaptureThread(TargetDataLine targetDataLine, String name) {
      super(name);
      this.targetDataLine = targetDataLine;
    }

    public int readData() {
      return targetDataLine.read(dataBuffer, 0, dataBuffer.length);
    }

    @Override
    public void run() {
      long loopStartTime = System.currentTimeMillis();
      try {
        while (isRunning()) {
          if (System.currentTimeMillis() - loopStartTime > captureTimeMs) {
            break;
          }
          int cnt;
          if ((cnt = readData()) > 0) {
            synchronized (byteArrayOutputStream) {
              byteArrayOutputStream.write(dataBuffer, 0, cnt);
              byteArrayOutputStream.notifyAll();
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      stopAudioCapture();
    }
  }

  public static void main(String[] args) throws Exception {
    String hostname = "foobar"; // TODO: make this dynamic
    int arg = 0;
    int captureTimeMs = 100 * 1000;
    int segmentLengthMs = arg < args.length ? new Integer(args[arg++]) : (captureTimeMs / 10);
    AudioFormat audioFormat = new AudioFormat((float) DEFAULT_SAMPLING_RATE,
            DEFAULT_BITS_PER_SAMPLE, DEFAULT_CHANNELS, true, false);
    List<TargetDataLine> lines = findTargetDataLines(audioFormat);
    if (lines.size() == 0) {
      throw new Exception("No valid data lines found");
    }
    int cnt = 0;
    for (TargetDataLine line : lines) {
      String name = hostname + "_" + cnt;
      AudioRunner runner = new AudioRunner(name, captureTimeMs, segmentLengthMs, audioFormat, line);
      cnt++;
      new Thread(runner, name).start();
    }
  }

  static class AudioRunner implements Runnable {
    final String namePrefix;
    final int captureTimeMs;
    final int segmentLengthMs;
    final AudioFormat format;
    final TargetDataLine line;

    private AudioRunner(String namePrefix, int captureTimeMs, int segmentLengthMs,
                        AudioFormat format, TargetDataLine line) {
      this.namePrefix = namePrefix;
      this.captureTimeMs = captureTimeMs;
      this.segmentLengthMs = segmentLengthMs;
      this.format = format;
      this.line = line;
    }

    public void run() {
      try {
        System.err.println("Starting capture loop " + namePrefix);
        int delay = segmentLengthMs - (int) (System.currentTimeMillis() % segmentLengthMs);
        Thread.sleep(delay);
        LiveAudioStream liveAudioStream = new LiveAudioStream(format, line, captureTimeMs, namePrefix);
        liveAudioStream.saveAudio(segmentLengthMs);
        liveAudioStream.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
      System.err.println("Terminating capture loop " + namePrefix);
    }
  }

  private void saveAudio(int segmentLengthMs)
          throws Exception {
    RawAudioFileStream rawAudioOutputStream = null;
    String fileName = name;
    captureAudio();
    StreamFrame frame;
    long loopTime = System.currentTimeMillis();
    do {
      if (rawAudioOutputStream == null) {
        fileName = name + "-" + loopTime + ".wav";
        rawAudioOutputStream = new RawAudioFileStream(fileName, true);
        rawAudioOutputStream.setHeader(getHeader());
        System.out.println("Saving captured audio to " + fileName + " at " + loopTime);
      }

      frame = recvFrame();
      rawAudioOutputStream.sendFrame(frame);

      if (System.currentTimeMillis() - loopTime > segmentLengthMs) {
        System.out.println("Audio stream " + fileName + " complete at " + System.currentTimeMillis());
        rawAudioOutputStream.close();
        rawAudioOutputStream = null;
        loopTime += segmentLengthMs;
      }
    } while (frame != null);
    if (rawAudioOutputStream != null) {
      rawAudioOutputStream.close();
    }
    System.out.println("Audio stream " + fileName + " complete at " + System.currentTimeMillis());
    close();
  }
}
