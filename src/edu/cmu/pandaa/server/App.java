package edu.cmu.pandaa.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import edu.cmu.pandaa.frame.StreamHeader;
import edu.cmu.pandaa.frame.StreamHeader.StreamFrame;
import edu.cmu.pandaa.module.StreamModule;
import edu.cmu.pandaa.stream.FrameStream;
import edu.cmu.pandaa.stream.MemoryStream;
import edu.cmu.pandaa.stream.SocketStream;

// server app
public class App {
    StreamModule decompress;
    StreamModule concatenate;

  App() {
    // listen for clients
    AcceptClients acceptClients = new AcceptClients();
  }
  
  public static void main(String[] args) {
    App server = new App();    
  }
  
  
  // server thread, spawning off one client thread per connection
  class AcceptClients extends Thread {
    
    ServerSocket server;
    Socket connection;
    Thread client;
    
    AcceptClients() {
      this.start();
    }
    
    public void run() {
      try {
        server = new ServerSocket(0);   // launch server on any free port and start listening for incoming connections
        System.out.println("Server started at " + server.getInetAddress().getHostAddress() + ":" + server.getLocalPort());
        
        while (true) {
          connection = server.accept();   // accept incoming connection
          client = new HandleClient(connection);   // launch new client thread
          //TODO: add client to client manager, which pairs client.impulsePeaks in instances of TDOAImpulseCorrelationModule
        }
      } 
      catch (IOException e) { 
        System.out.println("Error, connection closed abnormally."); e.printStackTrace(); 
      }
    }    
  }
  
  // client thread
  class HandleClient extends Thread {
    
    private Socket connection;
    private FrameStream clientStream;
    public FrameStream impulsePeaks;
    private StreamFrame frame;
    
    HandleClient(Socket connection) {
      this.connection = connection;
      this.clientStream = new SocketStream(connection);
      this.impulsePeaks = new MemoryStream();
      this.start();
    }
    
    public void run() {         
    	StreamHeader header = clientStream.getHeader();
    	header = decompress.initialize(header);
    	header = concatenate.initialize(header);
    
    	while (true) {
  	    frame = clientStream.recvFrame();    // message comes in as a compressed frame
          
  	    frame = decompress.process(frame);   //TODO: decompress
  	    frame = concatenate.process(frame);  //TODO: concatenate
  	    
        //TODO: align
        //TODO: detect impulsive peaks
  	    
        impulsePeaks.sendFrame(frame);    // put everything in a FrameStream for pair-wise TDOA computation
    	}
    }
  }
}