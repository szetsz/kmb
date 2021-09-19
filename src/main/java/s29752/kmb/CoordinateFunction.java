package s29752.kmb;

import java.util.Objects;

class CoordinateFunction {
  static class BusStopCoordinates {
    private final BusStop stop;
    private final Coordinates coordinates;

    private BusStopCoordinates(String stopId, int x, int y) {
      this.stop = Objects.requireNonNull(Kmb.getInstance().getBusStop(stopId));
      this.coordinates = new Coordinates(stop.getName(), new IntegerCoordinate(x, y), stop.getCoordinate());
    }

    IntegerCoordinate getIntegerCoordinate() {
      return coordinates.intCoordinate;
    }

    BusStop getStop() {
      return stop;
    }
  }

  static final BusStopCoordinates LUNG_POON_COURT = new BusStopCoordinates("4B9D547F0F450784", 647, 500);
  static final BusStopCoordinates CHI_LIN_NUNNERY = new BusStopCoordinates("951CE3B3EB98BA3A", 974, 763);
  static final BusStopCoordinates PO_TSZ_LANE = new BusStopCoordinates("0B160DE955F3AE10", 573, 204);
  static final BusStopCoordinates LOK_MOON_HOUSE = new BusStopCoordinates("2FBEE4E1965D7EF5", 631, 77);

  private static class Coordinates {
    private final IntegerCoordinate intCoordinate;
    private final GeographicCoordinate geoCoordinate;

    private Coordinates(Object name, IntegerCoordinate intCoordinate, GeographicCoordinate geoCoordinate) {
      this.intCoordinate = intCoordinate;
      this.geoCoordinate = geoCoordinate;
      Print.ln(name + ": " + this);
    }

    public int getX() {
      return intCoordinate.getX();
    }

    public int getY() {
      return intCoordinate.getY();
    }

    public double getLatitude() {
      return geoCoordinate.getLatitude();
    }

    public double getLongitude() {
      return geoCoordinate.getLongitude();
    }

    Coordinates orthogonal(Object name) {
      return new Coordinates(name, intCoordinate.orthogonal(), geoCoordinate.orthogonal());
    }

    Coordinates subtract(Object name, Coordinates that) {
      return new Coordinates(name,
          this.intCoordinate.subtract(that.intCoordinate),
          this.geoCoordinate.subtract(that.geoCoordinate));
    }

    @Override
    public String toString() {
      return intCoordinate + " <=> " + geoCoordinate;
    }
  }

  static int round(double d) {
    return Math.toIntExact(Math.round(d));
  }

  static int round(int n, int divisor) {
    return (n + divisor/2) / divisor * divisor;
  }

  static final CoordinateFunction INSTANCE = new CoordinateFunction(
      CHI_LIN_NUNNERY,
      LOK_MOON_HOUSE);

  static CoordinateFunction getInstance() {
    return INSTANCE;
  }

  private final Coordinates B;
  private final Coordinates C;
  private final Coordinates AB;
  private final Coordinates CB;
  private final int DET_INT;
  private final double DET_GEO;

  private final double NEAR_DISTANCE;

  CoordinateFunction(BusStopCoordinates base, BusStopCoordinates another) {
    this.B = base.coordinates;
    this.C = another.coordinates;
    this.CB = C.subtract("CB", B);
    this.AB = CB.orthogonal("AB");
    this.DET_INT = AB.getX() * CB.getY() - CB.getX() * AB.getY();
    this.DET_GEO = AB.getLatitude() * CB.getLongitude() - CB.getLatitude() * AB.getLongitude();


    final double geoDistance = B.geoCoordinate.geoDistance(C.geoCoordinate);
    final double distance = B.geoCoordinate.distance(C.geoCoordinate);
    Print.ln("Distance from " + base.getStop().getName() + " to " + another.getStop().getName()
        + ": " +  geoDistance + "m (" + distance + ")");
    Print.ln();
    NEAR_DISTANCE = distance * 50 / geoDistance;
  }

  boolean near(GeographicCoordinate a, GeographicCoordinate b) {
    return a.distance(b) < NEAR_DISTANCE;
  }

  IntegerCoordinate apply(GeographicCoordinate p) {
    final double pLat = p.getLatitude() - B.getLatitude();
    final double pLon = p.getLongitude() - B.getLongitude();

    final double md = CB.getLongitude() * pLat - CB.getLatitude() * pLon;
    final double nd = -AB.getLongitude() * pLat + AB.getLatitude() * pLon;

    final double x = B.getX() + (md * AB.getX() + nd * CB.getX()) / DET_GEO;
    final double y = B.getY() + (md * AB.getY() + nd * CB.getY()) / DET_GEO;
    return new IntegerCoordinate(round(x), round(y));
  }

  GeographicCoordinate apply(IntegerCoordinate p) {
    //  AB = (x1, y1) and CB = (x2, y2)
    //  p = m AB + n CB

    // [px] = [x1  x2] [m]
    // (py] = [y1  y2] [n]

    // [x1 x2]^-1  = [ y2 -x2]
    // [y1 y2]     = [-y1  x1] / d ;  where d = (x1 y2 - x2 y1)

    // [ y2 -x2] [px]     = [m]
    // [-y1  x1] [py] / d = [n]

    // m = ( y2 px - x2 py)/d
    // n = (-y1 px + x1 py)/d

    final int px = p.getX() - B.getX();
    final int py = p.getY() - B.getY();

    final int md = CB.getY() * px - CB.getX() * py;
    final int nd = -AB.getY() * px + AB.getX() * py;

    final double lat = B.getLatitude() + (md * AB.getLatitude() + nd * CB.getLatitude()) / DET_INT;
    final double lon = B.getLongitude() + (md * AB.getLongitude() + nd * CB.getLongitude()) / DET_INT;
    return new GeographicCoordinate(lat, lon);
  }
}
