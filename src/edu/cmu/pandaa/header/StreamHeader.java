package edu.cmu.pandaa.header;

import java.io.Serializable;

public class StreamHeader implements Serializable {
  public final String id; // device ID (hostname, IP address, whatever)
  public final long startTime; // client start time, ala System.currentTimeMillis()
  public final int frameTime;  // duration of each frame, measured in ms
  public int nextSeq; // next sequence number to use by frame constructor

  public StreamHeader(StreamHeader prototype) {
    this.id = prototype.id;
    this.startTime = prototype.startTime;
    this.frameTime = prototype.frameTime;
  }

  public StreamHeader(String id, long startTime, int frameTime) {
    if (id == null || id.trim().equals("") || id.indexOf(" ") >= 0) {
      throw new IllegalArgumentException("ID can not be null/empty");
    }
    this.id = id;
    this.startTime = startTime;
    this.frameTime = frameTime;
  }

  public class StreamFrame implements Serializable {
    public final int seqNum;

    public StreamFrame() {
      seqNum = nextSeq++;
    }

    public StreamFrame(int seq) {
      seqNum = seq;
    }

    public StreamHeader getHeader() {
      return StreamHeader.this;
    }

    public String toString() {
      return id + "#" + seqNum;
    }
  }

  public StreamFrame makeFrame() {
    return new StreamFrame();
  }
}

