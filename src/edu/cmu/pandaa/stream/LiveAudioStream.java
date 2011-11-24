package edu.cmu.pandaa.stream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.utils.DataConversionUtil;

public class LiveAudioStream implements FrameStream {

  boolean stopCapture = false;

  ByteArrayOutputStream byteArrayOutputStream;
  ByteArrayInputStream byteArrayInputStream;
  FrameStream fs;
  public long timeStamp;
  public boolean isTimeStamped;
  private int audioFormat, bitsPerSample;
  private long numChannels, samplingRate, dataSize, headerSize;
  private int frameLength;
  private int audioCaptureTime;
  private int audioOffset;
  private RawAudioHeader header;

  private final static int DEFAULT_FORMAT = 1; // PCM
  private final static long DEFAULT_CHANNELS = 1; // MONO
  private final static long DEFAULT_SAMPLING_RATE = 16000;
  private final static int DEFAULT_BITS_PER_SAMPLE = 16;
  private final static long DEFAULT_SUBCHUNK1_SIZE = 16; // For PCM
  private final static long DEFAULT_DATA_SIZE = 0;
  private final static int DEFAULT_FRAMELENGTH = 100;
  private final static int DEFAULT_AUDIO_CAPTURE_TIME = 10;

  public LiveAudioStream(int format, long samplingRate, int bitsPerSample, int frameLen,
                         String outFile) {
    audioFormat = format;
    this.samplingRate = samplingRate;
    this.bitsPerSample = bitsPerSample;
    frameLength = frameLen;
    numChannels = DEFAULT_CHANNELS;
    headerSize = DEFAULT_SUBCHUNK1_SIZE;
    dataSize = DEFAULT_DATA_SIZE;
    isTimeStamped = false;
    audioCaptureTime = DEFAULT_AUDIO_CAPTURE_TIME * 1000; // numSeconds * 1000
    // ms
    audioOffset = 0;
  }

  public LiveAudioStream() {
    this(DEFAULT_FORMAT, DEFAULT_SAMPLING_RATE, DEFAULT_BITS_PER_SAMPLE, DEFAULT_FRAMELENGTH, null);
  }

  @Override
  public void setHeader(StreamHeader h) throws Exception {
    /* TAP: this should be an error?  You can't set the header of an input audio stream -- it's read-only! */
    RawAudioHeader audioHeader = (RawAudioHeader)h;
    audioFormat = audioHeader.getAudioFormat();
    numChannels = audioHeader.getNumChannels();
    bitsPerSample = audioHeader.getBitsPerSample();
    samplingRate = audioHeader.getSamplingRate();
    dataSize = audioHeader.getSubChunk2Size();
    byteArrayOutputStream = new ByteArrayOutputStream();
    audioOffset = 0;
  }

  @Override
  public void sendFrame(StreamFrame m) throws Exception {
    /* TAP: this should be an error?  You can't write to an audio stream... */
    RawAudioFrame frame = (RawAudioFrame)m;
    byte[] audioBytes = DataConversionUtil.shortArrayToByteArray(frame.audioData);
    byteArrayOutputStream.write(audioBytes, 0, audioBytes.length);
    audioOffset += audioBytes.length;
  }

  @Override
  public StreamHeader getHeader() throws Exception {
    /* TAP: This shouldn't be an error -- it should wait() until data is ready */
    if (!isTimeStamped)
      throw new RuntimeException("No Audio captured yet");
    dataSize = audioCaptureTime * numChannels * (samplingRate / 1000) * 2;
    header = new RawAudioHeader("DeviceId", timeStamp, frameLength, audioFormat, numChannels,
            samplingRate, bitsPerSample, dataSize);
    return header;
  }

