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
import java.util.UnknownFormatConversionException;

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
  private long writtenSeqNum = -1, lastReadSeqNum = -1;
  private boolean prefetched;

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
    return readLine(false);
  }

  protected String readLine(boolean tryPrefetch) throws IOException {
    if (is == null)
      return null;

    if (br == null) {
      br = new BufferedReader(new InputStreamReader(is));
    }

    if (prefetched) {
      prefetched = false;
      return inputLine;
    }

    do {
      inputLine = br.readLine();
    } while (inputLine != null && inputLine.trim().startsWith("#"));

    if (tryPrefetch) {
      String prefix = lastReadSeqNum + " ";
      if (inputLine != null && inputLine.startsWith(prefix)) {
        return inputLine.substring(prefix.length());
      } else {
        prefetched = true;
        return null;
      }
    }

    return inputLine;
  }

  public void setFormatJson(boolean asJson) {
    this.asJosn = asJson;
  }

  public void setOutputStream(OutputStream os) {
    pw = new PrintWriter(os);
  }

  public boolean isOutputStream() {
    return pw != null;
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

  protected void writeValue(String id, int value) {
    writeValue(id, "" + value);
  }

  protected void writeValue(String id, short value) {
    writeValue(id, "" + value);
  }

  protected void writeValue(String id, long value) {
    writeValue(id, "" + value);
  }

  protected void writeValue(String id, double value) {
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

  protected void writeArray(String name) {
    if (inArray) {
      throw new IllegalArgumentException("Can't have directly nested arrays");
    }
    inArray = true;
    if (asJosn) {
      pw.print((first ? "" : ", "));
      pw.print("\"" + name + "\": [ ");
    } else if (!first) {
      pw.print(" ");
    }
    first = true;
  }

  protected void writeEndArray() {
    if (asJosn) {
      pw.print(inArray ? " ] " : " } ] ");
    }
    first = true;
    inArray = false;
  }

  protected void writeArrayObject() {
    if (asJosn) {
      pw.print(inArray ? " { " : " }, { ");
    } else if (!first) {
      pw.print("\n" + writtenSeqNum + " ");
    }
    inArray = false;
    first = true;
  }

  protected void writeValue(String id, String value) {
    if (asJosn) {
      pw.print((first ? "" : ", "));
      if (id == null) {
        if (!inArray) {
          throw new IllegalArgumentException("Array value not in array");
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
    pw.flush();
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
    writeValue("startTime", h.startTime);
    writeValue("frameTime", h.frameTime);
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

    pw.flush();  // can't easily flush after we write something, so flush the previous frame

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

    if (m.seqNum < 0 && !asJosn) {
      writeValue("seqNum", "###");
    } else {
      writeValue("seqNum", m.seqNum);
    }
    writtenSeqNum = m.seqNum;
  }

  @Override
  public StreamHeader getHeader() throws Exception {
    if (ois != null) {
      return (StreamHeader) ois.readObject();
    }
    lastLine = readLine();
    try {
      String targetClass = consumeString();
      prototypeHeader = new StreamHeader(consumeString(), consumeInt(), consumeInt(), consumeInt(), targetClass);
      return prototypeHeader;
    } catch (Exception e) {
      System.out.println("While processing inputline: " + inputLine);
      throw e;
    }
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
    try {
      int seqNum = consumeInt();
      if (seqNum <= lastReadSeqNum) {
        throw new IllegalArgumentException("Sequence numbers not advancing");
      }
      lastReadSeqNum = seqNum;
      return prototypeHeader.makeFrame(seqNum);
    } catch (NumberFormatException e) {
      System.out.println("Reached end of input file");
      return null;
    }
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
      System.out.println("While processing input line: "+ inputLine);
      throw e;
    }
  }

  protected boolean hasMoreData()  throws IOException {
    boolean hasMore = lastLine != null && lastLine.trim().length() > 0;

    if (!hasMore) {
      String newLine = readLine(true);
      if (newLine != null) {
        lastLine = newLine.trim();
        hasMore = true;
      }
    }

    return hasMore;
  }

  protected int consumeInt() {
    String string = consumeString();
    if (string.contains("###")) {
      System.out.println("foobar");
    }
    return Integer.parseInt(string);
  }

  protected double consumeDouble() {
    return Double.parseDouble(consumeString());
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
      System.out.println("Start time mismatch!");
    }
    if (frame1.seqNum != frame2.seqNum-2) {
      System.out.println("Sequence number mismatch!");
    }
  }
}