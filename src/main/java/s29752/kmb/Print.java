package s29752.kmb;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

class Print {
  static final AtomicBoolean isDebug = new AtomicBoolean();

  static synchronized void println(String s, Consumer<String> out) {
    final int max = 1000;
    out.accept(s == null ? "<EMPTY_LINE>" : s.length() < max ? s : s.substring(0, max) + " ...");
  }

  static void ln() {
    ln("");
  }

  static void ln(Object s) {
    println(Objects.toString(s), System.out::println);
  }

  static void debug(String s) {
    if (isDebug.get()) {
      println(s, System.err::println);
    }
  }
}
