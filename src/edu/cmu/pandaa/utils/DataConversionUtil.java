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

	public static final short byteArrayToShort(byte[] b) {
		short value = 0;
		for (int i = 0; i < b.length; i++) {
			value += (b[i] & 0xff) << (8 * i);
		}
		return value;
	}

  public static final short[] byteArrayToShortArray(byte[] bArray) {
    return byteArrayToShortArray(bArray, bArray.length);
  }

	public static final short[] byteArrayToShortArray(byte[] bArray, int size) {
		int numShorts = (int) Math.ceil(size) / 2;
		int nextByteIndex = 0;
		short[] shortArray = new short[numShorts];
		byte[] dataBytes = new byte[2];
		for (int i = 0; i < numShorts; i++) {
			if (nextByteIndex >= bArray.length)
				break;
			else {
				dataBytes[0] = bArray[nextByteIndex];
				nextByteIndex++;
			}
			if (nextByteIndex >= bArray.length)
				dataBytes[1] = 0;
			else
				dataBytes[1] = bArray[nextByteIndex];
			nextByteIndex++;
			shortArray[i] = byteArrayToShort(dataBytes);
		}

		return shortArray;
	}
	
	public static final byte[] shortArrayToByteArray(short[] sArray) {
		byte[] byteArray = null;
		int byteArrayIndex = 0;
		if(sArray.length >0)
			byteArray = new byte[sArray.length * 2];
		for(int i=0;i<sArray.length;i++,byteArrayIndex+=2) {
			byte[] audioByte = shortToByteArray(sArray[i]);
			byteArray[byteArrayIndex] = audioByte[0];
			byteArray[byteArrayIndex + 1] = audioByte[1];
		}
		return byteArray;
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
	
	public static int shortToUnsignedShortVal(short val) {
	     return ((int)val & 0xFFFF) ;
	}
}
