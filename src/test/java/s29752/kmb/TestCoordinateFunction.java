package s29752.kmb;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestCoordinateFunction {
  @Test
  public void testApply() {
    final List<AssertionError> errors = new ArrayList<>();
    for(int x = 0; x <= 1000; x += 50) {
      for(int y = 0; y <= 1000; y += 50) {
        final IntegerCoordinate original = new IntegerCoordinate(x, y);
        final GeographicCoordinate geo = CoordinateFunction.getInstance().apply(original);
        final IntegerCoordinate applied = CoordinateFunction.getInstance().apply(geo);
        try {
          Assert.assertEquals(original, applied);
        } catch (AssertionError e) {
          e.printStackTrace();
          errors.add(e);
        }
      }
    }
    Assert.assertEquals(0, errors.size());
  }

  @Test
  public void testThreePoints() {
    final CoordinateFunction.BusStopCoordinates A = CoordinateFunction.PO_TSZ_LANE;
    final CoordinateFunction.BusStopCoordinates B = CoordinateFunction.LUNG_POON_COURT;
    final CoordinateFunction.BusStopCoordinates C = CoordinateFunction.CHI_LIN_NUNNERY;

//    checkBusStopCoordinates(A);
    checkBusStopCoordinates(B);
    checkBusStopCoordinates(C);
  }

  static void checkBusStopCoordinates(CoordinateFunction.BusStopCoordinates busStopCoordinates) {
    final BusStop stop = busStopCoordinates.getStop();
    final IntegerCoordinate applied = CoordinateFunction.getInstance().apply(stop.getCoordinate());
    Print.ln(stop.getName() + ": " + applied);
    Assert.assertEquals(busStopCoordinates.getIntegerCoordinate(), applied);
  }
}
