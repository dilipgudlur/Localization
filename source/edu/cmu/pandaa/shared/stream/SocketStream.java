package com.google.cmusv.pandaa.stream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.cmusv.pandaa.stream.FrameStream.NetworkFrameStream;

class SocketStream implements NetworkFrameStream {
  
  private Header headerBuffer;                                //TODO: sent every couple of frames (need a counter)
  private LinkedBlockingQueue<AddressedMessage> sendBuffer;   // outgoing queue (data and control messages, encapsulated along with destination host address)
  private LinkedBlockingQueue<Frame> recvBuffer;              // incoming queue of Frame objects (Header and String messages are handled immeditely at arrival)
  private Thread sendThread;                                  // concurrent thread sending messages/frames
  private Thread recvThread;                                  // concurrent thread receiving messages/frames
    
  public SocketStream() {    
    sendBuffer = new LinkedBlockingQueue<AddressedMessage>();
    recvBuffer = new LinkedBlockingQueue<Frame>();
    
    // send thread to monitor and send outgoing messages
    sendThread = new Thread(new FrameSender());
    sendThread.start();
    
    // receive thread to monitor and handle incoming messages
    recvThread = new Thread(new FrameReceiver());    
    recvThread.start();
  }
  
  public void setHeader(Header h) {
    headerBuffer = h;    
    //TODO: send header
  }
  
  public Header getHeader() {
    return headerBuffer;
  }
  
  public synchronized void sendFrame(Frame m, String ip, int port) {
    try {
      sendBuffer.put(new AddressedMessage(m, ip, port));
    }
    catch (InterruptedException e) { System.out.println(e); }
  }
  
  public synchronized Frame recvFrame() {
    try {
      return recvBuffer.take();  // retrieve frame from receive buffer (will block until one is available)
    }
    catch (InterruptedException e) { System.out.println(e); return null; }
  }
  
  
  // frame encapsulation in a message, along with destination address
  class AddressedMessage {
    Object message;   // can be a Frame (data), a Header (data), or a String (control)
    String destIP;
    int destPort;
    
    AddressedMessage(Object message, String destIP, int destPort) {
      this.message = message;
      this.destIP = destIP;
      this.destPort = destPort;
    }
  }
  
  class FrameSender implements Runnable { 
    
    AddressedMessage outgoing;
    Socket sendConnection;
    ObjectOutputStream outObjectStream;
    
    public void run() {
      try {
        while (true) {
          outgoing = sendBuffer.take();    // get the first frame in the outgoing queue (NOTE: will block until there's something to send)
          sendConnection = new Socket(outgoing.destIP, outgoing.destPort);    // open a network connection
          outObjectStream = new ObjectOutputStream(sendConnection.getOutputStream());    // get the connection output stream
          outObjectStream.writeObject(outgoing.message);    // send
          sendConnection.close();
        }
      }
      catch (InterruptedException e) { System.out.println(e); }
      catch (UnknownHostException e) { System.out.println("Error, unknown host: " + e); }
      catch (IOException e) { System.out.println("Error sending message: " + e); }
    }    
  }
  
  class FrameReceiver implements Runnable {   
    
    ServerSocket recvSocket;
    Socket recvConnection;
    
    public void run() {
      try {
        recvSocket = new ServerSocket(0);    // open incoming socket on any free port
        System.out.println("Server started at " + recvSocket.getInetAddress().getHostAddress() + " and receiving on port " + recvSocket.getLocalPort());
        
        while (true) {
          recvConnection = recvSocket.accept();
          new Thread(new FrameHandler(recvConnection)).start();   // launch separate thread to handle incoming connection
        }
      }
      catch (IOException e) { System.out.println("Error, connection closed abnormally: " + e); }
    }    
  }
  
  class FrameHandler implements Runnable {
    
    Socket connection;    // incoming connection handled by thread
    ObjectInputStream inObjectStream;
    Object message;
    
    FrameHandler(Socket connection) {
      this.connection = connection;
    }
    
    public void run() {
      try {
        inObjectStream = new ObjectInputStream(connection.getInputStream());          
        message = inObjectStream.readObject();
        connection.close();
        
        if (message instanceof Frame) {
          recvBuffer.put((Frame) message);
        }
        else if (message instanceof Header) {
          //TODO: handle header
        }
        else if (message instanceof String) {
          //TODO: handle control messages
        }
      }
      catch (IOException e) { System.out.println("Error, connection closed abnormally: " + e); }
      catch (InterruptedException e) { System.out.println("Error, trying to add to full recvBuffer: " + e); }
      catch (Exception e) { System.out.println(e); }
    }
  }
}