package s29752.kmb;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

interface URLs {
  String BASE = "https://data.etabus.gov.hk/v1/transport/kmb/";
  String ROUTE = BASE + "route";
  String STOP = BASE + "stop";
  String STOP_PREFIX = BASE + "stop/";
  String STOP_ETA_PREFIX = BASE + "stop-eta/";

  String ROUTE_STOP_PREFIX = BASE + "route-stop/"; // route-stop/92/inbound/1

  class TrustAllCerts {
    /** All-trusting trust manager */
    private static final TrustManager TRUST_ALL_CERTS = new X509TrustManager() {
      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }

      @Override
      public void checkClientTrusted(X509Certificate[] certs, String authType) {
      }

      @Override
      public void checkServerTrusted(X509Certificate[] certs, String authType) {
      }
    };

    private static final CompletableFuture<Void> FUTURE = CompletableFuture.supplyAsync(() -> {
      try {
        // Install the all-trusting trust manager
        final SSLContext context = SSLContext.getInstance("SSL");
        context.init(null, new TrustManager[]{TRUST_ALL_CERTS}, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        return null;
      } catch (NoSuchAlgorithmException | KeyManagementException e) {
        throw new CompletionException("Failed to init TrustManager", e);
      }
    });

    static void init() {
      FUTURE.join();
    }
  }


  static String readLine(String url) {
    TrustAllCerts.init();

    Print.debug("readLine " + url);
    String line;
    try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
      line = in.readLine();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to readLine " + url, e);
    }
    Print.debug(line);
    return line;
  }

  static List<BusStop> readRouteStop(Kmb.Route route, Kmb.Route.Type type, Kmb.BusStopMap stopMap) {
    final String url = ROUTE_STOP_PREFIX + route.getId() + "/" + type;
    final JsonArray array = JsonParser.parseString(readLine(url)).getAsJsonObject().getAsJsonArray("data");
    final List<BusStop> stops = new ArrayList<>(array.size());
    for (int i = 0; i < array.size(); i++) {
      final String stopId = array.get(i).getAsJsonObject().get("stop").getAsString();
      stops.add(Objects.requireNonNull(stopMap.get(stopId)));
    }
    return Collections.unmodifiableList(stops);
  }

  static Map<String, Kmb.Route> readRoutes(Kmb.BusStopMap stops) {
    final JsonArray routes = JsonParser.parseString(readLine(ROUTE)).getAsJsonObject().getAsJsonArray("data");
    final Map<String, Kmb.Route> map = new TreeMap<>();
    for (int i = 0; i < routes.size(); i++) {
      final JsonObject json = routes.get(i).getAsJsonObject();
      final String route = json.get("route").getAsString();
      map.compute(route, (k, v) -> v != null ? v : new Kmb.Route(route)).put(json, stops);
    }
    return Collections.unmodifiableMap(map);
  }

  static Map<String, BusStop> readStops() {
    final JsonArray stops = JsonParser.parseString(readLine(STOP)).getAsJsonObject().getAsJsonArray("data");
    final Map<String, BusStop> map = new TreeMap<>();
    for (int i = 0; i < stops.size(); i++) {
      final JsonObject json = stops.get(i).getAsJsonObject();
      final BusStop stop = BusStop.valueOf(json);
      map.put(stop.getId(), stop);
    }
    Print.ln("  " + stops.size() + " bus stops");
    return Collections.unmodifiableMap(map);
  }

  static JsonObject readStop(String stopId) {
    return JsonParser.parseString(readLine(STOP_PREFIX + stopId)).getAsJsonObject();
  }

  ConcurrentMap<BusStop, Kmb.StopEta> ETAS = new ConcurrentHashMap<>();

  static List<Kmb.Eta> getStopEtas(BusStop stop) {
    return ETAS.compute(stop, (k, v) -> v != null && !v.isExpired()? v : readStopEtas(stop)).getEtas();
  }

  static Kmb.StopEta readStopEtas(BusStop stop) {
    final String url = STOP_ETA_PREFIX + stop.getId();
    final JsonObject json = JsonParser.parseString(readLine(url)).getAsJsonObject();
    final ZonedDateTime generated = Utils.toZonedDateTime(json.get("generated_timestamp"));
    final JsonArray data = json.getAsJsonArray("data");
    final List<Kmb.Eta> etas = new ArrayList<>(data.size());
    for (int i = 0; i < data.size(); i++) {
      etas.add(Kmb.Eta.get(stop, data.get(i).getAsJsonObject()));
    }
    return new Kmb.StopEta(stop, etas, generated);
  }
}
