package edu.cmu.pandaa.desktop;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioCapture {

	public AudioCapture(File audioFile) {
		try {
			// From file
			System.out.println("trying to get audio input stream" + audioFile.getAbsolutePath());
			AudioInputStream stream = AudioSystem
					.getAudioInputStream(audioFile);
			System.out.println("Got the audio stream");
			/*
			 * // From URL stream = AudioSystem.getAudioInputStream(new
			 * URL("http://hostname/audiofile"));
			 */
			// At present, ALAW and ULAW encodings must be converted
			// to PCM_SIGNED before it can be played
			AudioFormat format = stream.getFormat();
			if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
				format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
						format.getSampleRate(),
						format.getSampleSizeInBits() * 2, format.getChannels(),
						format.getFrameSize() * 2, format.getFrameRate(), true); // big
																					// endian
				stream = AudioSystem.getAudioInputStream(format, stream);
			}

			System.out.println("Audio format: " + format.toString());
			// Create line
			SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class,
					stream.getFormat(),
					((int) stream.getFrameLength() * format.getFrameSize()));
			SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(stream.getFormat());
			line.start();

			// Continuously read and play chunks of audio
			int numRead = 0;
			byte[] buf = new byte[line.getBufferSize()];
			while ((numRead = stream.read(buf, 0, buf.length)) >= 0) {
				int offset = 0;
				while (offset < numRead) {
					offset += line.write(buf, offset, numRead - offset);
				}
			}
			line.drain();
			line.stop();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}