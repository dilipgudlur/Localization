package edu.cmu.pandaa.stream;

import edu.cmu.pandaa.header.MatrixHeader;
import edu.cmu.pandaa.header.MatrixHeader.MatrixFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.io.File;

public class MatrixFileStream extends FileStream {
  private MatrixHeader header;
  boolean multiLine = false;

  public MatrixFileStream() throws Exception {
  }

  public MatrixFileStream(String filename) throws Exception {
    super(filename);
    int dot = filename.lastIndexOf('.');
    useMultipleFiles = new File(filename.substring(0, dot)).isDirectory();
  }

  public MatrixFileStream(String filename, boolean overwrite) throws Exception {
    this(filename, overwrite, false);
  }

  public MatrixFileStream(String filename, boolean overwrite, boolean multiple) throws Exception {
    super(filename, overwrite);
    useMultipleFiles = multiple;
  }

  public void setHeader(StreamHeader h) throws Exception {
    super.setHeader(h);
    MatrixHeader header = (MatrixHeader) h;
    writeValue("rows", header.rows);
    writeValue("cols", header.cols);
  }

  public void setMultiLine(boolean ml) {
    multiLine = ml;
  }

  public void sendFrame(StreamFrame f) throws Exception {
    if (f == null) {
      return;
    }
    MatrixFrame frame = (MatrixFrame) f;
    if (frame.data == null || frame.data.length == 0) {
      super.sendFrame(f);
      return;
    }
    int lines = multiLine ? frame.data[0].length : 1;
    int cols = multiLine ? 1 : frame.data[0].length;

    for (int l = 0; l < lines; l++) {
      super.sendFrame(f);
      int rows = frame.data.length;

      for (int j = 0; j < cols; j++) {
        writeFormatting();
        writeArray("data");
        for (int i = 0;i < rows; i++) {
          double val = frame.data[i][l + j];
          // simple way to keep the numbers reasonable (not too much precision)
          // really ony to make it visually look better...
          val = Math.floor(val*100.0)/100.0;
          writeValue(null, val);
        }
        writeEndArray();
      }
    }
  }

  public MatrixHeader getHeader() throws Exception {
    StreamHeader prototype = super.getHeader();
    header = new MatrixHeader(prototype, consumeInt(), consumeInt());
    return header;
  }

  public MatrixFrame recvFrame() throws Exception {
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
    double[][] matrix = { { 1.0 } };
    MatrixHeader mh = new MatrixHeader(dids, 0, 100, 1, 1);
    MatrixFileStream mstream = new MatrixFileStream("gtest1.txt", true, false);
    mstream.setHeader(mh);
    mstream.sendFrame(mh.makeFrame(matrix));
    mstream.close();
    mstream = new MatrixFileStream("gtest2.txt", true, true);
    mstream.setHeader(mh);
    mstream.sendFrame(mh.makeFrame(matrix));
    mstream.close();

    mstream = new MatrixFileStream("gtest1.txt");
    if (mstream.useMultipleFiles)
      System.out.println("Should not be using multiple files!");
    mstream.close();
    mstream = new MatrixFileStream("gtest2.txt");
    if (!mstream.useMultipleFiles)
      System.out.println("Should be using multiple files!");
    mstream.close();
  }
}