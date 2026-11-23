package dev.behaviordiff.compare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.behaviordiff.contract.Journey;
import dev.behaviordiff.contract.JourneyValidator;
import dev.behaviordiff.shop.Observation;
import dev.behaviordiff.shop.StepObservation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SafeguardTest {
  @Test
  void truncationIsAnInvalidRun() throws Exception {
    Journey journey =
        new JourneyValidator()
            .parse(Files.readAllBytes(Path.of("fixtures/journeys/cancel-o1.valid.json")));
    Map<String, List<Map<String, Object>>> tables =
        Map.of("orders", List.of(), "reservations", List.of());
    StepObservation step =
        new StepObservation(
            "cancel-order", 200, Map.of("orderId", "O-1"), tables, tables, List.of(), true);
    Observation observation = Observation.ok("good", List.of(step));
    Report report = DiffEngine.compare(journey, observation, observation, List.of());
    assertEquals("incomplete", report.status());
    assertEquals(2, report.exitCode());
    assertNull(report.toMap().get("coverage"));
    assertTrue(String.valueOf(report.toMap().get("message")).contains("truncated"));
  }

  @Test
  void inProcessComparisonStaysInsideTheBudget() throws Exception {
    Journey journey =
        new JourneyValidator()
            .parse(Files.readAllBytes(Path.of("fixtures/journeys/cancel-o1.valid.json")));
    long start = System.nanoTime();
    Report report = Comparison.run(journey, "good", "duplicate-cancel", List.of());
    long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
    assertEquals("changes", report.status());
    assertTrue(elapsedMs < 1000L, "comparison took " + elapsedMs + " ms; budget is 1000 ms");
  }
}
