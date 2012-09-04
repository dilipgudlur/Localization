package edu.cmu.pandaa.desktop;

import java.awt.image.ImagingOpException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
  CaptureThread captureThread;
  AudioCaptureState audioCaptureState = AudioCaptureState.BEFORE;
  final static int syncFrames = 10;
  static final SortedMap<Mixer, TargetDataLine> lines = new TreeMap<Mixer, TargetDataLine>(new MixerComparator());
  static int lineCount = 0;
  final static int delayWindowMs = 10 * 1000;
  private RawAudioFileStream rawAudioOutputStream;

  public enum AudioCaptureState {
    BEFORE, PREFETCH, RUNNING, STOPPED;
  };

  private final TargetDataLine targetDataLine;
  private final int audioEncoding, bitsPerSample;
  private final int numChannels, samplingRate;
  private final int frameTime;
  private final int captureTimeMs;
  private final String fileName;
  private final String id;
  private int dataSize = -1;
  private RawAudioHeader header;
  private int framesDesired, framesCaptured;
  private long loopTime;
  private final int segmentLengthMs;

  private final static int DEFAULT_ENCODING = 1; // PCM
  private final static int DEFAULT_CHANNELS = 1; // MONO
  private final static int DEFAULT_SAMPLING_RATE = 44100;
  private final static int DEFAULT_FRAME_TIME = 100; // 100ms per frame
  private final static int DEFAULT_BITS_PER_SAMPLE = 16;

  static class MixerComparator implements Comparator<Mixer> {
    public int compare(Mixer a, Mixer b) {
      return a.getMixerInfo().getName().compareTo(b.getMixerInfo().getName());
    }
  }

  private LiveAudioStream(String id, int encoding, int samplingRate, int bitsPerSample, int frameTime,
                          int captureTimeMs, int segmentLengthMs, TargetDataLine line, String fileName) {
    this.id = id;
    this.audioEncoding = encoding;
    this.samplingRate = samplingRate;
    this.bitsPerSample = bitsPerSample;
    this.frameTime = frameTime;
    this.numChannels = DEFAULT_CHANNELS;
    this.captureTimeMs = captureTimeMs;
    this.segmentLengthMs = segmentLengthMs;
    this.targetDataLine = line;
    this.fileName = fileName;
  }

  public LiveAudioStream(String id, TargetDataLine line, int captureTime, int segmentLengthMs, String fileName) {
    this(id, DEFAULT_ENCODING, (int) line.getFormat().getSampleRate(), line.getFormat().getSampleSizeInBits(),
            DEFAULT_FRAME_TIME, captureTime, segmentLengthMs, line, fileName);
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
          lines.put(mixer, line);
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
      startCapturing();
    }
    return header;
  }

  @Override
  public StreamFrame recvFrame() throws Exception {
    if (rawAudioOutputStream == null) {
      StreamHeader header = getHeader();
      loopTime = header.getNextFrameTime();
      String segmentFile = String.format(fileName, loopTime);
      rawAudioOutputStream = new RawAudioFileStream(segmentFile, true);
      rawAudioOutputStream.setHeader(header);
      System.out.println(System.currentTimeMillis() + " Saving captured audio to: " + segmentFile);
      framesDesired = segmentLengthMs / frameTime;
      framesCaptured = 0;
      long phase = System.currentTimeMillis() - loopTime;
      if (phase > frameTime*2 || phase < 0) {
        System.out.println(System.currentTimeMillis() + " Excessive frame drift detected: " + phase);
      }
    }

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

    if (rawAudioOutputStream != null) {
      rawAudioOutputStream.sendFrame(audioFrame);
      framesCaptured++;

      if (framesCaptured == framesDesired) {
        System.out.println(System.currentTimeMillis() + " Audio stream complete for " + id);
        rawAudioOutputStream.close();
        rawAudioOutputStream = null;
        loopTime += framesCaptured * frameTime;
      }
    }

    return audioFrame;
  }

  @Override
  public void close() {
    synchronized (this) {
      stopAudioCapture();
      while (isRunning()) {
        try {
          wait();
        } catch (InterruptedException e) {
          // ignore interruption
        }
      }
    }
    if (byteArrayOutputStream != null) {
      synchronized (byteArrayOutputStream) {
        try {
          byteArrayOutputStream.close();
        } catch (IOException e) {
          // ignore closing exception
        }
      }
    }
    if (targetDataLine != null) {
      targetDataLine.close();
    }
    if (rawAudioOutputStream != null) {
      rawAudioOutputStream.close();
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

  private synchronized void waitActive() {
    try {
      while (audioCaptureState == AudioCaptureState.PREFETCH) {
        wait();
      }
    } catch (InterruptedException e) {
      //
    }
  }

  private void startCapturing() throws Exception {
    long startTime = alignStartTime();
    String comment = "stime:" + startTime;
    header = new RawAudioHeader(id, startTime, frameTime, audioEncoding, numChannels,
            samplingRate, bitsPerSample, comment);

    System.out.println(System.currentTimeMillis() + " Starting data line " + id);

    dataSize = (frameTime * numChannels * samplingRate / 1000 * 2);
    byteArrayOutputStream = new ByteArrayOutputStream();

    captureThread = new CaptureThread(targetDataLine, id);
    setState(AudioCaptureState.PREFETCH);
    captureThread.start();
    waitActive();
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
        long loopStartTime = header.startTime;
        long delay = loopStartTime - System.currentTimeMillis();
        System.out.println(System.currentTimeMillis() + " Delaying start for " + delay);
        Thread.sleep(delay);
        readySetGo(targetDataLine);
        setState(AudioCaptureState.RUNNING);

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
    System.out.println("Starting audio capture for " + captureTimeMs/1000.0 + "s in "+segmentLengthMs/1000.0+"s segments");
    AudioFormat audioFormat = new AudioFormat((float) DEFAULT_SAMPLING_RATE,
            DEFAULT_BITS_PER_SAMPLE, DEFAULT_CHANNELS, true, false);
    List<LiveAudioStream> streams = getLiveAudioStreams(null, captureTimeMs, segmentLengthMs);
    for (LiveAudioStream stream : streams) {
      AudioRunner runner = new AudioRunner(stream);
      new Thread(runner, stream.id).start();
    }
  }

  public static List<LiveAudioStream> getLiveAudioStreams(String path, int captureTimeMs, int segmentLengthMs)
          throws Exception {
    String hostname = getHostName().replace('-', '+').replace('_','+');
    if (path == null) {
      path = "";
    }
    AudioFormat audioFormat = new AudioFormat((float) DEFAULT_SAMPLING_RATE,
            DEFAULT_BITS_PER_SAMPLE, DEFAULT_CHANNELS, true, false);
    findTargetDataLines(audioFormat);
    if (lines.size() == 0) {
      throw new Exception("No valid data lines found");
    }
    int cnt = 1;
    List<LiveAudioStream> streams = new ArrayList<LiveAudioStream>();
    for (Mixer mixer : lines.keySet()) {
      TargetDataLine line = lines.get(mixer);
      System.out.println(mixer.getMixerInfo().getName());
      String id = hostname + "-" + cnt;
      String fileName = path + id + "_%d.wav";
      LiveAudioStream stream = new LiveAudioStream(id, line, captureTimeMs,segmentLengthMs, fileName);
      streams.add(stream);
      cnt++;
    }
    return streams;
  }

  static class AudioRunner implements Runnable {
    final LiveAudioStream stream;

    private AudioRunner(LiveAudioStream stream) {
      this.stream = stream;
    }

    public void run() {
      System.out.println("Starting capture loop " + stream.id);
      try {
        stream.startSaveAudio();
        stream.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
      System.out.println("Terminating capture loop " + stream.id);
    }
  }

  private static void readySetGo(TargetDataLine targetDataLine) throws InterruptedException {
    synchronized(lines) {
      lineCount++;
      //System.out.println("Linecount is ++ " + lineCount);
      if (lineCount == lines.size()) {
        lines.notifyAll();
      } else while (lineCount < lines.size()) {
        lines.wait();
      }
      targetDataLine.start();

      if (targetDataLine.available() > 0) {
        System.out.println("Target line already has " + targetDataLine.available());
        // this flush is necessary on some versions of the JDK that seem to start capture before start()!
        targetDataLine.flush();
      }
    }
    //System.out.println("Linecount is == "+ + lineCount);
    Thread.sleep(1); // let other threads start their lines
    synchronized(lines) {
      lineCount--;
      //System.out.println("Linecount is -- "+ + lineCount);
      while (lineCount > 0) {
        lines.wait();
      }
      lines.notifyAll();
    }
    System.out.println(System.currentTimeMillis() + " Releasing line");
  }

  private long alignStartTime() throws Exception {
    if ((delayWindowMs % frameTime) != 0) {
      throw new IllegalArgumentException("delayWindowMs(" + delayWindowMs +
              ") must be a mod of frameTime("+frameTime+")");
    }
    long loopTime = System.currentTimeMillis();
    int delay = delayWindowMs - (int) (loopTime % delayWindowMs);
    if (delay < frameTime*2)
      delay += delayWindowMs;
    loopTime += delay;
    System.out.println("Aiming to start at " + loopTime);
    return loopTime;
  }

  private void startSaveAudio()
          throws Exception {
    String segmentName = id;
    try {
      StreamFrame frame;

      do {
        frame = recvFrame();
      } while (frame != null);
    } finally {
      System.out.println("Audio stream " + segmentName + " complete at " + System.currentTimeMillis());
      close();
    }
  }
}
