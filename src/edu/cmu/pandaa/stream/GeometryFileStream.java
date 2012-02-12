package edu.cmu.pandaa.stream;

import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.GeometryHeader.GeometryFrame;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;

import java.io.File;

public class GeometryFileStream extends FileStream {
  private GeometryHeader header;

  public GeometryFileStream() throws Exception {
  }

  public GeometryFileStream(String filename) throws Exception {
    super(filename);
    int dot = filename.lastIndexOf('.');
    useMultipleFiles = new File(filename.substring(0, dot)).isDirectory();
  }

  public GeometryFileStream(String filename, boolean overwrite) throws Exception {
    this(filename, overwrite, false);
  }

  public GeometryFileStream(String filename, boolean overwrite, boolean multiple) throws Exception {
    super(filename, overwrite);
    useMultipleFiles = multiple;
  }

  public void setHeader(StreamHeader h) throws Exception {
    super.setHeader(h);
    GeometryHeader header = (GeometryHeader) h;
    writeValue("rows", header.rows);
    writeValue("cols", header.cols);
  }

  public void sendFrame(StreamFrame f) throws Exception {
    if (f == null) {
      return;
    }

    super.sendFrame(f);

    GeometryFrame frame = (GeometryFrame) f;
    int rows = frame.geometry == null ? 0 : frame.geometry.length;
    int cols = rows == 0 ? rows : frame.geometry[0].length;
    String msg = "";
    for (int j = 0; j < cols; j++) {
      writeFormatting();
      writeArray("data");
      for (int i = 0;i < rows; i++) {
        double val = frame.geometry[i][j];
        // simple way to keep the numbers reasonable (not too much precision)
        // really ony to make it visually look better...
        val = Math.floor(val*100.0)/100.0;
        writeValue(null, val);
      }
      writeEndArray();
    }
  }

  public GeometryHeader getHeader() throws Exception {
    StreamHeader prototype = super.getHeader();
    header = new GeometryHeader(prototype, consumeInt(), consumeInt());
    return header;
  }

  public GeometryFrame recvFrame() throws Exception {
    StreamFrame prototype = super.recvFrame();
    if (prototype == null)
      return null;
    int rows = header.rows;
    int cols = header.cols;
    double[][] geometry = new double[cols][rows];
    for (int j = 0; j < rows; j++) {
      readFormatting();
      for (int i = 0; i < cols; i++) {
        geometry[i][j] = Double.parseDouble(consumeString());
      }
    }
    return header.makeFrame(prototype, geometry);
  }

  public static void main(String args[]) throws Exception {
    String[] dids = { "test" };
    double[][] geometry = { { 1.0 } };
    GeometryHeader gh = new GeometryHeader(dids, 0, 100, 1, 1);
    GeometryFileStream gstream = new GeometryFileStream("gtest1.txt", true, false);
    gstream.setHeader(gh);
    gstream.sendFrame(gh.makeFrame(geometry));
    gstream.close();
    gstream = new GeometryFileStream("gtest2.txt", true, true);
    gstream.setHeader(gh);
    gstream.sendFrame(gh.makeFrame(geometry));
    gstream.close();

    gstream = new GeometryFileStream("gtest1.txt");
    if (gstream.useMultipleFiles)
      System.out.println("Should not be using multiple files!");
    gstream.close();
    gstream = new GeometryFileStream("gtest2.txt");
    if (!gstream.useMultipleFiles)
      System.out.println("Should be using multiple files!");
    gstream.close();
  }
}