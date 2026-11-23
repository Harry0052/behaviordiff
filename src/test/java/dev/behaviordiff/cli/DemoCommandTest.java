package dev.behaviordiff.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class DemoCommandTest {
  @Test
  void demoDuplicateCancelIsInspectable() throws Exception {
    Path output = Path.of("out", "demo-duplicate-cancel.json");
    Run run = execute("demo", "--case", "duplicate-cancel", "--output", output.toString());
    assertEquals(1, run.code);
    assertTrue(run.out.contains("earliest changed step: cancel-order"));
    assertTrue(run.out.contains("fixtures/journeys/cancel-o1.valid.json"));
    assertTrue(run.out.contains("ship-cancel:O-1"));
    assertTrue(run.out.contains("USD cents"));
    assertTrue(run.out.contains("limitation:"));
    assertTrue(run.out.contains("export:"));
    String json = Files.readString(output);
    assertTrue(json.contains("\"actionId\":\"cancel-order\""));
    assertTrue(json.contains("\"journey\":\"cancel-o1\""));
    assertTrue(json.contains("\"key\":\"R-1\""));
  }

  @Test
  void unknownCaseExplainsTheChoices() {
    Run run = execute("demo", "--case", "not-a-case");
    assertEquals(3, run.code);
    assertTrue(run.err.startsWith("MALFORMED_INPUT:"));
    assertTrue(run.err.contains("duplicate-cancel"));
    assertTrue(run.err.contains("not-a-case"));
  }

  @Test
  void partialCaseDoesNotClaimCoverage() throws Exception {
    Path output = Path.of("out", "demo-partial.json");
    Run run = execute("demo", "--case", "partial-tables", "--output", output.toString());
    assertEquals(3, run.code);
    assertTrue(run.err.startsWith("UNOBSERVED_TABLE:"));
    assertTrue(run.out.contains("coverage was not claimed"));
    assertTrue(Files.readString(output).contains("\"coverage\":null"));
  }

  @Test
  void approvalOfTheDestinationFieldExitsZero() {
    Path output = Path.of("out", "demo-approved.json");
    Run run =
        execute(
            "demo",
            "--case",
            "volatile-destination",
            "--approve",
            "orders.destination",
            "--output",
            output.toString());
    assertEquals(0, run.code);
    assertTrue(run.out.contains("status: approved"));
    assertTrue(run.out.contains("dock-b"));
  }

  @Test
  void helpListsDemo() {
    Run run = execute("--help");
    assertEquals(0, run.code);
    assertTrue(run.out.contains("demo"));
    assertTrue(run.out.contains("compare"));
    assertTrue(run.out.contains("validate"));
  }

  private static Run execute(String... args) {
    CommandLine cmd = new CommandLine(new BehaviorDiff());
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    cmd.setOut(new PrintWriter(out, true));
    cmd.setErr(new PrintWriter(err, true));
    int code = cmd.execute(args);
    return new Run(code, out.toString(), err.toString());
  }

  private record Run(int code, String out, String err) {}
}
