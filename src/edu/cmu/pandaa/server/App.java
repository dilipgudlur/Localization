package edu.cmu.pandaa.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import edu.cmu.pandaa.shared.stream.FrameStream;
import edu.cmu.pandaa.shared.stream.MemoryStream;
import edu.cmu.pandaa.shared.stream.SocketStream;
import edu.cmu.pandaa.shared.stream.header.StreamHeader.StreamFrame;

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
    
    AcceptClients() {
      this.start();
    }
    
    public void run() {
      try {
        server = new ServerSocket(0);   // launch server on any free port and start listening for incoming connections
        System.out.println("Server started at " + server.getInetAddress().getHostAddress() + ":" + server.getLocalPort());
        
        while (true) {
          connection = server.accept();   // accept incoming connection
          new HandleClient(connection);   // launch new client thread
        }
      } 
      catch (IOException e) { System.out.println("Error, connection closed abnormally."); e.printStackTrace(); }
      
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
	    frame = clientStream.recvFrame();   // message comes in as a compressed frame
        
	    frame = decompress.process(frame);
	    frame = concatenate.process(frame);
        //TODO: decompress
        //TODO: concatenate
        //TODO: align
        //TODO: detect impulsive peaks
        
        //TODO: put everything in a FrameStream for pair-wise TDOA computation
	}
    }
  }
}