package dev.behaviordiff.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class ValidateCommandTest {
  @Test
  void acceptsTheValidFixture() {
    Run run = execute("validate", "fixtures/journeys/cancel-o1.valid.json");
    assertEquals(0, run.code);
    assertEquals("accepted fixture cancel-o1 seed seed-cancel-o1 actions 1", run.out.trim());
    assertEquals("", run.err.trim());
  }

  @Test
  void rejectsTheUnknownActionFixture() {
    Run run = execute("validate", "fixtures/journeys/unknown-action.invalid.json");
    assertEquals(3, run.code);
    assertTrue(run.err.startsWith("UNKNOWN_ACTION:"));
    assertTrue(run.err.contains("teleport_order"));
    assertTrue(run.out.isBlank());
  }

  @Test
  void rejectsAMissingFile() {
    Run run = execute("validate", "fixtures/journeys/missing.json");
    assertEquals(3, run.code);
    assertTrue(run.err.startsWith("MALFORMED_INPUT:"));
    assertTrue(run.err.contains("cannot read fixture"));
  }

  @Test
  void helpExitsZero() {
    Run run = execute("--help");
    assertEquals(0, run.code);
    assertTrue(run.out.contains("behaviordiff"));
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
