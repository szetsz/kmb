package s29752.kmb;

import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class StopEtaStage {
  private static final int SPACING = 5;

  private final BusStop stop;
  private final Stage stage;

  StopEtaStage(BusStop stop, StreetMapApp streetMapApp, ScheduledThreadPoolExecutor scheduler) {
    this.stop = stop;
    this.stage = newStage(streetMapApp, scheduler);
  }

  VBox newEtaBox() {
    final ZonedDateTime now = Utils.now();
    final VBox vBox = new VBox(2 * SPACING);
    final Text lastUpdated = new Text("Last Updated: " + now);
    lastUpdated.setTextAlignment(TextAlignment.RIGHT);
    lastUpdated.setFill(Color.BLUE);
    vBox.getChildren().add(lastUpdated);

    final Map<String, List<Kmb.Eta>> etas = Kmb.getInstance().getETAs(null, stop);
    for (Map.Entry<String, List<Kmb.Eta>> e : etas.entrySet()) {
      final HBox toDestRow = new HBox(SPACING);
      String previous = null;
      VBox toDestColumn = null;
      for (Kmb.Eta eta : e.getValue()) {
        final String toDest = eta.getToDestString();
        if (toDestColumn == null || !Objects.equals(toDest, previous)) {
          final Text toDestText = new Text(toDest);
          toDestText.setFill(Color.BLUE);
          toDestRow.getChildren().add(toDestText);
          toDestColumn = new VBox(SPACING);
          toDestRow.getChildren().add(toDestColumn);
          previous = toDest;
        }
        final HBox row = new HBox(SPACING);
        row.getChildren().add(new Text(eta.getEtaString()));
        row.getChildren().add(Utils.etaText(now, eta.getEta()));
        toDestColumn.getChildren().add(row);
      }
      vBox.getChildren().add(toDestRow);
    }
    return vBox;
  }

  Scene newScene(ScheduledThreadPoolExecutor scheduler) {
    final Scene scene = new Scene(newEtaBox());
    scheduler.scheduleAtFixedRate(() -> scene.setRoot(newEtaBox()), 3, 3, TimeUnit.SECONDS);
    return scene;
  }

  Stage newStage(StreetMapApp streetMapApp, ScheduledThreadPoolExecutor scheduler) {
    final Stage dialog = new Stage();
//    dialog.initModality(Modality.APPLICATION_MODAL);
    final StreetMapApp.AppProperties p = streetMapApp.getAppProperties();
    dialog.initOwner(p.getStage());
    dialog.setTitle("" + stop);
//    dialog.setOpacity(.8);
    dialog.setScene(newScene(scheduler));
    dialog.sizeToScene();
    dialog.setResizable(false);
    dialog.show();

    final IntegerCoordinate position = p.getPosition(dialog.getWidth(), dialog.getHeight());
    dialog.setX(position.getX());
    dialog.setY(position.getY());
    return dialog;
  }

  void show() {
    stage.show();
    stage.toFront();
  }
}
