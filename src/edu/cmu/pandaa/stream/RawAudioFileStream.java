package edu.cmu.pandaa.stream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.RawAudioHeader.RawAudioFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.utils.DataConversionUtil;
import edu.cmu.pandaa.utils.WavUtil;

public class RawAudioFileStream implements FrameStream {

	int wavFrameLength;
	private OutputStream os;
	private InputStream is;
	DataInputStream dis;
	DataOutputStream dos;
	private final String fileName;
	RawAudioHeader headerRef;
	private long numSamplesWritten = 0;
	private final int BITS_PER_BYTE = 8;

	public RawAudioFileStream(String fileName) throws IOException {
		this.fileName = fileName;
		is = new FileInputStream(fileName);
		wavFrameLength = RawAudioHeader.DEFAULT_FRAMETIME;
	}

	public RawAudioFileStream(String fileName, boolean overwrite) throws IOException {
		this.fileName = fileName;
		File file = new File(fileName);
		if (file.exists() && !overwrite) {
			throw new IOException("File exists");
		}
		os = new FileOutputStream(file);
		wavFrameLength = RawAudioHeader.DEFAULT_FRAMETIME;
	}

	@Override
	public StreamHeader getHeader() throws Exception {
		if (dis != null) {
			throw new RuntimeException("getHeader called twice!");
		}
		dis = new DataInputStream(is);
		byte[] tmpLong = new byte[4];
		byte[] tmpInt = new byte[2];

		long wavChunkSize = 0, wavSubChunk1Size = 0, wavDataSize = 0;
		long wavByteRate = 0, wavSamplingRate = 0;
		int wavFormat = 0, wavChannels = 0, wavBlockAlign = 0, wavBitsPerSample = 0;

		byte[] chunkID = new byte[4];
		int retval = dis.read(chunkID, 0, 4);

		dis.read(tmpLong);
		wavChunkSize = DataConversionUtil.byteArrayToLong(tmpLong);

		String format = "" + (char) dis.readByte() + (char) dis.readByte() + (char) dis.readByte()
				+ (char) dis.readByte();

		String subChunk1ID = "" + (char) dis.readByte() + (char) dis.readByte()
				+ (char) dis.readByte() + (char) dis.readByte();

		dis.read(tmpLong);
		wavSubChunk1Size = DataConversionUtil.byteArrayToLong(tmpLong);

		dis.read(tmpInt);
		wavFormat = DataConversionUtil.byteArrayToInt(tmpInt);

		dis.read(tmpInt);
		wavChannels = DataConversionUtil.byteArrayToInt(tmpInt);

		dis.read(tmpLong);
		wavSamplingRate = DataConversionUtil.byteArrayToLong(tmpLong);

		dis.read(tmpLong);
		wavByteRate = DataConversionUtil.byteArrayToLong(tmpLong);

		dis.read(tmpInt);
		wavBlockAlign = DataConversionUtil.byteArrayToInt(tmpInt);

		dis.read(tmpInt);
		wavBitsPerSample = DataConversionUtil.byteArrayToInt(tmpInt);

		String dataChunkID = "" + (char) dis.readByte() + (char) dis.readByte()
				+ (char) dis.readByte() + (char) dis.readByte();

		dis.read(tmpLong);
		wavDataSize = DataConversionUtil.byteArrayToLong(tmpLong);
		headerRef = new RawAudioHeader(System.currentTimeMillis(), wavFrameLength, wavFormat,
				wavChannels, wavSamplingRate, wavBitsPerSample, wavDataSize);
		return headerRef;
	}

	@Override
	public StreamFrame recvFrame() throws Exception {

		int frameLength = (int) (headerRef.getSamplingRate() / 1000) * wavFrameLength;
		RawAudioFrame rawAudioFrame = headerRef.makeFrame(frameLength);
		int numBytesInSample = headerRef.getBitsPerSample() / BITS_PER_BYTE;
		byte[] audioDataByte = new byte[frameLength * numBytesInSample];
		int bytesRead = dis.read(audioDataByte);
		if (bytesRead <= 0)
			return null;
		else {
			rawAudioFrame.audioData = DataConversionUtil.byteArrayToShortArray(audioDataByte);
			System.out.println(rawAudioFrame);
			return rawAudioFrame;
		}
	}

