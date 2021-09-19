package s29752.kmb;

import com.google.gson.JsonElement;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Utils {
  static final int SECONDS_IN_HOUR = 3600;
  static final int SECONDS_IN_MINUTES = 60;

  static final ZoneId HKT = ZoneId.ofOffset("", ZoneOffset.ofHours(8));

  static ZonedDateTime now() {
    return ZonedDateTime.now(HKT);
  }

  static ZonedDateTime toZonedDateTime(JsonElement e) {
    if (e.isJsonNull()) {
      return null;
    }
    return toZonedDateTime(e.getAsString());
  }

  static ZonedDateTime toZonedDateTime(String s) {
    if (s == null || s.equals("null")) {
      return null;
    }
    return LocalDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME).atZone(HKT);
  }

  static Text newText(String s, Paint paint) {
    final Text text = new Text(s);
    text.setFill(paint);
    return text;
  }

  static Text etaText(ZonedDateTime now, ZonedDateTime eta) {
    return eta == null? newText("UNKNOWN", Color.RED): etaText(Duration.between(now, eta));
  }

  static Text etaText(Duration duration) {
    long seconds = duration.getSeconds();
    if (seconds <= 0) {
      return newText("ARRIVED", Color.RED);
    }
    final StringBuilder b = new StringBuilder();
    if (seconds > SECONDS_IN_HOUR) {
      b.append(seconds / SECONDS_IN_HOUR).append("hr ");
    }
    seconds %= SECONDS_IN_HOUR;
    b.append(seconds / SECONDS_IN_MINUTES).append("min ");
    b.append(seconds % SECONDS_IN_MINUTES).append("s");
    return newText(b.toString(), Color.BLUE);
  }
}