  @Override
  public StreamFrame recvFrame() throws Exception {
    /* TAP This shouldn't be an error -- it should wait() until data is ready */
    if (byteArrayOutputStream == null)
      throw new RuntimeException("No Audio captured yet");
    int nextBytesLength = -1;
    synchronized (byteArrayOutputStream) {
      byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray(),
              audioOffset, frameLength * 2);
      nextBytesLength = byteArrayInputStream.available();
    }
    audioOffset += (frameLength * 2);
    if (nextBytesLength <= 0)
      return null;
    RawAudioFrame audioFrame = header.makeFrame();
    byte[] audioBytes = new byte[nextBytesLength];
    byteArrayInputStream.read(audioBytes);
    audioFrame.audioData = DataConversionUtil.byteArrayToShortArray(audioBytes);
    return audioFrame;
  }

  @Override
  public void close() throws Exception {
    if (byteArrayInputStream != null)
      byteArrayInputStream.close();
    if (byteArrayOutputStream != null)
      byteArrayOutputStream.close();
  }

  public void startAudioCapture() {
    System.out.println("Starting audio capture.");
    captureAudio(new AudioFormat((float) samplingRate, bitsPerSample, (int) numChannels, true,
            false));
    try {
      Thread.sleep(audioCaptureTime);
    } catch (InterruptedException e1) {
      e1.printStackTrace();
    }
    stopCapture = true;
  }

  public void setAudioCaptureTime(int sec) {
    audioCaptureTime = sec * 1000;
  }

  protected void saveCapturedAudio(String filePath) {
    RawAudioFileStream outFile = null;
    int startIndex = 0, endIndex = 0;
    try {
      outFile = new RawAudioFileStream(filePath, true);
      short[] audio = DataConversionUtil.byteArrayToShortArray(byteArrayOutputStream.toByteArray());
      dataSize = audio.length;
      int numAudioSamples = (int) (frameLength * samplingRate / 1000);
      RawAudioHeader header = new RawAudioHeader(getDeviceID(filePath), timeStamp, frameLength,
              audioFormat, numChannels, samplingRate, bitsPerSample, dataSize);
      outFile.setHeader(header);
      while (true) {
        startIndex = endIndex;
        endIndex = (int) ((dataSize < startIndex + numAudioSamples) ? dataSize : startIndex
                + numAudioSamples);
        RawAudioFrame frame = header.makeFrame(numAudioSamples);
        for (int i = 0; i < endIndex - startIndex; i++) {
          frame.audioData[i] = audio[startIndex + i];
        }
        outFile.sendFrame(frame);
        if (endIndex >= dataSize)
          break;
      }
      outFile.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private String getDeviceID(String fileName) {
    int startIndex = 0, endIndex;

    startIndex = fileName.lastIndexOf("\\") + 1;

    endIndex = fileName.lastIndexOf(".");
    if (endIndex == -1) {
      endIndex = fileName.length();
    }

    return fileName.substring(startIndex, endIndex);
  }

  // This method captures audio input from a microphone and saves it in a
  // ByteArrayOutputStream object.
  private void captureAudio(AudioFormat audioFormat) {
    try {
      Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
      Mixer mixer = null;
      for (int cnt = 0; cnt < mixerInfo.length; cnt++) {
        if (mixerInfo[cnt].getName().toLowerCase().contains("microphone")) {
          mixer = AudioSystem.getMixer(mixerInfo[cnt]);
          break;
        }
      }

      DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

      TargetDataLine targetDataLine = (TargetDataLine) mixer.getLine(dataLineInfo);
      targetDataLine.open(audioFormat);
      targetDataLine.start();

      // Creating a thread to capture the microphone data and start it running.
      // It will run until the Stop button is clicked.
      Thread captureThread = new CaptureThread(targetDataLine);
      captureThread.start();
    } catch (Exception e) {
      System.out.println(e);
      System.exit(0);
    }
  }

  // Inner class to capture data from microphone
  class CaptureThread extends Thread {
    byte tempBuffer[] = new byte[10000];
    TargetDataLine targetDataLine;

    public CaptureThread(TargetDataLine targetDataLine) {
      this.targetDataLine = targetDataLine;
    }

    @Override
    public void run() {
      byteArrayOutputStream = new ByteArrayOutputStream();
      stopCapture = false;
      try {
        while (!stopCapture) {
          if (!isTimeStamped) {
            isTimeStamped = true;
            timeStamp = System.currentTimeMillis();
          }
          int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
          if (cnt > 0) {
            synchronized (byteArrayOutputStream) {
              /* TAP: This should really just make this bit of data available and make it available
               * to recvFrame ASAP... it looks like you're trying to read the ENTIRE sequence of audio
               * into a buffer first, which isn't quite what we want.
               */
              byteArrayOutputStream.write(tempBuffer, 0, cnt);
            }
          }
        }
      } catch (Exception e) {
        System.out.println(e);
        System.exit(0);
      }
    }
  }

  public void playAudio() {
    try {
      byte audioData[] = byteArrayOutputStream.toByteArray();
      InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
      AudioFormat audioFormat = new AudioFormat(samplingRate,
              bitsPerSample, (int) numChannels, true, false);

      AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, audioFormat,
              audioData.length / audioFormat.getFrameSize());

      DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
      SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
      sourceDataLine.open(audioFormat);
      sourceDataLine.start();

      // Create a thread to play back the data and start it running.
      // It will run until all the data has been played back.
      Thread playThread = new PlayThread(audioInputStream, sourceDataLine);
      playThread.start();
    } catch (Exception e) {
      System.out.println(e);
      System.exit(0);
    }
  }

  class PlayThread extends Thread {
    byte tempBuffer[] = new byte[10000];
    AudioInputStream audioInputStream;
    SourceDataLine sourceDataLine;

    public PlayThread(AudioInputStream audioInputStream, SourceDataLine sourceDataLine) {
      this.audioInputStream = audioInputStream;
      this.sourceDataLine = sourceDataLine;
    }

    @Override
    public void run() {
      try {
        int cnt;
        while ((cnt = audioInputStream.read(tempBuffer, 0, tempBuffer.length)) != -1) {
          if (cnt > 0) {
            sourceDataLine.write(tempBuffer, 0, cnt);
          }
        }
        sourceDataLine.drain();
        sourceDataLine.close();
      } catch (Exception e) {
        System.out.println(e);
        System.exit(0);
      }
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      throw new RuntimeException("Usage java LiveAudioCapture outputFileName audioCaptureTime");
    }
    int arg = 0;
    String fileName = args[arg++];
    int captureTime = new Integer(args[arg++]);
    LiveAudioStream liveAudioStream = new LiveAudioStream();
    liveAudioStream.setAudioCaptureTime(captureTime);
    liveAudioStream.startAudioCapture();
    System.out.println("Saving captured audio in " + fileName);
    RawAudioFileStream rawAudioOutputStream = null, inStream = null;
    rawAudioOutputStream = new RawAudioFileStream(fileName, true);
    rawAudioOutputStream.setHeader(liveAudioStream.getHeader());
    StreamFrame frame = null;
    while ((frame = liveAudioStream.recvFrame()) != null) {
      rawAudioOutputStream.sendFrame(frame);
    }
    rawAudioOutputStream.close();
    System.out.println("Audio saved");

    /* TAP: I don't know what this is doing -- we should be done at this point.
    * where is this "sending" the audio?
    * If this is just to *test* the ability to play audio then I get it, although
    * it's really not necessary because our system is capture-only!
    */
    inStream = new RawAudioFileStream(fileName);
    liveAudioStream.setHeader(inStream.getHeader());
    while((frame = inStream.recvFrame()) != null) {
      liveAudioStream.sendFrame(frame);
    }
    System.out.println("Playing captured audio.");
    liveAudioStream.playAudio();

    if (liveAudioStream != null)
      liveAudioStream.close();
    if (rawAudioOutputStream != null)
      rawAudioOutputStream.close();
    if(inStream != null)
      inStream.close();
    System.out.println("Finished playing audio");
  }
}
