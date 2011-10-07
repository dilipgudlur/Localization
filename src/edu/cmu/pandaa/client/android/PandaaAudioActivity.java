package edu.cmu.pandaa.client.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Date;

import edu.cmu.pandaa.shared.stream.FileStream;
import edu.cmu.pandaa.shared.stream.FrameStream;
import edu.cmu.pandaa.shared.stream.header.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.shared.stream.header.StreamHeader;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.EditText;

public class PandaaAudioActivity extends Activity {
	EditText textArea;
	int numFrames = 0;

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
				this.wait(3000);
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

		StreamHeader h = audioFileStream.getHeader();
		RawAudioFrame f = null;
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(new File(
					getApplicationContext().getFilesDir() + "/test_audio.raw")));

			textArea.append("FrameTime = " + new Long(h.frameTime).toString()
					+ "\n");
			textArea.append("StartTime = " + new Date(h.startTime) + "\n");
			while ((f = (RawAudioFrame) audioFileStream.recvFrame()) != null) {
//				textArea.append(new Integer(f.audioData.length).toString());
				numFrames++;
//				oos.write(f.audioData);
			}
			textArea.append("\n Exceeding number of bytes: " + new Integer(AcquireAudio.numberOfBytes).toString() + "\n");
			textArea.append(new Integer(numFrames).toString() + "\n");
			oos.close();
//			MediaPlayer mpPlayer = new MediaPlayer();
//			textArea.setText("Creating Media player\n");
//			mpPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//			textArea.append("Setting Audio Stream\n");
//			mpPlayer.setDataSource(getApplicationContext().getFilesDir() + "/test_audio.raw");
//			textArea.append("setting Data source\n");
//			mpPlayer.prepare(); // might take long! (for buffering, etc)
//			textArea.append("Preparing Media player\n");
//			mpPlayer.start();
//			textArea.append("Playing media from: " + getApplicationContext().getFilesDir() + "/test_audio.raw");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}