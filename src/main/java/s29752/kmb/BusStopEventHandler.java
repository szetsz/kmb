package s29752.kmb;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

class BusStopEventHandler implements EventHandler<MouseEvent> {
  private final StreetMapApp streetMapApp;
  private final ConcurrentMap<BusStop, StopEtaStage> etaStages = new ConcurrentHashMap<>();
  private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(3);

  BusStopEventHandler(StreetMapApp streetMapApp) {
    this.streetMapApp = streetMapApp;
  }

  @Override
  public void handle(MouseEvent e) {
    final IntegerCoordinate xy = new IntegerCoordinate((int) e.getX(), (int) e.getY());
    final GeographicCoordinate geographic = CoordinateFunction.getInstance().apply(xy);
    Print.ln("click " + xy + " => " + geographic);

    final Kmb kmb = Kmb.getInstance();
    for (BusStop stop : kmb.busStops()) {
      final GeographicCoordinate c = stop.getCoordinate();
      if (CoordinateFunction.getInstance().near(c, geographic)) {
        final StopEtaStage s = etaStages.compute(stop,
            (k, v) -> v != null? v: new StopEtaStage(stop, streetMapApp, scheduler));
        s.show();
      }
    }
  }
}
