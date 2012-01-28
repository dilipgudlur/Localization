package edu.cmu.pandaa.stream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;

import java.io.*;
import java.net.InetSocketAddress;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 1/22/12
 * Time: 9:03 AM
 */

public class WebViewStream implements FrameStream {
  static final int VIEW_PORT = 8012;
  static final String STATIC_URL_PREFIX = "/";
  static final String SERVE_FILE_PREFIX = "www/";
  static final String DATA_FILE_PREFIX = "/json/";

  StreamHeader header;
  StreamFrame frame;
  HttpServer server;

  public WebViewStream() {
    try {
      server = HttpServer.create(new InetSocketAddress(VIEW_PORT), 2);
      server.createContext(STATIC_URL_PREFIX, new PageHandler());
      server.createContext(DATA_FILE_PREFIX, new DataHandler());
      server.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  class PageHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
      try {
        String uri = t.getRequestURI().getPath();
        if (!uri.startsWith(STATIC_URL_PREFIX))
          throw new RuntimeException("Unexpected URL");
        String file = SERVE_FILE_PREFIX + uri.substring(STATIC_URL_PREFIX.length());
        FileInputStream serveStream = new FileInputStream(file);
        t.sendResponseHeaders(200, 0 ); // indicate dynamic length reply
        OutputStream os = t.getResponseBody();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = serveStream.read(buffer)) > 0) {
          os.write(buffer, 0, len);
        }
        os.close();
        serveStream.close();
      } catch (FileNotFoundException e) {
        t.sendResponseHeaders(404, -1);
      } catch (IOException e) {
        e.printStackTrace();
        throw e;
      }
    }
  }

  class DataHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
      try {
        synchronized(WebViewStream.this) {
          if (frame == null) {
            t.sendResponseHeaders(400, -1);
            return;
          }
          t.sendResponseHeaders(200, 0);
          FileStream outStream = frame.getHeader().createOutput();
          OutputStream os = t.getResponseBody();
          outStream.setFormatJson(true);
          outStream.setOutputStream(os);
          outStream.setHeader(frame.getHeader());
          outStream.sendFrame(frame);
          outStream.close();
          os.close();
        }
      } catch (Exception e) {
        t.sendResponseHeaders(400, -1);
      }
    }
  }

  @Override
  public void setHeader(StreamHeader h) throws Exception {
    header = h;
  }

  @Override
  public synchronized void sendFrame(StreamFrame m) throws Exception {
    frame = m;
  }

  @Override
  public StreamHeader getHeader() throws Exception {
    return header;
  }

  @Override
  public StreamFrame recvFrame() throws Exception {
    return frame;
  }

  @Override
  public void close() throws Exception {
  }
}
