package edu.cmu.pandaa.shared.stream;

import edu.cmu.pandaa.shared.stream.header.GeometryHeader;
import edu.cmu.pandaa.shared.stream.header.StreamHeader;
import edu.cmu.pandaa.shared.stream.header.GeometryHeader.GeometryFrame;
import edu.cmu.pandaa.shared.stream.header.StreamHeader.StreamFrame;

public class GeometryFileStream extends FileStream {
  private GeometryHeader header;

  public GeometryFileStream(String filename) throws Exception {
    super(filename);
  }

  public GeometryFileStream(String filename, boolean overwrite) throws Exception {
    super(filename, overwrite);
  }

  public void setHeader(StreamHeader h) throws Exception {
    GeometryHeader header = (GeometryHeader) h;
    writeString(header.id + " " + header.startTime + " " + header.frameTime);
  }

  public void sendFrame(StreamFrame f) throws Exception {
    GeometryFrame frame = (GeometryFrame) f;
    nextFile();  // I wouldn't actually recommend this for ImpulseFileStream, but doing it as a demonstraiton
    String msg = "" + frame.seqNum;
    
    //TODO: construct the msg using the GeometryFrame object 
        
    for (int i = 0;i < frame.geometry.length; i++) {
    	for (int j = 0;i < frame.geometry[0].length; j++) {
    		msg += " " + frame.geometry[i][j];
    	}
    }
    writeString(msg);
  }

  public GeometryHeader getHeader() throws Exception {
    String line = readLine();
    String[] parts = line.split(" ");
    header = new GeometryHeader(parts[0],Long.parseLong(parts[1]),Integer.parseInt(parts[2]));
    return header;
  }

  public GeometryFrame recvFrame() throws Exception {
    nextFile();  // I wouldn't actually recommend this for ImpulseFileStream, but doing it as a demonstraiton
    String line = readLine();
    String[] parts = line.split(" ");
    int size = (parts.length-1)/2;
    int seqNum = Integer.parseInt(parts[0]);
    //TODO: construct the frame
    double[][] geometry = new double[size][size]; //initialize rows, cols using 'size'
    //unsure after this step
    
    return header.makeFrame(seqNum, geometry);
  }

  public static void main(String[] args) throws Exception {
    String filename = "test.txt";

    double[][] inputDissimilarity = {{0.00,1.00,10.19803903,10.44030651,6.403124237},
    								{1.00,0.00,10.04987562,10.19803903,5.830951895},
    								{10.19803903,10.04987562,0,1,5.385164807},
    								{10.44030651,10.19803903,1,0,5.099019514},
    								{6.403124237,5.830951895,5.385164807,5.099019514,0}};
    
    GeometryFileStream foo = new GeometryFileStream(filename, true);
    GeometryHeader header = new GeometryHeader("w00t", System.currentTimeMillis(), 100);
    foo.setHeader(header);
    GeometryFrame frame1 = header.makeFrame(inputDissimilarity);
    foo.sendFrame(frame1);
    foo.sendFrame(header.makeFrame(inputDissimilarity));
    foo.sendFrame(header.makeFrame(inputDissimilarity));
    foo.close();

    Thread.sleep(100);  // make sure start times are different

    foo = new GeometryFileStream(filename);
    GeometryHeader header2 = foo.getHeader();
    GeometryFrame frame2 = foo.recvFrame();
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

