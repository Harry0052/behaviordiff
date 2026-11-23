package dev.behaviordiff.report;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Canonical JSON: sorted keys, no insignificant whitespace, one trailing LF, HTML escaped. */
public final class CanonicalJson {
  private CanonicalJson() {}

  public static byte[] write(Object value) {
    StringBuilder out = new StringBuilder();
    append(out, value);
    out.append('\n');
    return out.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static void append(StringBuilder out, Object value) {
    switch (value) {
      case null -> out.append("null");
      case String text -> out.append(quote(text));
      case Integer number -> out.append(number.intValue());
      case Boolean flag -> out.append(flag.booleanValue() ? "true" : "false");
      case List<?> list -> {
        out.append('[');
        for (int i = 0; i < list.size(); i++) {
          if (i > 0) {
            out.append(',');
          }
          append(out, list.get(i));
        }
        out.append(']');
      }
      case Map<?, ?> map -> {
        List<String> keys = map.keySet().stream().map(String.class::cast).sorted().toList();
        out.append('{');
        for (int i = 0; i < keys.size(); i++) {
          if (i > 0) {
            out.append(',');
          }
          String key = keys.get(i);
          append(out, key);
          out.append(':');
          append(out, map.get(key));
        }
        out.append('}');
      }
      default -> throw new IllegalArgumentException("Cannot encode " + value.getClass().getName());
    }
  }

  static String quote(String text) {
    StringBuilder out = new StringBuilder(text.length() + 2);
    out.append('"');
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\b' -> out.append("\\b");
        case '\f' -> out.append("\\f");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        case '<' -> out.append("\\u003c");
        case '>' -> out.append("\\u003e");
        case '&' -> out.append("\\u0026");
        default -> {
          if (c < 0x20) {
            out.append(String.format("\\u%04x", (int) c));
          } else {
            out.append(c);
          }
        }
      }
    }
    out.append('"');
    return out.toString();
  }
}
