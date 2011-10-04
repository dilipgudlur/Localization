package edu.cmu.pandaa.shared.stream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketStream implements FrameStream {
  
  Header headerBuffer;
  Socket connection;
  ObjectOutputStream outObjectStream;
  ObjectInputStream inObjectStream;
  Object incomingMessage;
  
  SocketStream(Socket connection) {
    this.connection = connection;
    
    try {
      this.outObjectStream = new ObjectOutputStream(connection.getOutputStream());
      this.inObjectStream = new ObjectInputStream(connection.getInputStream());
    }
    catch (IOException e) { e.printStackTrace(); }
  }
  
  public void setHeader(Header h) {
    headerBuffer = h;
    notify();   // if receiver is waiting for header, wake up
  }
  
  public Header getHeader() {
    if (headerBuffer == null) {
      try {
        wait();   // sleep until there's a header
      } 
      catch (InterruptedException e) { e.printStackTrace(); }
    }
    return headerBuffer;
  }
  
  public void sendFrame(Frame f) {
    try {
      outObjectStream.writeObject(f);
    }
    catch (IOException e) { System.out.println("Error sending message."); e.printStackTrace(); }
  }
  
  public Frame recvFrame() {
    try {
      incomingMessage = inObjectStream.readObject();
      
      if (incomingMessage instanceof Frame) {
        return (Frame) incomingMessage;
      }
      else if (incomingMessage instanceof Header) {
        this.headerBuffer = (Header) incomingMessage;
      }
      return recvFrame();   // return next Message if this one wasn't a Frame
    }
    catch (Exception e) { e.printStackTrace(); return null; }
  }
}