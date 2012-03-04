package edu.cmu.pandaa.depricated;

import edu.cmu.pandaa.header.DistanceHeader.DistanceFrame;
import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.GeometryHeader.GeometryFrame;
import edu.cmu.pandaa.header.MultiHeader;
import edu.cmu.pandaa.header.MultiHeader.MultiFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
import edu.cmu.pandaa.module.StreamModule;
import edu.cmu.pandaa.stream.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: peringknife
 * Date: 12/15/11
 * Time: 10:52 AM
 */

public class TDOAMatrixModule implements StreamModule {
  GeometryHeader gHeader;
  FrameStream posStream;
  int numDevices;
  static final double distanceScale = 340.29; // m/s at sea level
  double[][] savedDistances;

  @Override
  public StreamHeader init(StreamHeader inHeader) throws Exception {
    MultiHeader tdoas = (MultiHeader) inHeader;
    gHeader = new GeometryHeader(tdoas);
    return gHeader;
  }

  @Override
  public StreamHeader.StreamFrame process(StreamFrame inFrame) throws Exception {
    GeometryFrame out = gHeader.makeFrame();
    StreamFrame[] frames = ((MultiFrame) inFrame).getFrames();
    GeometryFrame pos = (GeometryFrame) posStream.recvFrame();

    double[][] tdoas = makeTdoas(frames);
    Point source = findSource(tdoas, pos.geometry);
    savedDistances = findDistances(tdoas, source, savedDistances);
    out.geometry = savedDistances;
    return out;
  }

  private double[][] makeTdoas(StreamFrame[] frames) {
    int n = frames.length;
    double[][] geometry = new double[n][n];
    makeNan(geometry);
    for (int i = 0;i < n;i++) {
      DistanceFrame df = (DistanceFrame) frames[i];
      if (df.peakDeltas.length > 0) {
        int x = gHeader.indexOf(df.getHeaderId(0));
        int y = gHeader.indexOf(df.getHeaderId(1));
        geometry[x][y] = df.peakDeltas[0];
        geometry[y][x] = -df.peakDeltas[0];
      }
    }
    return geometry;
  }

  private void makeNan(double[][] matrix) {
    for (int i = 0;i < matrix.length;i++) {
      for (int j = 0;j < matrix[i].length;j++) {
        matrix[i][j] = Double.NaN;
      }
    }
  }

  private Point getPos(double[][] base, int p) {
    double x = base[0][p];
    double y = base[1][p];
    if (Double.isNaN(x) || Double.isNaN(y))
      return null;
    return new Point(x, y);
  }

  private Point findIntersection(Point a, Point b, Point c, double dist1, double dist2) {
    if (a == null || b == null || c == null)
      return null;
    if (Double.isNaN(dist1) || Double.isNaN(dist2))
      return null;

    double size = a.dist(c) + b.dist(c);
    Point guess = null;
    double guessScore = 0;
    for (int i = 0;i < 100; i++) {
      double ang = Math.random() * Math.PI * 2.0;
      double dist = Math.random() * size;
      Point next = new Point(c.x + Math.sin(ang)*dist, c.y + Math.cos(ang)*dist);
      double ddif1 = next.dist(c) - next.dist(a) - dist1;  // TODO: polarity could be inverted!
      double ddif2 = next.dist(c) - next.dist(b) - dist2;  // TODO: polarity could be inverted!
      double score = ddif1*ddif1 + ddif2*ddif2;
      if (guess == null || score < guessScore) {
        guess = next;
        guessScore = score;
      }
    }
    return guess;
  }

  private Point findSource(double[][] tdoas, double[][] base) {
    List<Point> points = new LinkedList<Point>();
    for (int i = 0;i < numDevices;i++) {
      for (int j = i+1;j < numDevices;j++) {
        for (int k = 0;k < numDevices;k++)
          if (k != i && k != j) {
            double dik = tdoas[i][k] * distanceScale;
            double djk = tdoas[j][k] * distanceScale;
            Point p = findIntersection(getPos(base, i), getPos(base, j), getPos(base, k), dik, djk);
            if (p != null)
              points.add(p);
          }
      }
    }

    if (points.size() == 0)
      return null;

    double x = 0;
    double y = 0;
    int num = points.size();
    for (Point p : points) {
      x += p.x;
      y += p.y;
    }
    return new Point(x/num, y/num);
  }

  private Point estimateLocation(List<Point> points) {
    if (points == null || points.size() == 0)
      return null;
    double x = 0;
    double y = 0;
    for (Point p : points) {
      x += p.x;
      y += p.y;
    }
    return new Point(x/points.size(), y/points.size());
  }

  private double[][] findDistances(double[][] tdoas, Point p, double[][] prevGeometry) {
    //if (p != null)
    //  throw new RuntimeException("Not implemented yet");
    return prevGeometry;
  }

  @Override
  public void close() {
  }

  public void setPositionStream(FrameStream posStream) throws Exception {
    this.posStream = posStream;

    GeometryHeader gh = (GeometryHeader) posStream.getHeader();
    numDevices = gh.rows;

    savedDistances = new double[numDevices][numDevices];
    makeNan(savedDistances);

    if (gh.frameTime != gHeader.frameTime)
      throw new IllegalArgumentException("Frame time mismatch");
  }

  public static void main(String[] args) throws Exception {
    int arg = 0;
    String outf = args[arg++];
    String base = args[arg++];

    FileStream[] infs = new FileStream[args.length - arg];
    MultiFrameStream mfs = new MultiFrameStream("tdoam");
    String msg = "TDOAMatrix: " + outf;
    for (int i = 0; i < infs.length;i++) {
      String fname= args[i+arg];
      infs[i] = new DistanceFileStream(fname);
      mfs.setHeader(infs[i].getHeader());
      msg = msg + " " + fname;
    }
    System.out.println(msg);

    FileStream bfs = new GeometryFileStream(base);
    FileStream ofs = new GeometryFileStream(outf, true);

    TDOAMatrixModule tdoa = new TDOAMatrixModule();
    ofs.setHeader(tdoa.init(mfs.getHeader()));
    tdoa.setPositionStream(bfs);

    try {
      mfs.noblock = true;
      while (true) {
        for (int i = 0; i < infs.length;i++) {
          mfs.sendFrame(infs[i].recvFrame());
        }
        ofs.sendFrame(tdoa.process(mfs.recvFrame()));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    ofs.close();
    mfs.close();
    for (int i = 0; i < infs.length;i++) {
      infs[i].close();
    }
  }

  static class Point {
    public double x,y;
    public Point(double x, double y) {
      this.x = x;
      this.y = y;
    }

    public double dist(Point b) {
      double dx = x - b.x;
      double dy = y - b.y;
      return Math.sqrt(dx*dx + dy*dy);
    }
  }
}
