package edu.cmu.pandaa.desktop;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.util.*;

import javax.sound.sampled.*;

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
  final static int syncFrames = 10;
  static final Map<TargetDataLine, Mixer> lines = new HashMap<TargetDataLine, Mixer>();
  static int lineCount = 0;
  final static int delayWindowMs = 10 * 1000;

  public enum AudioCaptureState {
    BEFORE, PREFETCH, RUNNING, STOPPED;
  };

  private final TargetDataLine targetDataLine;
  private final int audioEncoding, bitsPerSample;
  private final int numChannels, samplingRate;
  private final int frameTime;
  private final int captureTimeMs;
  private final String fileName;
  private int dataSize = -1;
  private RawAudioHeader header;

  private final static int DEFAULT_ENCODING = 1; // PCM
  private final static int DEFAULT_CHANNELS = 1; // MONO
  private final static int DEFAULT_SAMPLING_RATE = 44100;
  private final static int DEFAULT_FRAME_TIME = 125; // 125ms per frame
  private final static int DEFAULT_BITS_PER_SAMPLE = 16;

  public LiveAudioStream(int encoding, int samplingRate, int bitsPerSample, int frameTime,
                         int captureTimeMs, TargetDataLine line, String fileName) {
    this.audioEncoding = encoding;
    this.samplingRate = samplingRate;
    this.bitsPerSample = bitsPerSample;
    this.frameTime = frameTime;
    this.numChannels = DEFAULT_CHANNELS;
    this.captureTimeMs = captureTimeMs;
    this.targetDataLine = line;
    this.fileName = fileName;
  }

  public LiveAudioStream(TargetDataLine line, int captureTime, String name) {
    this(DEFAULT_ENCODING, (int) line.getFormat().getSampleRate(), line.getFormat().getSampleSizeInBits(),
            DEFAULT_FRAME_TIME, captureTime, line, name);
  }

  public static void findTargetDataLines(AudioFormat audioFormat) {
    Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
    DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

    for (int cnt = 0; cnt < mixerInfo.length; cnt++) {
      boolean added = false;
      Mixer mixer = AudioSystem.getMixer(mixerInfo[cnt]);
      String name = mixer.getMixerInfo().getName();
      String desc = mixer.getMixerInfo().getDescription();
      if (desc.contains("Direct Audio Device")) {
        try {
          TargetDataLine line = (TargetDataLine) mixer.getLine(dataLineInfo);
          line.open(audioFormat);
          lines.put(line, mixer);
          added = true;
        } catch (Exception e) {
          // skip this entry
        }
      }
      System.out.println((added ? "*" : " ") + "Audio mixer " + name + ": " + desc);
    }
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
    if (header == null) {
      String comment = "stime:" + (usecTime[1] - usecTime[0]) + "," + (usecTime[2] - usecTime[1])
              + "," + (usecTime[3] - usecTime[2]);
      startTime = 0;
      header = new RawAudioHeader(fileName, startTime, frameTime, audioEncoding, numChannels,
              samplingRate, bitsPerSample, comment);
    }
    return header;
  }

  @Override
  public StreamFrame recvFrame() throws Exception {
    byte[] audioData;
    synchronized (byteArrayOutputStream) {
      while (byteArrayOutputStream.size() == 0) {
        if (isRunning())
          byteArrayOutputStream.wait();
        else
          return null;
      }
      audioData = byteArrayOutputStream.toByteArray();
      byteArrayOutputStream.reset();
      if (audioData.length < dataSize) {
        throw new Exception("Bad data read length: " + audioData.length);
      }
      if (audioData.length > dataSize) {
        byte[] nData = new byte[dataSize];
        System.arraycopy(audioData, 0, nData, 0, dataSize);
        byteArrayOutputStream.write(audioData, dataSize, audioData.length - dataSize);
        audioData = nData;
      }
    }

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
      setState(AudioCaptureState.STOPPED);
      byteArrayOutputStream.notify();
    }
  }

  private synchronized void setState(AudioCaptureState newState) {
    audioCaptureState = newState;
    notifyAll();
  }

  private synchronized boolean isRunning() {
    return audioCaptureState == AudioCaptureState.RUNNING ||
            audioCaptureState == AudioCaptureState.PREFETCH;
  }

  private void startCaptureThread() throws Exception {
    System.err.println(System.currentTimeMillis() + " Started data line " + fileName);

    dataSize = (int) (frameTime * numChannels * samplingRate / 1000 * 2);
    byteArrayOutputStream = new ByteArrayOutputStream();

    usecTime[0] = System.nanoTime() / 1000;
    captureThread = new CaptureThread(targetDataLine, fileName);
    setState(AudioCaptureState.PREFETCH);
    captureThread.start();
  }

  class CaptureThread extends Thread {
    byte dataBuffer[] = new byte[dataSize];
    TargetDataLine targetDataLine;

    public CaptureThread(TargetDataLine targetDataLine, String name) {
      super(name);
      this.targetDataLine = targetDataLine;
    }

    private int readData() {
      return targetDataLine.read(dataBuffer, 0, dataBuffer.length);
    }

    @Override
    public void run() {
      try {
        long loopStartTime = System.currentTimeMillis();
        while (isRunning()) {
          if (captureTimeMs >= 0 && System.currentTimeMillis() - loopStartTime > captureTimeMs) {
            break;
          }
          int cnt = readData();
          if (cnt < 0) {
            break;
          }
          synchronized (byteArrayOutputStream) {
            if (audioCaptureState != AudioCaptureState.PREFETCH && byteArrayOutputStream.size() < dataSize*100) {
              byteArrayOutputStream.write(dataBuffer, 0, cnt);
              byteArrayOutputStream.notifyAll();
            }
          }
          if (targetDataLine.available() > 0) {
            Thread.sleep(frameTime/2);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      stopAudioCapture();
    }
  }

  private static String getHostName() throws Exception {
    InetAddress localMachine = java.net.InetAddress.getLocalHost();
    return localMachine.getHostName();
  }

  public static void main(String[] args) throws Exception {
    String hostname = getHostName().replace('-', '+').replace('_', '+');
    int arg = 0;
    int captureTimeMs = (arg < args.length ? new Integer(args[arg++]) : 100) * 1000;
    int segmentLengthMs = (arg < args.length ? new Integer(args[arg++]) : 10) * 1000;
    System.err.println("Starting audio capture for " + captureTimeMs/1000.0 + "s in "+segmentLengthMs/1000.0+"s segments");
    AudioFormat audioFormat = new AudioFormat((float) DEFAULT_SAMPLING_RATE,
            DEFAULT_BITS_PER_SAMPLE, DEFAULT_CHANNELS, true, false);
    findTargetDataLines(audioFormat);
    if (lines.size() == 0) {
      throw new Exception("No valid data lines found");
    }
    int cnt = 1;
    for (TargetDataLine line : lines.keySet()) {
      String fileName = hostname + "_%d-" + cnt;
      AudioRunner runner = new AudioRunner(fileName, captureTimeMs, segmentLengthMs, line);
      cnt++;
      new Thread(runner, fileName).start();
    }
  }

  public static List<FrameStream> getLiveAudioStreams() throws Exception {
    String hostname = getHostName().replace('-', '+').replace('_','+');
    AudioFormat audioFormat = new AudioFormat((float) DEFAULT_SAMPLING_RATE,
            DEFAULT_BITS_PER_SAMPLE, DEFAULT_CHANNELS, true, false);
    findTargetDataLines(audioFormat);
    if (lines.size() == 0) {
      throw new Exception("No valid data lines found");
    }
    int cnt = 1;
    List<FrameStream> streams = new ArrayList<FrameStream>();
    for (TargetDataLine line : lines.keySet()) {
      String fileName = hostname + "-" + cnt;
      LiveAudioStream stream = new LiveAudioStream(line, -1, fileName);
      streams.add(stream);
      stream.startLiveCapture();
      cnt++;
    }
    return streams;
  }

  static class AudioRunner implements Runnable {
    final String fileName;
    final int captureTimeMs;
    final int segmentLengthMs;
    final TargetDataLine line;

    private AudioRunner(String fileName, int captureTimeMs, int segmentLengthMs, TargetDataLine line) {
      this.fileName = fileName;
      this.captureTimeMs = captureTimeMs;
      this.segmentLengthMs = segmentLengthMs;
      this.line = line;
    }

    public void run() {
      try {
        LiveAudioStream liveAudioStream = new LiveAudioStream(line, captureTimeMs, fileName);
        System.err.println(System.currentTimeMillis() + " Starting capture thread for " + fileName);
        liveAudioStream.startSaveAudio(segmentLengthMs);
        liveAudioStream.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
      System.err.println("Terminating capture loop " + fileName);
    }
  }

  private static void readySetGo(TargetDataLine targetDataLine) throws InterruptedException {
    synchronized(lines) {
      lineCount++;
      if (lineCount == lines.size()) {
        lines.notifyAll();
      } else while (lineCount < lines.size()) {
        lines.wait();
      }
      targetDataLine.start();

      // this flush is necessary on some versions of the JDK that seem to start capture before start()!
      targetDataLine.flush();
    }
    Thread.sleep(1); // let other threads start their lines
    synchronized(lines) {
      lineCount--;
      while (lineCount > 0) {
        lines.wait();
      }
      lines.notifyAll();
    }
  }

  private void startLiveCapture() throws Exception {
    System.err.println("Starting live capture of audio from " + lines.get(targetDataLine).getMixerInfo().getName());
    startCaptureThread();
    targetDataLine.start();
    setState(AudioCaptureState.RUNNING);
  }

  private void startSaveAudio(int segmentLengthMs)
          throws Exception {
    RawAudioFileStream rawAudioOutputStream = null;
    String segmentName = fileName;
    try {
      startCaptureThread();

      int delay = delayWindowMs - (int) (System.currentTimeMillis() % delayWindowMs);
      if (delay < frameTime*2)
        delay += delayWindowMs;
      System.err.println(System.currentTimeMillis() + " Delaying start for " + delay);
      Thread.sleep(delay);

      readySetGo(targetDataLine);
      setState(AudioCaptureState.RUNNING);
      long loopTime = System.currentTimeMillis();

      System.err.println(System.currentTimeMillis() + " " + fileName + " has available " +
              targetDataLine.available());

      StreamFrame frame;

      int frameCount = 0;
      int captured = 0;

      do {
        if (rawAudioOutputStream == null) {
          segmentName = String.format(fileName, loopTime) + ".wav";
          rawAudioOutputStream = new RawAudioFileStream(segmentName, true);
          rawAudioOutputStream.setHeader(getHeader());
          System.err.println(System.currentTimeMillis() + " Saving captured audio to " + segmentName);
          frameCount = segmentLengthMs / frameTime;
          captured = 0;
          long phase = System.currentTimeMillis() - loopTime;
          if (phase > frameTime*2 || phase < 0) {
            System.err.println(System.currentTimeMillis() + " Excessive frame drift detected: " + phase);
          }
        }
        frame = recvFrame();
        rawAudioOutputStream.sendFrame(frame);
        captured++;

        if (captured == frameCount) {
          System.err.println(System.currentTimeMillis() + " Audio stream complete for " + segmentName);
          rawAudioOutputStream.close();
          rawAudioOutputStream = null;
          loopTime += captured * frameTime;
        }
      } while (frame != null);
    } finally {
      if (rawAudioOutputStream != null) {
        rawAudioOutputStream.close();
      }
      System.err.println("Audio stream " + segmentName + " complete at " + System.currentTimeMillis());
      close();
    }
  }
}
