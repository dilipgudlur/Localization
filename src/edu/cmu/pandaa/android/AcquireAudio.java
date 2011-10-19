package edu.cmu.pandaa.android;

import java.io.Serializable;

import edu.cmu.pandaa.frame.RawAudioHeader;
import edu.cmu.pandaa.frame.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.stream.FrameStream;
import edu.cmu.pandaa.utils.AudioTimeStamp;


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class AcquireAudio implements Runnable {

	FrameStream frameStream;
	RawAudioFrame audioFrame;
	RawAudioHeader audioHeader;

	private int frequency;
	private int channelConfiguration;
	private volatile boolean isRecording;
	public static int numberOfBytes = 0;
	private boolean isHeaderSet;
	private int audioIndex;
	private int frameTime, frameLength;

	// Changing the sample resolution changes sample type. byte vs. short.
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private static final int AUDIO_FREQUENCY = 16000;
	private static final int FRAME_TIME = 100;
	private static final int MILLISECONDS = 1000;
	
	public AcquireAudio(FrameStream out) {
		frameStream = out;
		isHeaderSet = false;
		this.setFrequency(AUDIO_FREQUENCY);
		this.setChannelConfiguration(AudioFormat.CHANNEL_CONFIGURATION_MONO);
		numberOfBytes = 0;
		audioIndex = 0;
		frameTime = FRAME_TIME;
		frameLength = (frequency / MILLISECONDS) * frameTime;
		audioFrame = null;
	}

	public void run() {
		// Since the audio frames should not be stalled, we assign this priority
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

		// Allocate Recorder and Start Recording…
		int bufferRead = 0;
		System.out.println("Freq: " + this.getFrequency() + "Channel Configuration: " +
				this.getChannelConfiguration() + " Audio Encoding: " + this.getAudioEncoding());
		int bufferSize = AudioRecord.getMinBufferSize(this.getFrequency(),
				this.getChannelConfiguration(), this.getAudioEncoding());
		System.out.println("Buffersise: " + bufferSize);
		AudioRecord recordInstance = new AudioRecord(
				MediaRecorder.AudioSource.MIC, this.getFrequency(),
				this.getChannelConfiguration(), this.getAudioEncoding(),
				bufferSize);
		recordInstance.startRecording();
		
		/* 
		 * While recording buffer audio samples for <frameTime> milliseconds 
		 * into a single Frame and send the Frame to the Stream  
		 */
		short[] tempBuffer = new short[bufferSize];
		while (this.isRecording) {
			bufferRead = recordInstance.read(tempBuffer, 0, bufferSize);
			if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
				throw new IllegalStateException(
						"read() returned AudioRecord.ERROR_INVALID_OPERATION");
			} else if (bufferRead == AudioRecord.ERROR_BAD_VALUE) {
				throw new IllegalStateException(
						"read() returned AudioRecord.ERROR_BAD_VALUE");
			} else if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
				throw new IllegalStateException(
						"read() returned AudioRecord.ERROR_INVALID_OPERATION");
			}
			
			// Set the header in the stream if it has not already been done
			if (bufferRead > 0 && !isHeaderSet) {
				isHeaderSet = true;
				audioHeader = initHeader();
				try {
					frameStream.sendHeader(audioHeader);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			for (int idxBuffer = 0; idxBuffer < bufferRead; ++idxBuffer) {
				
				// When a frame is full send it to the stream and create a new frame
				if (audioFrame == null || audioIndex >= frameLength) {
					if (audioFrame != null) {
						try {
							frameStream.sendFrame(audioFrame);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					audioFrame = ((RawAudioHeader)audioHeader).makeFrame(frameLength);
					audioIndex = 0;
				}
				audioFrame.audioData[audioIndex++] = (byte) tempBuffer[idxBuffer];
				numberOfBytes++;
			}
		}
		
		if(audioIndex > 0) {
			numberOfBytes = audioIndex;
			try {
				frameStream.sendFrame(audioFrame);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			audioIndex = 0;
			audioFrame = null;
		}

		// Close resources…
		recordInstance.stop();
	}

	private RawAudioHeader initHeader() {
		return new RawAudioHeader(AudioTimeStamp.getCurrentTime(), frameTime, this.getAudioEncoding(), this.getChannelConfiguration(), this.getFrequency(), 16, 0);
	}

	/**
	 * @param isRecording
	 *            the isRecording to set
	 */
	public void setRecording(boolean isRecording) {
		synchronized (this) {
			this.isRecording = isRecording;
			if (this.isRecording) {
				this.notify();
			}
		}
	}

	/**
	 * @return the isRecording
	 */
	public boolean isRecording() {
		synchronized (this) {
			return isRecording;
		}
	}

	/**
	 * @param frequency
	 *            the frequency to set
	 */
	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	/**
	 * @return the frequency
	 */
	public int getFrequency() {
		return frequency;
	}

	/**
	 * @param channelConfiguration
	 *            the channelConfiguration to set
	 */
	public void setChannelConfiguration(int channelConfiguration) {
		this.channelConfiguration = channelConfiguration;
	}

	/**
	 * @return the channelConfiguration
	 */
	public int getChannelConfiguration() {
		return channelConfiguration;
	}

	/**
	 * @return the audioEncoding
	 */
	public int getAudioEncoding() {
		return audioEncoding;
	}
}