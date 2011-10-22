package edu.cmu.pandaa.stream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;

public class FileStream implements FrameStream {
  protected OutputStream os;
  protected InputStream is;
  private BufferedReader br;
  private PrintWriter pw;
  private ObjectOutputStream oos;
  private ObjectInputStream ois;
  private int seqNum = 0;
  protected final String fileName;

  public FileStream(String fileName) throws IOException {
    this.fileName = fileName;
    is = new FileInputStream(fileName);
  }

  public FileStream(String fileName, boolean overwrite) throws IOException {
    this.fileName = fileName;
    File file = new File(fileName);
    if (file.exists() && !overwrite) {
      throw new IOException("File exists");
    }
    os = new FileOutputStream(file);
  }

  @Override
  public void close() {
    try {
      if (ois != null) {
        ois.close();
        ois = null;
      }
      if (oos != null) {
        oos.close();
        oos = null;
      }
      if (br != null) {
        br.close();
        br = null;
      }
      if (pw != null) {
        pw.close();
        pw = null;
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

  protected void nextFile() throws IOException {
    boolean isRead = is != null;
    close();
    int mark = fileName.lastIndexOf('.');
    String nextFile = fileName.substring(0,mark) + "_" + (seqNum++) + fileName.substring(mark);
    if (isRead) {
      is = new FileInputStream(nextFile);
    } else {
      os = new FileOutputStream(nextFile);
    }
  }

  protected String readLine() throws IOException {
    if (br == null) {
      br = new BufferedReader(new InputStreamReader(is));
    }
    return br.readLine();
  }

  protected void writeString(String out) {
    if (pw == null) {
      pw = new PrintWriter(os);
    }
    pw.println(out);
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
  public StreamHeader getHeader() throws Exception {
    if (ois != null) {
      throw new RuntimeException("getHeader called twice!");
    }
    ois = new ObjectInputStream(is);
    return (StreamHeader) ois.readObject();
  }

  @Override
  public StreamFrame recvFrame() throws Exception {
    return (StreamFrame) ois.readObject();
  }

  public static void main(String[] args) throws Exception {
    String filename = "test.obj";

    FileStream foo = new FileStream(filename, true);
    StreamHeader header = new StreamHeader("w00t", System.currentTimeMillis(), 100);
    foo.setHeader(header);
    StreamFrame frame1 = header.makeFrame();
    foo.sendFrame(frame1);
    foo.sendFrame(header.makeFrame());
    foo.sendFrame(header.makeFrame());
    foo.close();

    Thread.sleep(100);  // make sure start times are different

    foo = new FileStream(filename);
    StreamHeader header2 = foo.getHeader();
    StreamFrame frame2 = foo.recvFrame();
    frame2 = foo.recvFrame();
    frame2 = foo.recvFrame();
    foo.close();

    if (frame1.getHeader().startTime != frame2.getHeader().startTime) {
      System.err.println("Start time mismatch!");
    }
    if (frame1.seqNum != frame2.seqNum-2) {
      System.err.println("Sequence number mismatch!");
    }
  }
}