package com.google.cmusv.pandaa.audio;

import java.io.Serializable;

public interface FrameStream {

	public void setHeader(Header h);
	public Header getHeader();

	public void sendMessage(Frame m); // should be non-blocking
	public Frame recvMessage(); // will block until ready

	class Frame implements Serializable {
		Integer seqNum; // automatically set by sendMsg
	}

	class Header implements Serializable {
		long startTime; // client start time, ala System.currentTimeMillis()
		long frameTime; // duration of each frame, measured in ms
	}
	
}