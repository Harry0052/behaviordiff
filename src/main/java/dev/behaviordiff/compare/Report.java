package dev.behaviordiff.compare;

import dev.behaviordiff.contract.ContractException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Schema version 1 report. {@link #toMap()} is the canonical document model. */
public final class Report {
  public static final String LIMITATION =
      "Coverage describes the observed journey and the configured tables and sinks only.";

  private final Map<String, Object> body;
  private final String status;

  private Report(String status, Map<String, Object> body) {
    this.status = status;
    this.body = body;
  }

  public String status() {
    return status;
  }

  public Map<String, Object> toMap() {
    return body;
  }

  public int exitCode() {
    return switch (status) {
      case "identical", "approved" -> 0;
      case "changes" -> 1;
      case "incomplete" -> 2;
      case "rejected" -> 3;
      default -> 2;
    };
  }

  public static Report rejected(ContractException ex) {
    return terminal("rejected", ex.category().name(), ex.getMessage());
  }

  public static Report incomplete(String message) {
    return terminal("incomplete", "INCOMPLETE_EVIDENCE", message);
  }

  public static Report success(
      String status,
      String journey,
      String seed,
      String clock,
      String leftBuild,
      String rightBuild,
      String earliestChangedStep,
      boolean responseChanged,
      List<String> approvals,
      List<Map<String, Object>> rowChanges,
      List<Map<String, Object>> effectChanges,
      List<String> steps) {
    Map<String, Object> coverage = new LinkedHashMap<>();
    coverage.put("limitation", LIMITATION);
    coverage.put("seed", seed);
    coverage.put("sinks", List.of("email", "shipping"));
    coverage.put("steps", List.copyOf(steps));
    coverage.put("tables", List.of("orders", "reservations"));

    Map<String, Object> units = new LinkedHashMap<>();
    units.put("amountCents", "USD cents");
    units.put("quantity", "item count");

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("approvals", List.copyOf(approvals));
    body.put("clock", clock);
    body.put("coverage", coverage);
    body.put("earliestChangedStep", earliestChangedStep);
    body.put("effectChanges", List.copyOf(effectChanges));
    body.put("journey", journey);
    body.put("leftBuild", leftBuild);
    body.put("responseChanged", Boolean.valueOf(responseChanged));
    body.put("rightBuild", rightBuild);
    body.put("rowChanges", List.copyOf(rowChanges));
    body.put("schemaVersion", "1");
    body.put("seed", seed);
    body.put("status", status);
    body.put("units", units);
    return new Report(status, body);
  }

  private static Report terminal(String status, String category, String message) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("category", category);
    body.put("coverage", null);
    body.put("message", message);
    body.put("schemaVersion", "1");
    body.put("status", status);
    return new Report(status, body);
  }
}
