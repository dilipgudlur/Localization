package edu.cmu.pandaa.module;

import edu.cmu.pandaa.header.DistanceHeader.DistanceFrame;
import edu.cmu.pandaa.header.GeometryHeader;
import edu.cmu.pandaa.header.GeometryHeader.GeometryFrame;
import edu.cmu.pandaa.header.MultiHeader;
import edu.cmu.pandaa.header.MultiHeader.MultiFrame;
import edu.cmu.pandaa.header.StreamHeader;
import edu.cmu.pandaa.header.StreamHeader.StreamFrame;
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
    List<Point> points = findPoints(tdoas, pos.geometry);
    Point p = estimateLocation(points);
    savedDistances = findDistances(tdoas, p, savedDistances);
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
        geometry[y][x] = df.peakDeltas[0];
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

  /*
  private Point intersect(Ray r1, Ray r2, Point def) {
    if (r1 == null || r2 == null)
      return def;
    double x1 = r1.x1, x2 = r1.x2, x3 = r2.x1, x4 = r2.x2;
    double y1 = r1.y1, y2 = r1.y2, y3 = r2.x1, y4 = r2.y2;
    double x = ( (x1*y2 - y1*x2)*(x3-x4) - (x1-x2)*(x3*y4-y3*x4) ) /
       ( (x1 - x2)*(y3 - y4) - (y1 - y2)*(x3- x4) );
    double y = ( (x1*y2 - y1*x2)*(y3-y4) - (y1-y2)*(x3*y4-y3*x4) ) /
       ( (x1 - x2)*(y3 - y4) - (y1 - y2)*(x3 - x4) );

    if (Double.isNaN(x) || Double.isNaN(y))
      return def;

    // we are really done with x & y, so we can switch them when we check
    // the "direction" of our intersection
    if (x2 > x1 && x < x1)
      return def;
    if (x2 < x1 && x > x1)
      return def;
    if (x4 > x3 && x < x3)
      return def;
    if (x4 < x3 && x > x3)
      return def;
    if (y2 > y1 && y < y1)
      return def;
    if (y2 < y1 && y > y1)
      return def;
    if (y4 > y3 && y < y3)
      return def;
    if (y4 < y3 && y > y3)
      return def;

    if (def != null)
      throw new RuntimeException("Huh?  Multiple intersections detected");

    return new Point(x, y);
  }
*/

  private Point findIntersection(Point a, Point b, Point c, double dist1, double dist2) {
    if (a == null || b == null || c == null)
      return null;
    if (Double.isNaN(dist1) || Double.isNaN(dist2))
      return null;
    return new Point(1,1);
  }

  private List<Point> findPoints(double[][] tdoas, double[][] base) {
    List<Point> points = new LinkedList<Point>();
    for (int i = 2;i < numDevices;i++) {
      for (int j = 1;j < i;j++) {
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
    return points;
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
    if (p != null)
      throw new RuntimeException("Not implemented yet");
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
      while (true) {
        for (int i = 0; i < infs.length;i++) {
          mfs.sendFrame(infs[i].recvFrame());
        }
        if (!mfs.isReady())
          break;
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
  }
}
