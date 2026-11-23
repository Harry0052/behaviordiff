package dev.behaviordiff.shop;

import java.util.List;
import java.util.Map;

/** Tables immediately before and after one action, plus the effects of that action. */
public record StepObservation(
    String actionId,
    int httpStatus,
    Map<String, Object> responseBody,
    Map<String, List<Map<String, Object>>> before,
    Map<String, List<Map<String, Object>>> after,
    List<Effect> effects,
    boolean truncated) {}
