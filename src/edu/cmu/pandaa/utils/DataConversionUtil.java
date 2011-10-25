package edu.cmu.pandaa.utils;

public class DataConversionUtil {
	// ===========================
	// CONVERT BYTES TO JAVA TYPES
	// ===========================
	public static long byteArrayToLong(byte[] b) {
		long value = 0;
		for (int i = 0; i < b.length; i++) {
			value += (b[i] & 0xff) << (8 * i);
		}
		return value;
	}

	public static final int byteArrayToInt(byte[] b) {
		int value = 0;
		for (int i = 0; i < b.length; i++) {
			value += (b[i] & 0xff) << (8 * i);
		}
		return value;
	}

	// convert a short to a byte array
	public static byte[] shortToByteArray(short data) {
		return new byte[] { (byte) (data & 0xff), (byte) ((data >>> 8) & 0xff) };
	}

	public static final byte[] intToByteArray(int value) {
		byte[] intBytes = new byte[4];
		for (int i = 0; i < intBytes.length; i++) {
			intBytes[i] = (byte) (value >>> (8 * (i)));
		}
		return intBytes;
	}

	public static byte[] longToByteArray(long value) {
		byte[] longBytes = new byte[4];
		for (int i = 0; i < longBytes.length; i++) {
			longBytes[i] = (byte) (value >>> (8 * (i)));
		}
		return longBytes;
	}
}
