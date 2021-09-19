package s29752.kmb;

import com.google.gson.JsonObject;

import java.util.Objects;

class GeographicCoordinate {
  static GeographicCoordinate get(JsonObject obj) {
    return new GeographicCoordinate(
        obj.get("lat").getAsDouble(),
        obj.get("long").getAsDouble());
  }

  double distance(GeographicCoordinate that) {
    final double dx = this.getLatitude() - that.getLatitude();
    final double dy = this.getLongitude() - that.getLongitude();
    return Math.sqrt(dx*dx + dy*dy);
  }

  double geoDistance(GeographicCoordinate that) {
    return geoDistance(this.getLatitude(), that.getLatitude(), this.getLongitude(), that.getLongitude());
  }

  /** Radius of the earth in m */
  private static final int EARTH_RADIUS_M = 6_371_000;

  static double geoDistance(double lat1, double lat2, double lon1, double lon2) {
    final double latDistance = Math.toRadians(lat2 - lat1);
    final double lonDistance = Math.toRadians(lon2 - lon1);
    final double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return EARTH_RADIUS_M * c;
  }

  private final double latitude;
  private final double longitude;

  GeographicCoordinate(double latitude, double longitude) {
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  GeographicCoordinate orthogonal() {
    return new GeographicCoordinate(-longitude, latitude);
  }

  GeographicCoordinate subtract(GeographicCoordinate that) {
    return new GeographicCoordinate(this.getLatitude() - that.getLatitude(),
        this.getLongitude() - that.getLongitude());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof GeographicCoordinate)) {
      return false;
    }
    final GeographicCoordinate that = (GeographicCoordinate) obj;
    return Double.compare(this.latitude, that.latitude) == 0
        && Double.compare(this.longitude, that.longitude) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(latitude, longitude);
  }

  @Override
  public String toString() {
    return "(" + latitude + ", " + longitude + ')';
  }
}
