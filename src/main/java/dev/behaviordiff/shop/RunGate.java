package dev.behaviordiff.shop;

import dev.behaviordiff.contract.ContractException;
import dev.behaviordiff.contract.ErrorCategory;
import java.util.Set;
import java.util.TreeSet;

/** Fail setup before a build runs when instrumentation or sinks are outside the contract. */
public final class RunGate {
  public static final Set<String> REQUIRED_TABLES = Set.of("orders", "reservations");
  public static final Set<String> IN_PROCESS_SINKS = Set.of("email", "shipping");

  private RunGate() {}

  public static void check(ShopBuild build) {
    if (!build.observedTables().containsAll(REQUIRED_TABLES)) {
      TreeSet<String> missing = new TreeSet<>(REQUIRED_TABLES);
      missing.removeAll(build.observedTables());
      throw new ContractException(
          ErrorCategory.UNOBSERVED_TABLE,
          "build '"
              + build.id()
              + "' does not observe "
              + missing
              + ". Both orders and reservations are required, so this run has no coverage.");
    }
    for (String sink : build.declaredSinks()) {
      if (!IN_PROCESS_SINKS.contains(sink) || sink.indexOf(':') >= 0 || sink.indexOf('/') >= 0) {
        throw new ContractException(
            ErrorCategory.EXTERNAL_SINK,
            "build '"
                + build.id()
                + "' declares sink '"
                + sink
                + "', which is not an in-process test sink. Allowed sinks: "
                + new TreeSet<>(IN_PROCESS_SINKS)
                + ". The run was not started.");
      }
    }
  }
}
