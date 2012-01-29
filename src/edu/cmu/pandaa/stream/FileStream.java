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
  private OutputStream os;
  private InputStream is;
  private BufferedReader br;
  private PrintWriter pw;
  protected ObjectOutputStream oos;
  protected ObjectInputStream ois;
  private int fileSeqNum = 0;
  protected final String fileName;
  final String padding = "0000";
  private boolean asJosn;
  boolean useMultipleFiles = false;
  private boolean first;
  private boolean inArray;
  private String lastLine, inputLine;
  private StreamHeader prototypeHeader;
  private int frameCount = 0;

  public FileStream() {
    fileName = null;
  }

  public FileStream(String fileName) throws IOException {
    this.fileName = fileName;
    is = new FileInputStream(fileName);
    os = null;
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
    if (pw != null && asJosn) {
      if (frameCount == 0) {
        pw.println("},");
        pw.print("\"frames\": [ ");
      }  else {
        pw.print((inArray ? "]" : "}" ) + "} ");
      }
      pw.println("] }");
    }
    if (lastLine != null) {
      throw new RuntimeException("Extra data left on line: " + lastLine);
    }
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

  protected boolean nextFile() throws IOException {
    boolean isRead = os == null;
    close();
    int mark = fileName.lastIndexOf('.');
    String nextFile = fileName.substring(0,mark);
    String seqNumStr = "" + fileSeqNum;
    if (!isRead)
      new File(nextFile).mkdir();
    nextFile = nextFile + File.separatorChar + padding.substring(0,padding.length() - seqNumStr.length()) +
            seqNumStr + fileName.substring(mark);
    is = null;
    os = null;
    if (isRead && !new File(nextFile).exists())
      return false;
    if (isRead) {
      is = new FileInputStream(nextFile);
    } else {
      os = new FileOutputStream(nextFile);
      ensureOutputStreams();
    }
    fileSeqNum++;
    return true;
  }

  protected String readLine() throws IOException {
    if (is == null)
      return null;

    if (br == null) {
      br = new BufferedReader(new InputStreamReader(is));
    }

    inputLine = br.readLine();
    return inputLine;
  }

  public void setFormatJson(boolean asJson) {
    this.asJosn = asJson;
  }

  public void setOutputStream(OutputStream os) {
    pw = new PrintWriter(os);
  }

  public void createObjectStreams() throws Exception {
    if (oos != null) {
      throw new RuntimeException("setHeader called twice!");
    }
    oos = new ObjectOutputStream(os);

    if (ois != null) {
      throw new RuntimeException("getHeader called twice!");
    }
    ois = new ObjectInputStream(is);

  }

  protected void writeValue(String id, long value) {
    writeValue(id, "" + value);
  }

  protected void readFormatting() throws IOException {
    if (useMultipleFiles) {
      if (lastLine != null) {
        throw new RuntimeException("Extra data left over");
      }
      lastLine = readLine();
      if (lastLine != null)
        lastLine = lastLine.trim();
    }
  }

  protected void writeFormatting() {
    if (useMultipleFiles) {
      pw.println();
      first = true;
    } else {
      pw.print("    ");
    }
  }

  protected void writeValue(String id, String value) {
    if (asJosn) {
      pw.print((first ? "" : ", "));
      if (id == null) {
        if (!inArray) {
          inArray = true;
          pw.print("\"data\": [ ");
        }
      } else {
        pw.print("\"" + id + "\": ");
      }
      pw.print("\"" + value + "\"");
    } else if (value == null || !value.contains(" "))
      pw.print((first ? "" : " ") + value);
    else
      throw new RuntimeException("Invalid output value");
    first = false;
  }

  protected void writeString(String out) {
    if (pw == null) {
      pw = new PrintWriter(os);
    }
    pw.println(out);
    first = true;
  }

  private void ensureOutputStreams() {
    if (pw == null) {
      pw = new PrintWriter(os);
    }
    first = true;
  }

  @Override
  public void setHeader(StreamHeader h) throws Exception {
    if (oos != null) {
      oos.writeObject(h);
      oos.flush();
      return;
    }

    ensureOutputStreams();
    if (asJosn) {
      pw.println("{ \"header\": {");
    }
    writeValue("type", h.getClass().getSimpleName());
    writeValue("id", h.id);
    writeValue("frameTime", h.frameTime);
    writeValue("startTime", h.startTime);
    writeValue("nextSeq", h.nextSeq);

  }

  @Override
  public void sendFrame(StreamFrame m) throws Exception {
    if (m == null) {
      return;
    }

    if (oos != null) {
      oos.writeObject(m);
      oos.flush();
      return;
    }

    frameCount++;

    if (asJosn) {
      pw.println("},");
      pw.print("\"frames\": [ { ");
    }

    if (useMultipleFiles) {
      nextFile();
    } else {
      pw.println();
      first = true;
    }

    writeValue("seqNum", m.seqNum);
  }

  @Override
  public StreamHeader getHeader() throws Exception {
    if (ois != null) {
      return (StreamHeader) ois.readObject();
    }
    lastLine = readLine();
    String targetClass = consumeString();
    prototypeHeader = new StreamHeader(consumeString(), consumeInt(), consumeInt(), consumeInt(), targetClass);
    return prototypeHeader;
  }

  @Override
  public StreamFrame recvFrame() throws Exception {
    if (ois != null) {
      return (StreamFrame) ois.readObject();
    }
    if (useMultipleFiles)
      nextFile();
    lastLine = readLine();
    if (lastLine == null) {
      return null;
    }
    lastLine = lastLine.trim();
    return prototypeHeader.makeFrame(consumeInt());
  }

  protected String consumeString() {
    try {
      int mark = lastLine.indexOf(' ');
      String part;
      if (mark >= 0) {
        part = lastLine.substring(0,mark);
        lastLine = lastLine.substring(mark+1).trim();
      } else {
        part = lastLine;
        lastLine = null;
      }
      return part;
    } catch (RuntimeException e) {
      System.err.println("While processing input line: "+ inputLine);
      throw e;
    }
  }

  protected int consumeInt() {
    return Integer.parseInt(consumeString());
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