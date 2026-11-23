package dev.behaviordiff.shop;

import java.util.LinkedHashMap;
import java.util.Map;

/** One outbound effect observed during a step. Payload values may be null. */
public record Effect(
    String sink,
    String effectType,
    String businessKey,
    String actionId,
    int sequence,
    Map<String, Object> payload) {

  public Effect {
    payload = new LinkedHashMap<>(payload);
  }
}
