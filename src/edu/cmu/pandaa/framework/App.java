package edu.cmu.pandaa.framework;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.module.StreamModule;
import edu.cmu.pandaa.stream.*;

// server app
public class App {
  static final int STARTUP_DELAY = 500;
  static final int SERVER_PORT = 12345;
  MultiFrameStream combiner = new MultiFrameStream("combiner");
  final Map<StreamHeader, PipeHandler> inHeaders = new HashMap<StreamHeader, PipeHandler>();

  public App(String[] args) throws Exception {
    if (args.length == 0) {
      new AcceptClients().start();
    } else {
      String file1 = args[0];
      for (String file : args) {
        RawAudioFileStream in = new RawAudioFileStream(file, file1, 3);
        in.setTimeDialtion(0.5);
        activateNewDevice(in);
      }
    }

    PipeHandler combpipe = new PipeHandler(combiner, new MergePipeline(), new GeometryFileStream("output.txt", true));
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
      MultiFrameStream mixer = new MultiFrameStream(id);
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
    private final String id;
    private StreamHeader outHeader;
    private boolean closed = false;
    private int count = 0;
    private boolean trace;

    PipeHandler(FrameStream in, StreamModule pipeline, FrameStream out) throws Exception {
      this(in, pipeline, out, false);
    }

    PipeHandler(FrameStream in, StreamModule pipeline, FrameStream out, boolean trace) throws Exception {
      this.trace = trace;
      if (in == null)
        throw new IllegalArgumentException("argument can not be null");

      this.in = in;
      if (out != null) {
        outSet.add(out);
      }
      String pipeName = pipeline.getClass().getSimpleName();
      id = pipeName + '.' + in.getHeader().id;
      this.pipeline = pipeline;
      if (trace)
        System.err.println("Pipeline " + id + " created with " + outSet.size());
      Thread.sleep(1000);
    }

    public void addOutput(MultiFrameStream out) throws Exception {
      if (trace)
        System.err.println("Adding stream " + out.id + " to " + id);
      if (closed) {
        throw new IllegalStateException("Pipeline closed");
      }
      String outId = out.id;
      synchronized(outSet) {
        System.out.println("Adding output " + outId + " from pipe " + id + " at frame " + count);
        outSet.add(out);
        if (outHeader != null)
          out.setHeader(outHeader);
      }
    }

    @Override
    public void run() {
      try {
        if (trace)
          System.err.println("Running stream " + id);
        Thread.sleep(STARTUP_DELAY);

        synchronized(outSet) {
          outHeader = pipeline.init(in.getHeader());
          for (FrameStream out : outSet) {
            out.setHeader(outHeader);
          }
        }

        System.out.println("Delay pipe " + id);
        Thread.sleep(10000);
        System.out.println("Start pipe " + id);

        try {
          while (true) {
            StreamFrame frame = in.recvFrame();
            if (frame == null)
              break;
            frame = pipeline.process(frame);
            count++;
            synchronized(outSet) {
              for (FrameStream out : outSet) {
                out.sendFrame(frame);
              }
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }

        synchronized(outSet) {
          System.out.println("Done with pipe " + id + " count=" + count);
          closed = true;
          for (FrameStream out : outSet) {
            out.close();
          }
          pipeline.close();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
