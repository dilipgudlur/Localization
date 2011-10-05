package edu.cmu.pandaa.shared.stream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;

import edu.cmu.pandaa.shared.stream.GenericFrame.Frame;
import edu.cmu.pandaa.shared.stream.GenericFrame.Header;

public class FileStream implements FrameStream {

  private File audioFile;
  private static int sequenceNum;
  ObjectOutputStream oos = null;
  ObjectInputStream ois = null;
  
  public FileStream(String fileName) {
    audioFile = new File(fileName);
    sequenceNum = 0;
    // Open output stream
    if (this.audioFile == null) {
      throw new IllegalStateException("fileName is null");
    }

    try {
      oos = new ObjectOutputStream(new FileOutputStream(this.audioFile));
      ois = new ObjectInputStream(new FileInputStream(this.audioFile));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public synchronized void setHeader(Header h) {
    try {
            oos.writeObject(h);
            oos.flush();
    } catch (FileNotFoundException e) {
      throw new IllegalStateException("Cannot Open File", e);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public synchronized Header getHeader() {
    Header audioHeader = null;
    try {
      audioHeader = (Header)ois.readObject();
    } catch (OptionalDataException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return audioHeader;
  }

  public synchronized void sendFrame(Frame m) throws IllegalStateException {
    try {
      m.seqNum = sequenceNum++;
            oos.writeObject(m);
            oos.flush();
    } catch (FileNotFoundException e) {
      throw new IllegalStateException("Cannot Open File", e);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public Frame recvFrame() {
    Frame audioFrame = null;
    try {
      audioFrame = (Frame)ois.readObject();
    } catch (OptionalDataException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return audioFrame;
  }
  
  public void setFile(File audioFile) {
    this.audioFile = audioFile;
  }

  public File getFile() {
    return audioFile;
  }
}