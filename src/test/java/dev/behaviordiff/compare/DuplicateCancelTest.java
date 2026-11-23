package dev.behaviordiff.compare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.behaviordiff.contract.ContractException;
import dev.behaviordiff.contract.ErrorCategory;
import dev.behaviordiff.contract.Journey;
import dev.behaviordiff.contract.JourneyValidator;
import dev.behaviordiff.report.CanonicalJson;
import dev.behaviordiff.shop.Builds;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Hand oracle: fixtures/oracle/duplicate-cancel.md. */
class DuplicateCancelTest {
  private static Journey journey;

  @BeforeAll
  static void load() throws Exception {
    journey =
        new JourneyValidator()
            .parse(Files.readAllBytes(Path.of("fixtures/journeys/cancel-o1.valid.json")));
  }

  @Test
  void duplicateCancelMatchesTheHandOracle() {
    Report report = Comparison.run(journey, "good", "duplicate-cancel", List.of());
    Map<String, Object> body = report.toMap();
    assertEquals("changes", report.status());
    assertEquals(1, report.exitCode());
    assertEquals("cancel-order", body.get("earliestChangedStep"));
    assertEquals(Boolean.FALSE, body.get("responseChanged"));

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("rowChanges");
    assertEquals(1, rows.size());
    Map<String, Object> row = rows.get(0);
    assertEquals("reservations", row.get("table"));
    assertEquals("R-1", row.get("key"));
    assertEquals("delete", row.get("leftOp"));
    assertEquals("none", row.get("rightOp"));
    assertNull(row.get("leftAfter"));
    @SuppressWarnings("unchecked")
    Map<String, Object> retained = (Map<String, Object>) row.get("rightAfter");
    assertEquals("R-1", retained.get("reservationId"));
    assertEquals("O-1", retained.get("orderId"));
    assertEquals("SKU-1", retained.get("sku"));
    assertEquals(1, retained.get("quantity"));

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> effects = (List<Map<String, Object>>) body.get("effectChanges");
    assertEquals(1, effects.size());
    Map<String, Object> effect = effects.get(0);
    assertEquals("shipping", effect.get("sink"));
    assertEquals("cancel_shipment", effect.get("effectType"));
    assertEquals("ship-cancel:O-1", effect.get("businessKey"));
    assertEquals(1, effect.get("leftCount"));
    assertEquals(2, effect.get("rightCount"));
    assertEquals(List.of(1), effect.get("leftSequences"));
    assertEquals(List.of(1, 2), effect.get("rightSequences"));
    assertEquals("duplicate-idempotency-key", effect.get("cause"));
    assertEquals("cancel-order", effect.get("actionId"));
  }

  @Test
  void identicalBuildHasNoChangesAndIsByteStable() {
    byte[] first = CanonicalJson.write(Comparison.run(journey, "good", "good", List.of()).toMap());
    byte[] second = CanonicalJson.write(Comparison.run(journey, "good", "good", List.of()).toMap());
    assertEquals(
        new String(first, StandardCharsets.UTF_8), new String(second, StandardCharsets.UTF_8));
    Report report = Comparison.run(journey, "good", "good", List.of());
    assertEquals("identical", report.status());
    assertEquals(0, report.exitCode());
    assertNull(report.toMap().get("earliestChangedStep"));
    assertEquals(List.of(), report.toMap().get("rowChanges"));
    assertEquals(List.of(), report.toMap().get("effectChanges"));
    byte[] again = CanonicalJson.write(report.toMap());
    byte[] duplicate =
        CanonicalJson.write(Comparison.run(journey, "good", "duplicate-cancel", List.of()).toMap());
    assertEquals(
        new String(again, StandardCharsets.UTF_8),
        new String(CanonicalJson.write(report.toMap()), StandardCharsets.UTF_8));
    assertFalse(new String(duplicate, StandardCharsets.UTF_8).contains("req-"));
    assertTrue(new String(duplicate, StandardCharsets.UTF_8).contains("ship-cancel:O-1"));
  }

  @Test
  void hiddenDeletionSurvivesEqualResponseAndEffectCount() {
    Report report = Comparison.run(journey, "retain-reservation", "drop-reservation", List.of());
    Map<String, Object> body = report.toMap();
    assertEquals(Boolean.FALSE, body.get("responseChanged"));
    assertEquals(List.of(), body.get("effectChanges"));
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("rowChanges");
    assertEquals(1, rows.size());
    assertEquals("none", rows.get(0).get("leftOp"));
    assertEquals("delete", rows.get(0).get("rightOp"));
    assertEquals("R-1", rows.get(0).get("key"));
  }

