package s29752.kmb;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class StreetMapApp extends Application {
  static class StopLocations {
    private final Map<IntegerCoordinate, Kmb.Name> map = new ConcurrentHashMap<>();
    private final int grid;

    StopLocations(int grid) {
      this.grid = grid;
    }

    IntegerCoordinate allocate(IntegerCoordinate p, BusStop stop) {
      final IntegerCoordinate rounded = new IntegerCoordinate(
          CoordinateFunction.round(p.getX(), grid),
          CoordinateFunction.round(p.getY(), grid));
      for(int i = 0; ; i++) {
        final IntegerCoordinate c = new IntegerCoordinate(rounded.getX(), rounded.getY() + i*grid);
        final Kmb.Name previous = map.putIfAbsent(c, stop.getName());
        if (previous == null || previous.equals(stop.getName())) {
          return c;
        }
      }
    }
  }

  static class AppProperties {
    private final Stage stage;
    private final IntegerCoordinate initPosition;
    private final AtomicReference<IntegerCoordinate> position;

    AppProperties(Stage stage) {
      this.stage = stage;
      this.initPosition = new IntegerCoordinate((int) stage.getX(), (int) stage.getY());
      this.position = new AtomicReference<>(initPosition);
    }

    Stage getStage() {
      return stage;
    }

    IntegerCoordinate getPosition(double width, double height) {
      return position.getAndUpdate(
          previous -> updateDialogPosition(previous, width, height));
    }

    private IntegerCoordinate updateDialogPosition(IntegerCoordinate previous, double width, double height) {
      Print.debug("previous=" + previous + ", width=" + width + ", height=" + height);
      final Rectangle2D screenBounds = Screen.getPrimary().getBounds();
      if (!(height > 0)) {
        height = 200;
      }
      double x = previous.getX();
      double y = previous.getY() + height;
      if (y > screenBounds.getHeight() - 200) {
        x += width;
        y = 0;
      }
      if (x > screenBounds.getWidth() - 200) {
        return initPosition;
      }
      return new IntegerCoordinate((int)x, (int)y);
    }
  }

  private final StopLocations stopLocations = new StopLocations(32);

  private final String name = "map_kln203.png";
  private final Image mapImage;
  private final AtomicReference<AppProperties> appProperties = new AtomicReference<>();

  public StreetMapApp() throws IOException {
    this.mapImage = SwingFXUtils.toFXImage(ImageIO.read(new File(name)), null);
    Print.ln(this);
  }

  @Override
  public String toString() {
    return name + ": " + (int)mapImage.getWidth() + "x" + (int)mapImage.getHeight();
  }

  ImageView initMapImageView() {
    final ImageView mapView = new ImageView();
    //Setting image to the image view
    mapView.setImage(mapImage);
    //Setting the image view parameters
    mapView.setFitWidth(mapImage.getWidth()/2);
    mapView.setPreserveRatio(true);

    return mapView;
  }

  void mark(CoordinateFunction.BusStopCoordinates coordinates, GraphicsContext graphics) {
    mark(coordinates.getStop(), coordinates.getIntegerCoordinate(), graphics);
  }

  void mark(BusStop stop, GraphicsContext graphics) {
    final IntegerCoordinate p = CoordinateFunction.getInstance().apply(stop.getCoordinate());
    mark(stop, p, graphics);
  }

  void mark(BusStop stop, IntegerCoordinate p, GraphicsContext graphics) {
    final int d = 5;
    final int cx = p.getX() - d/2;
    final int cy = p.getY() - d/2;
    graphics.fillOval(cx, cy, d, d);

    final IntegerCoordinate t = stopLocations.allocate(p, stop);
    graphics.strokeLine(cx, cy, t.getX(), t.getY());
    graphics.fillText("" + stop.getName(), t.getX(), t.getY());
  }

  private Canvas initCanvas() {
    final Canvas canvas = new Canvas(mapImage.getWidth()/2, mapImage.getHeight()/2);
    final GraphicsContext graphics = canvas.getGraphicsContext2D();
    graphics.drawImage(mapImage, 0, 0,mapImage.getWidth()/2, mapImage.getHeight()/2);

    graphics.setFont(Font.font(16));

    graphics.setFill(Color.BLUE);
    final Kmb kmb = Kmb.getInstance();
    for (BusStop stop : kmb.busStops()) {
      mark(stop, graphics);
    }

    graphics.setFill(Color.RED);
    mark(CoordinateFunction.LUNG_POON_COURT, graphics);
    mark(CoordinateFunction.CHI_LIN_NUNNERY, graphics);
    mark(CoordinateFunction.PO_TSZ_LANE, graphics);
    mark(CoordinateFunction.LOK_MOON_HOUSE, graphics);

    return canvas;
  }

  @Override
  public void start(Stage stage) {
    final Canvas canvas = initCanvas();
//    final ImageView mapView = initMapImageView();
    final BusStopEventHandler busStopEventHandler = new BusStopEventHandler(this);
    canvas.addEventFilter(MouseEvent.MOUSE_CLICKED, busStopEventHandler);

    //Setting the Scene object
    final Group root = new Group(canvas);
    final Scene scene = new Scene(root);
    stage.setTitle(name);
    stage.setScene(scene);
    stage.getIcons().add(new Image("file:91M.png"));
    stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, e -> System.exit(0));
    stage.setX(0);
    stage.setY(0);
    stage.show();

    appProperties.set(new AppProperties(stage));
    stage.centerOnScreen();
    stage.setX(Screen.getPrimary().getBounds().getWidth() - stage.getWidth());
  }

  AppProperties getAppProperties() {
    return appProperties.get();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
