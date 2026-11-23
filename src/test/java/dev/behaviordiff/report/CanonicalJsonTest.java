package dev.behaviordiff.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CanonicalJsonTest {
  @Test
  void escapesMarkupAndSortsKeys() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("b", "a&b");
    body.put("a", "<script>");
    String json = new String(CanonicalJson.write(body), StandardCharsets.UTF_8);
    assertEquals("{\"a\":\"\\u003cscript\\u003e\",\"b\":\"a\\u0026b\"}\n", json);
    assertFalse(json.contains("<"));
    assertFalse(json.contains("&b"));
  }
}
