package edu.cmu.pandaa.desktop;

import java.io.ByteArrayOutputStream;

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

	public enum AudioCaptureState {
		BEFORE, RUNNING, STOPPED;
	};

	AudioCaptureState audioCaptureState = AudioCaptureState.BEFORE;
	private final int audioFormat, bitsPerSample;
	private final long numChannels, samplingRate;
	private int dataSize = -1;
	private final int frameLength;
  private final int captureTime;
	private RawAudioHeader header;

	private final static int DEFAULT_FORMAT = 1; // PCM
	private final static long DEFAULT_CHANNELS = 1; // MONO
	private final static long DEFAULT_SAMPLING_RATE = 22050;
	private final static int DEFAULT_BITS_PER_SAMPLE = 16;
	private final static long DEFAULT_SUBCHUNK1_SIZE = 16; // For PCM
  private final static int DEFAULT_FRAMELENGTH = 100;
  private final static int DEFAULT_CAPTURE_TIME = 10;


	public LiveAudioStream(int format, long samplingRate, int bitsPerSample, int frameLen, int captureTime) {
		audioFormat = format;
		this.samplingRate = samplingRate;
		this.bitsPerSample = bitsPerSample;
		frameLength = frameLen;
    numChannels = DEFAULT_CHANNELS;
    this.captureTime = captureTime;
  }

  public LiveAudioStream() {
    this(DEFAULT_FORMAT, DEFAULT_SAMPLING_RATE, DEFAULT_BITS_PER_SAMPLE, DEFAULT_FRAMELENGTH, DEFAULT_CAPTURE_TIME);
  }

  public LiveAudioStream(int captureTime) {
    this(DEFAULT_FORMAT, DEFAULT_SAMPLING_RATE, DEFAULT_BITS_PER_SAMPLE, DEFAULT_FRAMELENGTH, captureTime);
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
		synchronized (this) {
      startAudioCapture();
			while (!isRunning()) {
				System.out.println("Waiting for data capture to start");
				wait();
			}
		}

    String comment = "stime:" + (usecTime[1]- usecTime[0]) + ","
            + (usecTime[2]- usecTime[1]) + ","
            + (usecTime[3]- usecTime[2]);
		header = new RawAudioHeader("DeviceId", startTime, frameLength, audioFormat, numChannels,
				samplingRate, bitsPerSample, captureTime, comment);
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

    if (header.nextSeq * header.frameTime > captureTime)
      return null;

		RawAudioFrame audioFrame = header.makeFrame();
		audioFrame.audioData = DataConversionUtil.byteArrayToShortArray(audioData);
		return audioFrame;
	}

  @Override
  public void close() throws Exception {
    synchronized(this) {
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
  }

  private void startAudioCapture() throws Exception {
    dataSize = (int) (frameLength * numChannels * (samplingRate / 1000) * 2);
		System.out.println("Starting audio capture.");
		captureAudio(new AudioFormat((float) samplingRate, bitsPerSample, (int) numChannels,
            true, false));
  }

  private void stopAudioCapture() {
		setStatus(AudioCaptureState.STOPPED);
	}

	private synchronized void setStatus(AudioCaptureState newState) {
		audioCaptureState = newState;
		notifyAll();
	}

	private synchronized boolean isRunning() {
    return audioCaptureState == AudioCaptureState.RUNNING;
  }

  // This method captures audio input from a microphone and saves it in a
  // ByteArrayOutputStream object.
  private void captureAudio(AudioFormat audioFormat) throws Exception {
    Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
    Mixer mixer = null;
    for (int cnt = 0; cnt < mixerInfo.length; cnt++) {
      if (mixerInfo[cnt].getName().toLowerCase().contains("microphone")) {
        mixer = AudioSystem.getMixer(mixerInfo[cnt]);
        break;
      }
    }

    if (mixer == null)
      mixer = AudioSystem.getMixer(mixerInfo[0]);

    DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

    TargetDataLine targetDataLine = (TargetDataLine) mixer.getLine(dataLineInfo);
    targetDataLine.open(audioFormat);
    targetDataLine.start();

    // Creating a thread to capture the microphone data and start it running.
    startTime = System.currentTimeMillis();
    usecTime[0] = System.nanoTime()/1000;
    captureThread = new CaptureThread(targetDataLine);
    for (int i = 1; i < usecTime.length; i++) {
      captureThread.readData();
      usecTime[i] = System.nanoTime()/1000;
    }
    captureThread.start();
  }

  // Inner class to capture data from microphone
	class CaptureThread extends Thread {
		byte dataBuffer[] = new byte[dataSize];
		TargetDataLine targetDataLine;

		public CaptureThread(TargetDataLine targetDataLine) {
			this.targetDataLine = targetDataLine;
		}

    public int readData() {
      return targetDataLine.read(dataBuffer, 0, dataBuffer.length);
    }

    @Override
    public void run() {
      byteArrayOutputStream = new ByteArrayOutputStream();
      setStatus(AudioCaptureState.RUNNING);
      try {
        while (isRunning()) {
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
        System.exit(0);
      }
    }
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      throw new RuntimeException("Usage: java LiveAudioCapture outputFileName audioCaptureTime");
    }
    int arg = 0;
    final String fileName = args[arg++];
    final int captureTime = new Integer(args[arg++]) * 1000;
    final LiveAudioStream liveAudioStream = new LiveAudioStream(captureTime);
    RawAudioFileStream rawAudioOutputStream;
    try {
      rawAudioOutputStream = new RawAudioFileStream(fileName, true);
      rawAudioOutputStream.setHeader(liveAudioStream.getHeader());
      System.out.println("Saving captured audio in " + fileName);
      StreamFrame frame;
      while ((frame = liveAudioStream.recvFrame()) != null) {
        rawAudioOutputStream.sendFrame(frame);
      }
      liveAudioStream.close();
      rawAudioOutputStream.close();
      System.out.println("Audio saved");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
