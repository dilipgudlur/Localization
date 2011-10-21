package edu.cmu.pandaa.framework;

import java.net.ServerSocket;

import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.module.StreamModule;
import edu.cmu.pandaa.stream.DummyStream;
import edu.cmu.pandaa.stream.FrameStream;
import edu.cmu.pandaa.stream.MultiFrameStream;
import edu.cmu.pandaa.stream.SocketStream;

// server app
public class App {
  static final int SERVER_PORT = 12345;
  MultiFrameStream mixer = new MultiFrameStream("mixer");
  MultiFrameStream combiner = new MultiFrameStream("combiner");
  static final int frameTime = 100;

  App(String[] args) throws Exception {
    if (args.length == 0) {
      new AcceptClients().start();
    } else {
      int count = 1;
      for (String file : args) {
        FrameStream in = new DummyStream(new RawAudioHeader("dummy" + count++, System.currentTimeMillis(), frameTime));
        activateNewDevice(in);
      }
    }

    PipeHandler mixpipe = new PipeHandler(mixer, new DualPipeline(), combiner);
    new Thread(mixpipe, this.mixer.getHeader().id).start();

    PipeHandler combpipe = new PipeHandler(combiner, new MergePipeline(), new DummyStream("output"));
    new Thread(combpipe, this.mixer.getHeader().id).start();
  }

  public static void main(String[] args) {
    try {
      new App(args);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private synchronized void activateNewDevice(FrameStream in) throws Exception {
    System.out.println("Activating device " + in.getHeader().id);
    StreamModule pipeline = new SinglePipeline();
    PipeHandler client = new PipeHandler(in, pipeline, mixer);
    new Thread(client, in.getHeader().id).start();
  }

  // server thread, spawning off one client thread per connection
  class AcceptClients extends Thread {
    ServerSocket server;

    @Override
	public void run() {
      try {
        server = new ServerSocket(SERVER_PORT);   // launch server on any free port and start listening for incoming connections
        System.out.println("Server started at " + server.getInetAddress().getHostAddress() + ":" + server.getLocalPort());

        while (true) {
          activateNewDevice(new SocketStream(server.accept()));
        }
      }
      catch (Exception e) {
        System.out.println("Error, closing connection.");
        e.printStackTrace();
      }
    }
  }

  class PipeHandler implements Runnable {
    public final String id;
    private final FrameStream in, out;
    private final StreamModule pipeline;

    PipeHandler(FrameStream in, StreamModule pipeline, FrameStream out) throws Exception {
      this.in = in;
      this.out = out;
      this.pipeline = pipeline;
      id = in.getHeader().id;
    }

    @Override
	public void run() {
      try {
        out.setHeader(pipeline.init(in.getHeader()));
        try {
          System.out.println("Starting pipe " + id);
          while (true) {
            out.sendFrame(pipeline.process(in.recvFrame()));
          }
        } catch (Exception e) {
          System.out.println("Done with pipe " + id);
          pipeline.close();
          out.close();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
