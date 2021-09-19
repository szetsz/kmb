package s29752.kmb;

import com.google.gson.JsonObject;

import java.util.Objects;

class BusStop {
  static BusStop get(String stopId) {
    final JsonObject json = URLs.readStop(stopId);
    final JsonObject data = json.getAsJsonObject("data");
    return new BusStop(stopId, Kmb.Name.get("name", data), GeographicCoordinate.get(data));
  }

  static BusStop valueOf(JsonObject json) {
    return new BusStop(json.get("stop").getAsString(), Kmb.Name.get("name", json), GeographicCoordinate.get(json));
  }

  private final String id;
  private final Kmb.Name name;
  private final GeographicCoordinate coordinate;

  BusStop(String id, Kmb.Name name, GeographicCoordinate coordinate) {
    this.id = id;
    this.name = name;
    this.coordinate = coordinate;
  }

  String getId() {
    return id;
  }

  Kmb.Name getName() {
    return name;
  }

  GeographicCoordinate getCoordinate() {
    return coordinate;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof BusStop)) {
      return false;
    }
    final BusStop that = (BusStop) obj;
    return Objects.equals(this.id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return name + "-" + getId() + " " + getCoordinate();

  }
}
