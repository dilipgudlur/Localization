package edu.cmu.pandaa.module;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.stream.RawAudioFileStream;

public class AudioSynchronizationModule implements StreamModule {

	private boolean firstPeakImpulseFound;
	RawAudioHeader rawAudioHeader;

	@Override
	public StreamHeader init(StreamHeader inHeader) {
		if (!(inHeader instanceof RawAudioHeader))
			throw new RuntimeException("Wrong header type");
		firstPeakImpulseFound = false;
		rawAudioHeader = (RawAudioHeader) inHeader;
		return rawAudioHeader;
	}

	@Override
	public StreamFrame process(StreamFrame inFrame) {
		if (inFrame == null)
			return null;
		if (!(inFrame instanceof RawAudioFrame))
			throw new RuntimeException("Wrong frame type");
		RawAudioFrame audioFrame = (RawAudioFrame) inFrame;
		short audioData[] = audioFrame.audioData;
		if (!firstPeakImpulseFound) {
			int peakIndex = -1;
			for (int i = 0; i < audioData.length; i++) {
				if (!firstPeakImpulseFound && (audioData[i] == 32767 || audioData[i] == -32768)) {
					firstPeakImpulseFound = true;
					peakIndex = i;
					break;
				}
			}
			if (peakIndex == -1) {
				rawAudioHeader.setDataSize(rawAudioHeader.getSubChunk2Size() - audioFrame.audioData.length);
				audioFrame.audioData = null;
			} else {
				rawAudioHeader.setDataSize(rawAudioHeader.getSubChunk2Size() - peakIndex);
				audioData = new short[audioFrame.audioData.length - peakIndex];
				for (int i = 0; i < (audioFrame.audioData.length - peakIndex); i++)
					audioData[i] = audioFrame.audioData[i + peakIndex];
				audioFrame.audioData = audioData;
			}
		}
		return audioFrame;
	}

	@Override
	public void close() {

	}

	/*
	 * Arguments: 1- File Directory location 2- Input file prefix 3- Output file
	 * prefix
	 */
	public static void main(String args[]) {
		int arg = 0;
		File fileLoc = new File(args[arg++]);
		String inputFilePrefix = args[arg++];
		String outputFilePrefix = args[arg++];
		if (args.length != arg)
			throw new RuntimeException(
					"Invalid number of arguments\n Usage java AudioSynchronizationModule <DirectoryLocation> <InputFilePrefix> <OutputFilePrefix>");
		File[] audioFiles = fileLoc.listFiles();
		ArrayList<File> audioFilesList = new ArrayList<File>();
		for (int i = 0; i < audioFiles.length; i++) {
			if (!audioFiles[i].isDirectory() && audioFiles[i].getName().startsWith(inputFilePrefix)
					&& audioFiles[i].getName().endsWith(".wav"))
				audioFilesList.add(audioFiles[i]);
		}
		RawAudioFileStream inFile = null, outFile = null;
		AudioSynchronizationModule syncModule = null;
		for (File inFileName : audioFilesList) {
			try {
				String outFileName = fileLoc.getAbsolutePath() + "\\" + outputFilePrefix
						+ inFileName.getName();
				inFile = new RawAudioFileStream(inFileName.getAbsolutePath());
				outFile = new RawAudioFileStream(outFileName, true);
				syncModule = new AudioSynchronizationModule();
				outFile.setHeader(syncModule.init(inFile.getHeader()));
				RawAudioFrame frame = null;
				while ((frame = (RawAudioFrame) syncModule.process(inFile.recvFrame())) != null) {
					outFile.sendFrame(frame);
				}
				System.out.println("Saved file: " + outFileName);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
