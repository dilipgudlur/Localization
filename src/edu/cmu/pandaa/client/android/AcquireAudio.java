package edu.cmu.pandaa.client.android;

import java.io.Serializable;

import edu.cmu.pandaa.shared.stream.FrameStream;
import edu.cmu.pandaa.shared.stream.header.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.shared.stream.header.RawAudioHeader;


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class AcquireAudio implements Runnable, Serializable {

	FrameStream frameStream;
	transient RawAudioFrame audioFrame;

	transient private int frequency;
	transient private int channelConfiguration;
	transient private volatile boolean isRecording;
	transient public static int numberOfBytes = 0;
	transient private boolean isHeaderSet;
	transient private int audioIndex;
	transient private int frameTime, frameLength;

	// Changing the sample resolution changes sample type. byte vs. short.
	transient private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	transient private static final int AUDIO_FREQUENCY = 16000;
	transient private static final int FRAME_TIME = 100;
	transient private static final int MILLISECONDS = 1000;
	
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
		int bufferSize = AudioRecord.getMinBufferSize(this.getFrequency(),
				this.getChannelConfiguration(), this.getAudioEncoding());
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
				frameStream.setHeader(new RawAudioHeader(frameTime));
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
					audioFrame = new RawAudioFrame(frameLength);
					audioIndex = 0;
				}
				audioFrame.audioData[audioIndex++] = tempBuffer[idxBuffer];
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