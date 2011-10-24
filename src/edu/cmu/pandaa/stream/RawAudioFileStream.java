package edu.cmu.pandaa.stream;

import java.io.DataInputStream;
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
	ObjectOutputStream oos;
	ObjectInputStream ois = null;
	private final String fileName;
	RawAudioHeader rawAudioHeaderRef;

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
		dis = new DataInputStream(new FileInputStream(fileName));
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

		rawAudioHeaderRef = new RawAudioHeader(System.currentTimeMillis(), wavFrameLength,
				wavFormat, wavChannels, wavSamplingRate, wavBitsPerSample, wavDataSize);
		return rawAudioHeaderRef;
	}

	@Override
	public StreamFrame recvFrame() throws Exception {
		RawAudioFrame rawAudioFrame = rawAudioHeaderRef.makeFrame(wavFrameLength);
		byte[] audioData = new byte[wavFrameLength];
		int bytesRead = dis.read(audioData);
		for (int i = 0; i < audioData.length; i++) {
			rawAudioFrame.audioData[i] = audioData[i];
		}
		if (bytesRead <= 0)
			return null;
		else
			return rawAudioFrame;
	}

	@Override
	public void setHeader(StreamHeader h) throws Exception {
		if (oos != null) {
			throw new RuntimeException("setHeader called twice!");
		}
		oos = new ObjectOutputStream(os);
		oos.writeObject(h);
		oos.flush();
	}

	@Override
	public void sendFrame(StreamFrame m) throws Exception {
		oos.writeObject(m);
		oos.flush();
	}

	@Override
	public void close() {
		try {
			if (dis != null) {
				dis.close();
				dis = null;
			}
			if (oos != null) {
				oos.close();
				oos = null;
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

	public RawAudioHeader getHeaderFromFrameFormat() throws Exception {
		if (ois != null) {
			throw new RuntimeException("getHeader called twice!");
		}
		ois = new ObjectInputStream(is);
		return (RawAudioHeader)ois.readObject();
	}

	public StreamFrame recvFrameFromFrameFormat() throws Exception {
		return (StreamFrame) ois.readObject();
	}

	public boolean saveInFrameFormat(WavUtil wavUtil) {
		try {
			RawAudioHeader rawAudioHeader = new RawAudioHeader(System.currentTimeMillis(),
					RawAudioHeader.DEFAULT_FRAMETIME, wavUtil.getFormat(),
					wavUtil.getNumChannels(), wavUtil.getSamplingRate(),
					wavUtil.getBitsPerSample(), wavUtil.getDataSize());
			System.out.println(rawAudioHeader);
			setHeader(rawAudioHeader);

			int frameLength = (int) (wavUtil.getSamplingRate() / 1000)
					* RawAudioHeader.DEFAULT_FRAMETIME;

			RawAudioFrame audioFrame = null;
			int audioIndex = 0;
			for (int idxBuffer = 0; idxBuffer < wavUtil.getDataSize(); ++idxBuffer) {

				// When a frame is full send it to the stream and create a new
				// frame
				if (audioFrame == null || audioIndex >= frameLength) {
					if (audioFrame != null) {
						try {
							sendFrame(audioFrame);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					audioFrame = rawAudioHeader.makeFrame(frameLength);
					audioIndex = 0;
				}
				audioFrame.audioData[audioIndex++] = wavUtil.audioData[idxBuffer];
			}
			if (audioIndex > 0) {
				try {
					sendFrame(audioFrame);
				} catch (Exception e) {
					e.printStackTrace();
				}
				audioIndex = 0;
				audioFrame = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public WavUtil readFromFrameFormat() {
		WavUtil wavUtil = null;
		try {
			RawAudioHeader audioHeader = (RawAudioHeader) getHeaderFromFrameFormat();
			System.out.println("Reading from frame file:\n" + audioHeader);
			wavUtil = new WavUtil(audioHeader.getAudioFormat(), audioHeader.getNumChannels(),
					audioHeader.getSamplingRate(), audioHeader.getBitsPerSample(),
					audioHeader.getSubChunk2Size());
			RawAudioFrame audioFrame = null;

			long wavDataSize = audioHeader.getSubChunk2Size();
			wavUtil.audioData = new byte[(int) wavDataSize];
			int numSamples = 0;
			boolean dataComplete = false;
			try {
				while ((audioFrame = (RawAudioFrame) recvFrameFromFrameFormat()) != null) {
					short[] data = audioFrame.getAudioData();
					for (int i = 0; i < data.length; i++) {
						wavUtil.audioData[numSamples] = (byte) data[i];
						if (numSamples++ == wavDataSize - 1) {
							dataComplete = true;
							break;
						}
					}
					if (dataComplete)
						break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return wavUtil;
	}
}
