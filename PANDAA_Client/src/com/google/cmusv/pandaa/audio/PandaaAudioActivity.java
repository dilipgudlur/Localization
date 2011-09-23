package com.google.cmusv.pandaa.audio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Date;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.EditText;

import com.google.cmusv.pandaa.audio.AcquireAudio.RawAudioFrame;
import com.google.cmusv.pandaa.audio.FrameStream.Header;

public class PandaaAudioActivity extends Activity {
	EditText textArea;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		textArea = (EditText) findViewById(R.id.textarea);

		String fileName = getApplicationContext().getFilesDir() + "/test.raw";

		FrameStream audioFileStream = new FileStream(fileName);
		AcquireAudio audioRecorder = new AcquireAudio(audioFileStream);
		Thread th = new Thread(audioRecorder);

		th.start();
		audioRecorder.setRecording(true);
		synchronized (this) {
			try {
				this.wait(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
		audioRecorder.setRecording(false);
		try {
			th.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Header h = audioFileStream.getHeader();
		RawAudioFrame f = null;
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(new File(
					getApplicationContext().getFilesDir() + "/test_audio.raw")));

			textArea.append("FrameTime = " + new Long(h.frameTime).toString()
					+ "\n");
			textArea.append("StartTime = " + new Date(h.startTime) + "\n");
			while ((f = (RawAudioFrame) audioFileStream.recvMessage()) != null) {
				oos.writeShort(f.audioData);
			}
			MediaPlayer mpPlayer = new MediaPlayer();
			mpPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mpPlayer.setDataSource(getApplicationContext().getFilesDir() + "/test_audio.raw");
			mpPlayer.prepare(); // might take long! (for buffering, etc)
			mpPlayer.start();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}