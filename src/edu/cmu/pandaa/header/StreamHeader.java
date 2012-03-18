package edu.cmu.pandaa.header;

import edu.cmu.pandaa.framework.App;
import edu.cmu.pandaa.stream.FileStream;

import java.io.Serializable;

public class StreamHeader implements Serializable {
  public final String id; // device ID (hostname, IP address, whatever)
  public final long startTime; // client start time, ala System.currentTimeMillis()
  public final int frameTime;  // duration of each frame, measured in ms
  public int nextSeq; // next sequence number to use by frame constructor
  private final String targetClass;

  public StreamHeader(StreamHeader prototype) {
    this.id = prototype.getMetaId();
    this.startTime = prototype.startTime;
    this.frameTime = prototype.frameTime;
    this.targetClass = prototype.targetClass;
    this.nextSeq = prototype.nextSeq;
    if (targetClass != null && !this.getClass().getSimpleName().equals(targetClass)) {
      throw new RuntimeException("Mismatching target class");
    }
  }

  protected String getMetaId() {
    return id;
  }

  public StreamHeader(String id, long startTime, int frameTime) {
    this(id, startTime, frameTime, 0, null);
  }

  public StreamHeader(String id, long startTime, int frameTime, int nextSeq, String targetClass) {
    if (id == null || id.trim().equals("") || id.indexOf(" ") >= 0) {
      throw new IllegalArgumentException("ID can not be null/empty");
    }
    if (frameTime <= 0) {
      throw new IllegalArgumentException("frameTime must be > 0");
    }
    this.id = id;
    this.startTime = startTime;
    this.frameTime = frameTime;
    this.targetClass = targetClass;
    this.nextSeq = nextSeq;
  }

  public class StreamFrame implements Serializable {
    public final int seqNum;

    public StreamFrame() {
      seqNum = nextSeq++;
    }

    public StreamFrame(int seq) {
      seqNum = seq;
    }

    public StreamFrame(StreamFrame prototype) {
      seqNum = prototype.seqNum;
    }

    public StreamHeader getHeader() {
      return StreamHeader.this;
    }

    public String toString() {
      return id + "#" + seqNum;
    }

    public long getStartTime() {
      return startTime + frameTime * seqNum;
    }
  }

  public long getNextFrameTime() {
    return startTime + frameTime * nextSeq;
  }

  public StreamFrame makeFrame() {
    return new StreamFrame();
  }

  public StreamFrame makeFrame(int seqNum) {
    return new StreamFrame(seqNum);
  }

  public static String makeId(String base,String[] ids) {
    String nid = ids[0];
    for (int i = 1;i < ids.length;i++) {
      nid = App.combineIds(nid,ids[i]);
    }
    return nid;
  }

  public String[] getIds() {
    int list = id.indexOf(',');
    int prefix = id.substring(0, list).lastIndexOf('-');
    int postfix = id.indexOf('_', list);
    String lstr = id.substring(prefix + 1, postfix);
    String[] parts = lstr.split(",");
    for (int i = 0; i < parts.length; i++) {
      parts[i] = id.substring(0, prefix+1) + parts[i] + id.substring(postfix);
    }
    return parts;
  }

  public FileStream createOutput() throws Exception {
    throw new RuntimeException("createOutput not implemented for " + getClass());
  }
}

