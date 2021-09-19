package s29752.kmb;

import java.util.Objects;

class IntegerCoordinate {

  private final int x;
  private final int y;

  IntegerCoordinate(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  IntegerCoordinate orthogonal() {
    return new IntegerCoordinate(-y, x);
  }

  IntegerCoordinate subtract(IntegerCoordinate that) {
    return new IntegerCoordinate(this.getX() - that.getX(),
        this.getY() - that.getY());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof IntegerCoordinate)) {
      return false;
    }
    final IntegerCoordinate that = (IntegerCoordinate) obj;
    return this.x == that.x && this.y == that.y;
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y);
  }

  @Override
  public String toString() {
    return "(" + x + ", " + y + ")";
  }
}
