package dev.behaviordiff.report;

import dev.behaviordiff.compare.Report;
import java.util.List;
import java.util.Map;

/** Human summary. Values are JSON-encoded so the text is plain and escaped. */
public final class TextSummary {
  private TextSummary() {}

  public static String format(Report report, String fixturePath) {
    Map<String, Object> body = report.toMap();
    StringBuilder out = new StringBuilder();
    out.append("BehaviorDiff 0.1.0\n");
    if (fixturePath != null) {
      out.append("fixture: ").append(fixturePath).append('\n');
    }
    out.append("status: ").append(body.get("status")).append('\n');
    if (body.get("category") != null) {
      out.append("category: ").append(body.get("category")).append('\n');
      out.append("message: ").append(body.get("message")).append('\n');
      out.append("coverage: none\n");
      out.append("This run is not successful coverage. Fix the message above and retry.\n");
      return out.toString();
    }
    out.append("journey: ").append(body.get("journey")).append('\n');
    out.append("seed: ").append(body.get("seed")).append('\n');
    out.append("clock: ").append(body.get("clock")).append('\n');
    out.append("left: ").append(body.get("leftBuild")).append('\n');
    out.append("right: ").append(body.get("rightBuild")).append('\n');
    out.append("earliest changed step: ").append(body.get("earliestChangedStep")).append('\n');
    out.append("response changed: ").append(body.get("responseChanged")).append('\n');
    out.append("units: amountCents in USD cents; quantity in item count\n");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("rowChanges");
    if (rows.isEmpty()) {
      out.append("row changes: none\n");
    }
    for (Map<String, Object> row : rows) {
      out.append("row ")
          .append(row.get("table"))
          .append(" key ")
          .append(row.get("key"))
          .append(" step ")
          .append(row.get("actionId"))
          .append('\n');
      out.append("  left ")
          .append(row.get("leftOp"))
          .append(" before ")
          .append(json(row.get("leftBefore")))
          .append(" after ")
          .append(json(row.get("leftAfter")))
          .append('\n');
      out.append("  right ")
          .append(row.get("rightOp"))
          .append(" before ")
          .append(json(row.get("rightBefore")))
          .append(" after ")
          .append(json(row.get("rightAfter")))
          .append('\n');
      out.append("  changed fields: ").append(row.get("changedFields")).append('\n');
    }
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> effects = (List<Map<String, Object>>) body.get("effectChanges");
    if (effects.isEmpty()) {
      out.append("effect changes: none\n");
    }
    for (Map<String, Object> effect : effects) {
      out.append("effect ")
          .append(effect.get("sink"))
          .append(' ')
          .append(effect.get("effectType"))
          .append(' ')
          .append(effect.get("businessKey"))
          .append(" step ")
          .append(effect.get("actionId"))
          .append('\n');
      out.append("  count ")
          .append(effect.get("leftCount"))
          .append(" versus ")
          .append(effect.get("rightCount"))
          .append('\n');
      out.append("  sequences ")
          .append(effect.get("leftSequences"))
          .append(" versus ")
          .append(effect.get("rightSequences"))
          .append('\n');
      out.append("  cause: ").append(effect.get("cause")).append('\n');
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> coverage = (Map<String, Object>) body.get("coverage");
    out.append("coverage steps: ").append(coverage.get("steps")).append('\n');
    out.append("coverage tables: ").append(coverage.get("tables")).append('\n');
    out.append("coverage sinks: ").append(coverage.get("sinks")).append('\n');
    out.append("limitation: ").append(coverage.get("limitation")).append('\n');
    return out.toString();
  }

  private static String json(Object value) {
    return new String(CanonicalJson.write(value), java.nio.charset.StandardCharsets.UTF_8).trim();
  }
}