	private final String riffString = "RIFF";
	private final String formatString = "WAVE";
	private final String subChunk1String = "fmt ";
	private final String subChunk2String = "data";
	private final long DEFAULT_SUBCHUNK1_SIZE = 16; // For PCM

	@Override
	public void setHeader(StreamHeader h) throws Exception {
		headerRef = (RawAudioHeader) h;
		if (dos != null) {
			throw new RuntimeException("setHeader called twice!");
		}
		dos = new DataOutputStream(new FileOutputStream(fileName));

		long chunkSize = 36 + headerRef.getSubChunk2Size();
		long byteRate = (headerRef.getSamplingRate() * headerRef.getNumChannels() * headerRef
				.getBitsPerSample()) / 8;
		int blockAlign = (int) (headerRef.getNumChannels() * headerRef.getBitsPerSample()) / 8;
		long subChunk1Size = DEFAULT_SUBCHUNK1_SIZE;

		dos.writeBytes(riffString);
		dos.write(DataConversionUtil.intToByteArray((int) chunkSize), 0, 4);
		dos.writeBytes(formatString);
		dos.writeBytes(subChunk1String);
		dos.write(DataConversionUtil.intToByteArray((int) subChunk1Size), 0, 4);
		dos.write(DataConversionUtil.shortToByteArray((short) headerRef.getAudioFormat()), 0, 2);
		dos.write(DataConversionUtil.shortToByteArray((short) headerRef.getNumChannels()), 0, 2);
		dos.write(DataConversionUtil.intToByteArray((int) headerRef.getSamplingRate()), 0, 4);
		dos.write(DataConversionUtil.intToByteArray((int) byteRate), 0, 4);
		dos.write(DataConversionUtil.shortToByteArray((short) blockAlign), 0, 2);
		dos.write(DataConversionUtil.shortToByteArray((short) headerRef.getBitsPerSample()), 0, 2);
		dos.writeBytes(subChunk2String);
		dos.write(DataConversionUtil.intToByteArray((int) headerRef.getSubChunk2Size()), 0, 4);
		dos.flush();
		numSamplesWritten = 0;
	}

	@Override
	public void sendFrame(StreamFrame m) throws Exception {
		short[] audioData = ((RawAudioFrame) m).getAudioData();
		for (int i = 0; i < audioData.length; i++) {
			dos.write((DataConversionUtil.shortToByteArray(audioData[i])), 0, 2);
			numSamplesWritten += 2;
			if ((numSamplesWritten) >= headerRef.getSubChunk2Size() - 1) {
				break;
			}
		}
		dos.flush();
	}

	@Override
	public void close() {
		try {
			if (dis != null) {
				dis.close();
				dis = null;
			}
			if (dos != null) {
				dos.close();
				dos = null;
			}
			if (os != null) {
				os.close();
				os = null;
			}
			if (is != null) {
				is.close();
				is = null;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws Exception {
		int arg = 0;
		String outArg = args[arg++];
		String inArg = args[arg++];
		if (args.length != arg) {
			throw new IllegalArgumentException("Invalid number of arguments");
		}

		System.out.println("AudioTest: " + inArg + " to " + outArg);
		RawAudioFileStream aIn = new RawAudioFileStream(inArg);
		RawAudioFileStream aOut = new RawAudioFileStream(outArg, true);

		aOut.setHeader(aIn.getHeader());
		RawAudioFrame frame;
		while ((frame = (RawAudioFrame) aIn.recvFrame()) != null) {
			// frame.audioData[0] = Short.MIN_VALUE;
			// frame.audioData[frame.audioData.length-1] = Short.MAX_VALUE;
			aOut.sendFrame(frame);
		}
		aOut.close();
	}
}
