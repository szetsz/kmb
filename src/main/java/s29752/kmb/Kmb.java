package s29752.kmb;

import com.google.common.base.Suppliers;
import com.google.gson.JsonObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Kmb {
  static Kmb INSTANCE = new Kmb();

  public static Kmb getInstance() {
    return INSTANCE;
  }

  static class Name {
    static Name get(String prefix, JsonObject obj) {
      return new Name(
          obj.get(prefix + "_tc").getAsString(),
          obj.get(prefix + "_sc").getAsString(),
          obj.get(prefix + "_en").getAsString());
    }

    private final String tc;
    private final String sc;
    private final String en;

    Name(String tc, String sc, String en) {
      this.tc = tc;
      this.sc = sc;
      this.en = en;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if (!(obj instanceof Name)) {
        return false;
      }
      final Name that = (Name) obj;
      return Objects.equals(this.tc, that.tc)
          && Objects.equals(this.sc, that.sc)
          && Objects.equals(this.en, that.en);
    }

    @Override
    public int hashCode() {
      return Objects.hash(tc, sc, en);
    }

    @Override
    public String toString() {
      return tc;
    }
  }

  static class RouteMap {
    private final Map<String, Route> map;

    RouteMap(Map<String, Route> map) {
      this.map = map;
    }

    Route get(String routeId) {
      return Objects.requireNonNull(map.get(routeId), () -> "Failed to get " + routeId);
    }

    void print(Consumer<Object> out) {
      map.values().forEach(out);
    }
  }

  static class Route {
    static class Type {
      private static final ConcurrentMap<Integer, Map<Bound, Type>> TYPES = new ConcurrentHashMap<>();
      private static Map<Bound, Type> newEnumMap(int serviceType) {
        final EnumMap<Bound, Type> map = new EnumMap<>(Bound.class);
        map.put(Bound.INBOUND, new Type(Bound.INBOUND, serviceType));
        map.put(Bound.OUTBOUND, new Type(Bound.OUTBOUND, serviceType));
        return Collections.unmodifiableMap(map);
      }

      static Type valueOf(Bound bound, int serviceType) {
        return TYPES.compute(serviceType, (k, v) -> v != null? v: newEnumMap(serviceType))
            .get(bound);
      }

      static int getServiceType(JsonObject obj) {
        return Integer.parseInt(obj.get("service_type").getAsString());
      }

      private final Bound bound;
      private final int serviceType; // "1"
      private final String name;

      Type(Bound bound, int serviceType) {
        this.bound = bound;
        this.serviceType = serviceType;
        this.name = bound.name().toLowerCase() + "/" + serviceType;
      }

      @Override
      public boolean equals(Object obj) {
        if (this == obj) {
          return true;
        } else if (!(obj instanceof Type)) {
          return false;
        }
        final Type that = (Type) obj;
        return this.serviceType == that.serviceType && this.bound == that.bound;
      }

      @Override
      public int hashCode() {
        return Objects.hash(bound, serviceType);
      }

      @Override
      public String toString() {
        return name;
      }
    }

    static class OrigDest {
      private final Name orig;       // "PO LAM", "寶林", "宝林"
      private final Name dest;       // "DIAMOND HILL STATION", "鑽石山站", "钻石山站"
      private final Supplier<List<BusStop>> stops;

      OrigDest(Name orig, Name dest, Supplier<List<BusStop>> stops) {
        this.orig = orig;
        this.dest = dest;
        this.stops = stops;
      }

      boolean isLast(int seq) {
        return stops.get().size() == seq;
      }

      @Override
      public String toString() {
        return orig + "->" + dest;
      }
    }

    private final String id;    // "91M"
    private final ConcurrentMap<Type, OrigDest> types = new ConcurrentHashMap<>();

    Route(String id) {
      this.id = id;
    }

    String getId() {
      return id;
    }

    OrigDest getOrigDest(Type type) {
      return types.get(type);
    }

    void put(JsonObject json, BusStopMap stops) {
      if (!id.equalsIgnoreCase(json.get("route").getAsString())) {
        throw new IllegalArgumentException("Route mismatched: route=" + id + " but " + json);
      }

      final Type type = Type.valueOf(Bound.parse(json.get("bound").getAsString()),
          Type.getServiceType(json));
      final Name orig = Name.get("orig", json);
      final Name dest = Name.get("dest", json);
      final OrigDest origDest = new OrigDest(orig, dest,
          Suppliers.memoize(() -> {
            final List<BusStop> busStops = URLs.readRouteStop(this, type, stops);
            Print.debug(getId() + " " + orig + "->" + dest + " [" + type + "]");
            for(int i = 0; i < busStops.size(); i++) {
              final int seq = i + 1;
              Print.debug(String.format("  %2d: %s", seq, busStops.get(i)));
            }
            return busStops;
          }));
      types.put(type, origDest);
    }

    boolean match(Eta eta) {
      return id.equalsIgnoreCase(eta.getRoute());
    }

    @Override
    public String toString() {
      return id + types.values();
    }
  }

  static class BusStopMap implements Iterable<BusStop> {
    private final Map<String, BusStop> map;

    BusStopMap(Map<String, BusStop> map) {
      this.map = map;
    }

    BusStop get(String stopId) {
      return Objects.requireNonNull(map.get(stopId), () -> "Failed to get BusStop " + stopId);
    }

    @Override
    public Iterator<BusStop> iterator() {
      return map.values().iterator();
    }
  }

  enum Company {KMB, LWS}

  enum Bound {
    INBOUND, OUTBOUND;

    boolean match(String symbol) {
      if (symbol == null) {
        return false;
      }
      if (symbol.length() > name().length()) {
        symbol = symbol.substring(0, name().length());
      }
      final String sub = name().substring(0, symbol.length());
      return sub.equalsIgnoreCase(symbol);
    }

    static Bound parse(String symbol) {
      for(Bound b : values()) {
        if (b.match(symbol)) {
          return b;
        }
      }
      return null;
    }
  }

  static class Eta {
    static Eta get(BusStop stop, JsonObject eta) {
      final Route.Type type = Route.Type.valueOf(
          Bound.parse(eta.get("dir").getAsString()),
          Route.Type.getServiceType(eta));
      return new Eta(stop,
          Company.valueOf(eta.get("co").getAsString()),
          eta.get("route").getAsString(),
          type,
          Integer.parseInt(eta.get("seq").getAsString()),
          Name.get("dest", eta),
          Integer.parseInt(eta.get("eta_seq").getAsString()),
          Utils.toLocalDateTime(eta.get("eta")),
          Name.get("rmk", eta),
          Utils.toLocalDateTime(eta.get("data_timestamp")));
    }

    private final BusStop stop;

    private final Company company;
    private final String route;
    private final Route.Type type;
    private final int seq;
    private final Name dest;
    private final int etaSeq;
    private final LocalDateTime eta;
    private final Name remark;
    private final LocalDateTime timestamp;

    Eta(BusStop stop, Company company, String route, Route.Type type, int seq,
        Name dest, int etaSeq, LocalDateTime eta, Name remark, LocalDateTime timestamp) {
      this.stop = stop;
      this.company = company;
      this.route = route;
      this.type = type;
      this.seq = seq;
      this.dest = dest;
      this.etaSeq = etaSeq;
      this.eta = eta;
      this.remark = remark;
      this.timestamp = timestamp;
    }

    String getRoute() {
      return route;
    }

    Route.Type getType() {
      return type;
    }

    int getSeq() {
      return seq;
    }

    LocalDateTime getEta() {
      return eta;
    }

    public String getToDestString() {
      return company + " " + route + " to " + dest;
    }

    public String getEtaString() {
      final String t = stop.getName() + "(Stop " + seq  + ")";
      final String s = eta != null? " will arrive " + t + " at " + eta.toLocalTime() + " on " + eta.toLocalDate()
          : " has NO eta for " + t;
      return s + " (" + remark + " " + etaSeq + ")";
    }

    @Override
    public String toString() {
      return getToDestString() + getEtaString();
    }
  }

  static class StopEta {
    private final BusStop stop;
    private final List<Kmb.Eta> etas;
    private final LocalDateTime generated;

    StopEta(BusStop stop, List<Eta> etas, LocalDateTime generated) {
      this.stop = stop;
      this.etas = etas;
      this.generated = generated;
    }

    List<Eta> getEtas() {
      return etas;
    }

    boolean isExpired() {
      return Duration.between(generated, LocalDateTime.now()).getSeconds() > 30;
    }
  }

  private final BusStopMap stops;
  private final RouteMap routes;

  private Kmb() {
    stops = new BusStopMap(URLs.readStops());
    routes = new RouteMap(URLs.readRoutes(stops));
  }

  Route getRoute(String route) {
    return routes.get(route);
  }

  BusStop getBusStop(String stopId) {
    return stops.get(stopId);
  }

  Iterable<BusStop> busStops() {
    return stops;
  }

  public static void main(String[] args) {
    final Kmb kmb = getInstance();
    final Route bus91M = kmb.getRoute("91M");
    final Route bus92 = kmb.getRoute("92");

    final BusStop lungPoonCourt = kmb.getBusStop("4B9D547F0F450784");
//    kmb.printEta(bus91M, lungPoonCourt, Print::ln);
    kmb.printEta(null, lungPoonCourt, Print::ln);

    final BusStop diamondHillStationBusTerminus91M = kmb.getBusStop("53889000AA9C33E2");
//    kmb.printEta(bus91M, diamondHillStationBusTerminus91M, Print::ln);
    kmb.printEta(null, diamondHillStationBusTerminus91M, Print::ln);

    final BusStop diamondHillStationBusTerminus92 = kmb.getBusStop("10B8C166D8E60F65");
//    kmb.printEta(bus92, diamondHillStationBusTerminus92, Print::ln);
    kmb.printEta(null, diamondHillStationBusTerminus92, Print::ln);

    final BusStop chiLinNunnery = kmb.getBusStop("951CE3B3EB98BA3A");
    kmb.printEta(null, chiLinNunnery, Print::ln);

    final BusStop shunLeeFireStation = kmb.getBusStop("927CE95D5C98C195");
    kmb.printEta(null, shunLeeFireStation, Print::ln);
  }

  void printEta(Route route, BusStop stop, Consumer<Object> out) {
    out.accept("");
    out.accept("ETA for " + stop);
    getETAs(route, stop).values().forEach(list -> list.forEach(s -> out.accept("  " + s)));
  }

  Map<String, List<Eta>> getETAs(Route route, BusStop stop) {
    final List<Eta> data = URLs.getStopEtas(stop);
    final Map<String, List<Eta>> map = new TreeMap<>();
    for(final Eta eta : data) {
      final Route r;
      if (route == null) {
        r = routes.get(eta.getRoute());
      } else if (route.match(eta)) {
        r = route;
      } else {
        r = null;
      }

      if (r != null && !r.getOrigDest(eta.getType()).isLast(eta.getSeq())) {
        final List<Eta> etas = map.compute(r.getId(), (k, v) -> v != null ? v : new ArrayList<>());
        boolean found = false;
        for(int j = 0; !found && j < etas.size(); j++) {
          found = Objects.equals(eta.getEta(), etas.get(j).getEta());
        }
        if (!found) {
          etas.add(eta);
        }
      }
    }
    return map;
  }
}
