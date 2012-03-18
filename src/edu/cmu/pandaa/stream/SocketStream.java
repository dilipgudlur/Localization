package edu.cmu.pandaa.stream;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;

public class SocketStream implements FrameStream {

  StreamHeader headerBuffer;
  Socket connection;
  ObjectOutputStream outObjectStream;
  ObjectInputStream inObjectStream;
  Object incomingMessage;

  public SocketStream(Socket connection) {
    this.connection = connection;

    try {
      this.outObjectStream = new ObjectOutputStream(connection.getOutputStream());
      this.inObjectStream = new ObjectInputStream(connection.getInputStream());
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
public void setHeader(StreamHeader h) {
    headerBuffer = h;
    sendObject(h);    // send header over network
    notify();         // if a thread is waiting for the header, wake it up
  }

  @Override
public StreamHeader getHeader() {
    if (headerBuffer == null) {
      try {
        wait();   // sleep until there's a header
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return headerBuffer;
  }

  @Override
public void sendFrame(StreamFrame f) {
    sendObject(f);    // send frame over network
  }

  @Override
public StreamFrame recvFrame() {
    try {
      incomingMessage = inObjectStream.readObject();

      if (incomingMessage instanceof StreamFrame) {
        return (StreamFrame) incomingMessage;
      }
      else if (incomingMessage instanceof StreamHeader) {
        setHeader((StreamHeader) incomingMessage);
      }
      return recvFrame();   // return next message if this one wasn't a frame
    }
    catch (EOFException e) {
      System.out.println("Connection closed by client.");
      e.printStackTrace();
      try { connection.close(); }
      catch (IOException ioex) { ioex.printStackTrace(); }
      return null;
    }
    catch (IOException e) {
      System.out.println("Error, connection closed abnormally."); e.printStackTrace(); return null;
    }
    catch (Exception e) {
      e.printStackTrace(); return null;
    }
  }

  private void sendObject(Object o) {
    try {
      outObjectStream.writeObject(o);
    }
    catch (IOException e) {
      System.out.println("Error sending message."); e.printStackTrace();
    }
  }

  @Override
  public void close() {
    try {
      connection.close();
      outObjectStream.close();
      inObjectStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}