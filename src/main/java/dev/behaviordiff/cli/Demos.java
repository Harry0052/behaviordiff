package dev.behaviordiff.cli;

import dev.behaviordiff.contract.ContractException;
import dev.behaviordiff.contract.ErrorCategory;
import java.util.List;
import java.util.TreeSet;

/** Named journeys an engineer can run without choosing build ids. */
public final class Demos {
  public static final String JOURNEY = "fixtures/journeys/cancel-o1.valid.json";

  private Demos() {}

  public record Case(String name, String left, String right) {}

  public static List<String> names() {
    return List.copyOf(new TreeSet<>(List.of(
        "duplicate-cancel",
        "unexpected-delete",
        "volatile-destination",
        "amount-change",
        "missing-email",
        "null-vs-absent",
        "identical-control",
        "external-sink",
        "partial-tables",
        "broken-adapter")));
  }

  public static Case get(String name) {
    return switch (name) {
      case "duplicate-cancel" -> new Case(name, "good", "duplicate-cancel");
      case "unexpected-delete" -> new Case(name, "retain-reservation", "drop-reservation");
      case "volatile-destination" -> new Case(name, "good", "reroute");
      case "amount-change" -> new Case(name, "good", "reprice");
      case "missing-email" -> new Case(name, "email-cancel", "silent-cancel");
      case "null-vs-absent" -> new Case(name, "null-template", "absent-template");
      case "identical-control" -> new Case(name, "good", "good");
      case "external-sink" -> new Case(name, "good", "external-sink");
      case "partial-tables" -> new Case(name, "good", "partial-tables");
      case "broken-adapter" -> new Case(name, "good", "broken-adapter");
      default ->
          throw new ContractException(
              ErrorCategory.MALFORMED_INPUT,
              "case '"
                  + name
                  + "' is not a demo. Known cases: "
                  + names()
                  + ". Start with duplicate-cancel.");
    };
  }
}
