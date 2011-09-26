package com.google.cmusv.pandaa.audio;

import java.io.Serializable;

import com.google.cmusv.pandaa.stream.FrameStream.Frame;
import com.google.cmusv.pandaa.stream.FrameStream.Header;
import com.google.cmusv.pandaa.stream.FrameStream.LocalFrameStream;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class AcquireAudio implements Runnable, Serializable {

	transient LocalFrameStream frameStream;
	transient RawAudioFrame audioFrame;

	transient private int frequency;
	transient private int channelConfiguration;
	transient private volatile boolean isPaused;
	transient private final Object mutex = new Object();
	transient private volatile boolean isRecording;
	transient public static int numberOfBytes = 0;
	transient private boolean isHeaderSet;

	// Changing the sample resolution changes sample type. byte vs. short.
	transient private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

	public AcquireAudio(LocalFrameStream out) {
		frameStream = out;
		isHeaderSet = false;
		this.setFrequency(16000);
		this.setChannelConfiguration(AudioFormat.CHANNEL_CONFIGURATION_MONO);
		this.setPaused(false);
		numberOfBytes = 0;
	}

	public void run() {
		// Wait until we're recording
		synchronized (mutex) {
			while (!this.isRecording) {
				try {
					mutex.wait();
				} catch (InterruptedException e) {
					throw new IllegalStateException("Wait() interrupted!", e);
				}
			}
		}

		// Since the audio frames should not be stalled, we assign this priority
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

		// Allocate Recorder and Start Recording
		int bufferRead = 0;
		int bufferSize = AudioRecord.getMinBufferSize(this.getFrequency(),
				this.getChannelConfiguration(), this.getAudioEncoding());
		AudioRecord recordInstance = new AudioRecord(
				MediaRecorder.AudioSource.MIC, this.getFrequency(),
				this.getChannelConfiguration(), this.getAudioEncoding(),
				bufferSize);
		short[] tempBuffer = new short[bufferSize];
		recordInstance.startRecording();
		while (this.isRecording) {
			// Are we paused?
			synchronized (mutex) {
				if (this.isPaused) {
					try {
						mutex.wait(250);
					} catch (InterruptedException e) {
						throw new IllegalStateException("Wait() interrupted!",
								e);
					}
					continue;
				}
			}

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
			if(bufferRead > 0 && !isHeaderSet) {
				isHeaderSet = true;
				frameStream.setHeader(new RawAudioHeader());
			}
			for (int idxBuffer = 0; idxBuffer < bufferRead; ++idxBuffer) {
				audioFrame = new RawAudioFrame();
				audioFrame.audioData = tempBuffer[idxBuffer];
				frameStream.sendFrame(audioFrame);
				numberOfBytes++;
			}
		}

		// Close resources
		recordInstance.stop();
	}

	class RawAudioFrame extends Frame implements Serializable {
		Short audioData;
	}

	class RawAudioHeader extends Header implements Serializable {
		
		public RawAudioHeader() {
			/*SntpClient sntpClient = new SntpClient();
			if (sntpClient.requestTime("pool.ntp.org", 1000)) {
				startTime = sntpClient.getNtpTime() + SystemClock.elapsedRealtime()
						- sntpClient.getNtpTimeReference();
			}*/
			startTime = System.currentTimeMillis();
			frameTime = new Long(100);
		}
	}

	/**
	 * @param isRecording
	 *            the isRecording to set
	 */
	public void setRecording(boolean isRecording) {
		synchronized (mutex) {
			this.isRecording = isRecording;
			if (this.isRecording) {
				mutex.notify();
			}
		}
	}

	/**
	 * @return the isRecording
	 */
	public boolean isRecording() {
		synchronized (mutex) {
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

	/**
	 * @param isPaused
	 *            the isPaused to set
	 */
	public void setPaused(boolean isPaused) {
		synchronized (mutex) {
			this.isPaused = isPaused;
		}
	}

	/**
	 * @return the isPaused
	 */
	public boolean isPaused() {
		synchronized (mutex) {
			return isPaused;
		}
	}

}