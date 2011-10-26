package edu.cmu.pandaa.framework;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.cmu.pandaa.header.RawAudioHeader;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.module.StreamModule;
import edu.cmu.pandaa.stream.DummyStream;
import edu.cmu.pandaa.stream.FrameStream;
import edu.cmu.pandaa.stream.MultiFrameStream;
import edu.cmu.pandaa.stream.SocketStream;

// server app
public class App {
  static final int SERVER_PORT = 12345;
  MultiFrameStream combiner = new MultiFrameStream("combiner");
  static final int frameTime = 100;
  final Map<StreamHeader, PipeHandler> inHeaders = new HashMap<StreamHeader, PipeHandler>();

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

    PipeHandler combpipe = new PipeHandler(combiner, new MergePipeline(), new DummyStream("output"));
    new Thread(combpipe, "combiner").start();
  }

  public static void main(String[] args) {
    try {
      new App(args);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String makeId(StreamHeader a, StreamHeader b) {
    String aid = a.id;
    String bid = b.id;
    if (aid.equals(bid)) {
      throw new IllegalArgumentException("Streamin IDs must be unique!");
    }

    if (aid.compareTo(bid) < 0) {
      String tmp = bid;
      bid = aid;
      aid = tmp;
    }

    return aid + "+" + bid;
  }

  private synchronized void activateNewDevice(FrameStream in) throws Exception {
    StreamHeader inHeader = in.getHeader();
    System.out.println("Activating device " + inHeader.id);
    StreamModule pipeline = new SinglePipeline();
    PipeHandler pipe = new PipeHandler(in, pipeline, null);
    new Thread(pipe, inHeader.id).start();

    for (StreamHeader other : inHeaders.keySet()) {
      String id = makeId(inHeader, other);
      FrameStream mixer = new MultiFrameStream(id);
      PipeHandler otherPipe = inHeaders.get(other);
      pipe.addOutput(mixer);
      otherPipe.addOutput(mixer);
      PipeHandler dualPipe = new PipeHandler(mixer, new DualPipeline(), combiner);
      new Thread(dualPipe, id).start();
    }
    inHeaders.put(inHeader, pipe);
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
    private final FrameStream in;
    private final Set<FrameStream> outSet = new HashSet<FrameStream>();
    private final StreamModule pipeline;
    private StreamHeader outHeader;
    private boolean closed = false;

    PipeHandler(FrameStream in, StreamModule pipeline, FrameStream out) throws Exception {
      this.in = in;
      if (out != null) {
        outSet.add(out);
      }
      this.pipeline = pipeline;
    }

    public void addOutput(FrameStream out) throws Exception {
      synchronized(outSet) {
        if (closed) {
          throw new IllegalArgumentException("Pipeline closed");
        }
        while (outHeader == null && !closed) {
          outSet.wait();
        }
        outSet.add(out);
        out.setHeader(outHeader);
      }
    }

    @Override
    public void run() {
      try {
        int count = 0;
        String id = in.getHeader().id;
        outHeader = pipeline.init(in.getHeader());
        synchronized(outSet) {
          outSet.notifyAll();
          for (FrameStream out : outSet) {
            out.setHeader(outHeader);
          }
        }
        try {
          System.out.println("Starting pipe " + id);
          StreamFrame frame;
          do {
            frame = pipeline.process(in.recvFrame());
            synchronized(outSet) {
              if (frame != null) {
                count++;
                for (FrameStream out : outSet) {
                  out.sendFrame(frame);
                }
              }
            }
          } while (frame != null);
        } catch (Exception e) {
          e.printStackTrace();
        }
        synchronized(outSet) {
          System.out.println("Done with pipe " + id + " count=" + count);
          pipeline.close();
          closed = true;
          for (FrameStream out : outSet) {
            out.close();
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
