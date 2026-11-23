package dev.behaviordiff.contract;

import java.util.List;
import java.util.Map;

/** Versioned journey. Lists and maps are unmodifiable. */
public record Journey(
    String schemaVersion, String name, String seed, String clock, List<Action> actions) {

  public Journey {
    actions = List.copyOf(actions);
  }

  public record Action(String id, String name, Map<String, String> input) {
    public Action {
      input = Map.copyOf(input);
    }
  }
}
