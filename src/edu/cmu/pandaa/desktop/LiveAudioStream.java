package edu.cmu.pandaa.desktop;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

	boolean stopCapture = false;

	ByteArrayOutputStream byteArrayOutputStream;
	ByteArrayInputStream byteArrayInputStream;
	FrameStream fs;
	public long timeStamp;

	public enum AudioCaptureStatus {
		STARTED, NOT_STARTED
	};

	AudioCaptureStatus audioCaptureStatus;
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
		audioCaptureStatus = AudioCaptureStatus.NOT_STARTED;
		audioCaptureTime = DEFAULT_AUDIO_CAPTURE_TIME * 1000; // numSeconds * 1000
																													// ms
		audioOffset = 0;
	}

	public LiveAudioStream() {
		this(DEFAULT_FORMAT, DEFAULT_SAMPLING_RATE, DEFAULT_BITS_PER_SAMPLE, DEFAULT_FRAMELENGTH, null);
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
		synchronized (audioCaptureStatus) {
			if (audioCaptureStatus == AudioCaptureStatus.NOT_STARTED) {
				System.out.println("Waiting for data capture to start");
				audioCaptureStatus.wait();
				audioCaptureStatus = AudioCaptureStatus.STARTED;
				System.out.println("Audio capture started");
			}
		}
		dataSize = audioCaptureTime * numChannels * (samplingRate / 1000) * 2;
		header = new RawAudioHeader("DeviceId", timeStamp, frameLength, audioFormat, numChannels,
				samplingRate, bitsPerSample, dataSize);
		return header;
	}

	@Override
	public StreamFrame recvFrame() throws Exception {
		if (byteArrayOutputStream == null) {
			System.out.println("Waiting for audio data");
			byteArrayOutputStream.wait();
		}
		int nextBytesLength = -1;

		byte[] audioData = byteArrayOutputStream.toByteArray();
		while (true) {
			if ((audioData.length == 0
					|| (audioData.length != 0 && (audioData.length - audioOffset) == 0) || ((audioData.length - audioOffset) < frameLength * 2))
					&& !stopCapture) {
				synchronized (byteArrayOutputStream) {
					byteArrayOutputStream.wait();
				}
			} else
				break;
			audioData = byteArrayOutputStream.toByteArray();
		}
		byteArrayInputStream = new ByteArrayInputStream(audioData, audioOffset, frameLength * 2);
		nextBytesLength = byteArrayInputStream.available();
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

	public void stopAudioCapture() {
		stopCapture = true;
	}
	
	public void setAudioCaptureTime(int sec) {
		audioCaptureTime = sec * 1000;
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
					synchronized (audioCaptureStatus) {
						if (audioCaptureStatus == AudioCaptureStatus.NOT_STARTED) {
							timeStamp = System.currentTimeMillis();
							audioCaptureStatus.notifyAll();
						}
					}
					int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
					if (cnt > 0) {
						synchronized (byteArrayOutputStream) {
							byteArrayOutputStream.write(tempBuffer, 0, cnt);
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
			throw new RuntimeException("Usage java LiveAudioCapture outputFileName audioCaptureTime");
		}
		int arg = 0;
		final String fileName = args[arg++];
		final int captureTime = new Integer(args[arg++]);
		final LiveAudioStream liveAudioStream = new LiveAudioStream();
		new Thread() {
			public void run() {
				RawAudioFileStream rawAudioOutputStream = null;
				try {
					rawAudioOutputStream = new RawAudioFileStream(fileName, true);
					rawAudioOutputStream.setHeader(liveAudioStream.getHeader());
					System.out.println("Saving captured audio in " + fileName);
					StreamFrame frame = null;
					while ((frame = liveAudioStream.recvFrame()) != null) {
						rawAudioOutputStream.sendFrame(frame);
					}
					rawAudioOutputStream.close();
					System.out.println("Audio saved");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					if (rawAudioOutputStream != null)
						rawAudioOutputStream.close();
				}
			}
		}.start();
		new Thread() {
			public void run() {
				try {
					liveAudioStream.startAudioCapture();
//					try {
//						Thread.sleep(captureTime);
//					} catch (InterruptedException e1) {
//						e1.printStackTrace();
//					}
//					liveAudioStream.stopAudioCapture();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (liveAudioStream != null)
						try {
							liveAudioStream.close();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				}
			}
		}.start();
	}
}