  @Test
  void volatileRequestIdIsIgnoredAndDestinationIsKept() {
    Report report = Comparison.run(journey, "good", "reroute", List.of());
    String json = new String(CanonicalJson.write(report.toMap()), StandardCharsets.UTF_8);
    assertFalse(json.contains("req-"));
    assertFalse(json.contains("requestId"));
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> rows = (List<Map<String, Object>>) report.toMap().get("rowChanges");
    assertEquals(1, rows.size());
    assertEquals(List.of("destination"), rows.get(0).get("changedFields"));
    @SuppressWarnings("unchecked")
    Map<String, Object> after = (Map<String, Object>) rows.get(0).get("rightAfter");
    assertEquals("dock-b", after.get("destination"));
    assertEquals(2500, after.get("amountCents"));
  }

  @Test
  void amountChangeIsRetained() {
    Report report = Comparison.run(journey, "good", "reprice", List.of());
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> rows = (List<Map<String, Object>>) report.toMap().get("rowChanges");
    assertEquals(List.of("amountCents"), rows.get(0).get("changedFields"));
    @SuppressWarnings("unchecked")
    Map<String, Object> before = (Map<String, Object>) rows.get(0).get("leftAfter");
    @SuppressWarnings("unchecked")
    Map<String, Object> after = (Map<String, Object>) rows.get(0).get("rightAfter");
    assertEquals(2500, before.get("amountCents"));
    assertEquals(1999, after.get("amountCents"));
  }

  @Test
  void missingEffectAndNullVersusAbsentAreDistinct() {
    Report missing = Comparison.run(journey, "email-cancel", "silent-cancel", List.of());
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> missingEffects =
        (List<Map<String, Object>>) missing.toMap().get("effectChanges");
    assertEquals("missing-effect", missingEffects.get(0).get("cause"));
    assertEquals(1, missingEffects.get(0).get("leftCount"));
    assertEquals(0, missingEffects.get(0).get("rightCount"));
    assertEquals("email:O-1:canceled", missingEffects.get(0).get("businessKey"));

    Report nullAbsent = Comparison.run(journey, "null-template", "absent-template", List.of());
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> effects =
        (List<Map<String, Object>>) nullAbsent.toMap().get("effectChanges");
    assertEquals(1, effects.size());
    assertEquals("null-vs-absent", effects.get(0).get("cause"));
    assertEquals(1, effects.get(0).get("leftCount"));
    assertEquals(1, effects.get(0).get("rightCount"));
  }

  @Test
  void externalSinkAndUnobservedTableDoNotRun() {
    Builds.External external = new Builds.External();
    ContractException externalError =
        org.junit.jupiter.api.Assertions.assertThrows(
            ContractException.class,
            () -> Comparison.run(journey, Builds.get("good"), external, List.of()));
    assertEquals(ErrorCategory.EXTERNAL_SINK, externalError.category());
    assertEquals(0, external.runs());

    Builds.Partial partial = new Builds.Partial();
    ContractException partialError =
        org.junit.jupiter.api.Assertions.assertThrows(
            ContractException.class,
            () -> Comparison.run(journey, partial, Builds.get("good"), List.of()));
    assertEquals(ErrorCategory.UNOBSERVED_TABLE, partialError.category());
    assertEquals(0, partial.runs());
  }

  @Test
  void brokenAdapterIsIncomplete() {
    Report report = Comparison.run(journey, "good", "broken-adapter", List.of());
    assertEquals("incomplete", report.status());
    assertEquals(2, report.exitCode());
    assertNull(report.toMap().get("coverage"));
    assertTrue(String.valueOf(report.toMap().get("message")).contains("adapter exploded"));
  }

  @Test
  void fieldApprovalClearsOnlyThatField() {
    Report approved = Comparison.run(journey, "good", "reroute", List.of("orders.destination"));
    assertEquals("approved", approved.status());
    assertEquals(0, approved.exitCode());
    Report still =
        Comparison.run(journey, "good", "duplicate-cancel", List.of("orders.destination"));
    assertEquals("changes", still.status());
  }
}
