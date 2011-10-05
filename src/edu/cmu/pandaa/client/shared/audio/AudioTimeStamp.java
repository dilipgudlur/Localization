package edu.cmu.pandaa.client.shared.audio;

import android.os.SystemClock;

public class AudioTimeStamp {

	public static long getCurrentTime() {
		long startTime = 0;
		SntpClient sntpClient = new SntpClient();
		if (sntpClient.requestTime("pool.ntp.org", 1000)) {
			startTime = sntpClient.getNtpTime()
					+ SystemClock.elapsedRealtime()
					- sntpClient.getNtpTimeReference();
		}
		return startTime;
	}
}
