package edu.cmu.pandaa.stream;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;

import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.GeometryHeader.GeometryFrame;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;

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
    writeString(header.deviceIds + " " + header.startTime + " " + header.frameTime);
	  /*if (oos != null) {
			throw new RuntimeException("setHeader called twice!");
		}
		oos = new ObjectOutputStream(os);
		oos.writeObject(h);
		oos.flush();*/
  }

  public void sendFrame(StreamFrame f) throws Exception {
    GeometryFrame frame = (GeometryFrame) f;
    nextFile();  // I wouldn't actually recommend this for ImpulseFileStream, but doing it as a demonstraiton
    String msg = "" + frame.seqNum;
      
    for (int i = 0;i < frame.geometry.length; i++) {
    	for (int j = 0;j < frame.geometry[i].length; j++) {
    		msg += " " + frame.geometry[i][j];
    	}
    	writeString(msg); //writing each row 
    	msg = "";
    }    
  }

  public GeometryHeader getHeader() throws Exception {
    String line = readLine();
    String[] parts = line.split(" ");
    String[] deviceIds = parts[0].split(","); //extract the array of deviceIds
    header = new GeometryHeader(deviceIds,Long.parseLong(parts[1]),Integer.parseInt(parts[2]));
    return header;
  }

  public GeometryFrame recvFrame() throws Exception {
    nextFile();  // I wouldn't actually recommend this for ImpulseFileStream, but doing it as a demonstraiton
	//InputStream is = new FileInputStream("/test/gIn_0.txt");
	String line = readLine();
    int k =0;
    String[] parts = line.split(" ");
    int size = (int) Math.sqrt(parts.length-1);
    //int size = (parts.length-1);
    int seqNum = Integer.parseInt(parts[0]);
    /*construct frame*/
    double[][] geometry = new double[size][size]; //initialize rows, cols using 'size'
    for (int i = 0;i < size;i++) {
    	k = i*(size-1) ;
    	for (int j = 0;j < size;j++) {
    		geometry[i][j] = Double.parseDouble(parts[ i + j + k + 1]);
    	}    	    	
    }
    return header.makeFrame(seqNum, geometry);
  }

  /*public static void main(String[] args) throws Exception {
    String filename = "test.txt";
    String[] deviceIds = {"1a","2b","3c","4d"};

    double[][] inputDissimilarity = {{0,1,2,3},
    								{4,5,6,7},
    								{8,9,10,11},
    								{12,13,14,15}};
    
    GeometryFileStream foo = new GeometryFileStream(filename, true);
    
    GeometryHeader header = new GeometryHeader(deviceIds, System.currentTimeMillis(), 100);
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
  }*/
}