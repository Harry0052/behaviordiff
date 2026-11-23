package dev.behaviordiff.shop;

import java.util.List;

/** Everything one build did with a journey. Incomplete runs are not coverage. */
public record Observation(
    String buildId, boolean complete, String incompleteReason, List<StepObservation> steps) {

  public Observation {
    steps = List.copyOf(steps);
  }

  public static Observation ok(String buildId, List<StepObservation> steps) {
    return new Observation(buildId, true, null, steps);
  }

  public static Observation incomplete(String buildId, String reason) {
    return new Observation(buildId, false, reason, List.of());
  }
}
