package dev.behaviordiff.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class CompareCommandTest {
  @Test
  void writesTheDuplicateCancelReport() throws Exception {
    Path output = Path.of("out", "duplicate-cancel.json");
    Files.createDirectories(output.getParent());
    Run run =
        execute(
            "compare",
            "--journey",
            "fixtures/journeys/cancel-o1.valid.json",
            "--left",
            "good",
            "--right",
            "duplicate-cancel",
            "--output",
            output.toString());
    assertEquals(1, run.code);
    assertTrue(run.out.contains("earliest changed step: cancel-order"));
    assertTrue(run.out.contains("ship-cancel:O-1"));
    assertTrue(run.out.contains("fixtures/journeys/cancel-o1.valid.json"));
    String json = Files.readString(output);
    assertTrue(json.contains("\"leftCount\":1"));
    assertTrue(json.contains("\"rightCount\":2"));
    assertFalse(json.contains("req-"));
    byte[] again = Files.readAllBytes(output);
    Run second =
        execute(
            "compare",
            "--journey",
            "fixtures/journeys/cancel-o1.valid.json",
            "--left",
            "good",
            "--right",
            "duplicate-cancel",
            "--output",
            output.toString());
    assertEquals(1, second.code);
    assertEquals(new String(again, StandardCharsets.UTF_8), Files.readString(output));
  }

  @Test
  void rejectsUnknownActionBeforeCoverage() throws Exception {
    Path output = Path.of("out", "unknown-action.json");
    Run run =
        execute(
            "compare",
            "--journey",
            "fixtures/journeys/unknown-action.invalid.json",
            "--left",
            "good",
            "--right",
            "good",
            "--output",
            output.toString());
    assertEquals(3, run.code);
    assertTrue(run.err.startsWith("UNKNOWN_ACTION:"));
    String json = Files.readString(output);
    assertTrue(json.contains("\"status\":\"rejected\""));
    assertTrue(json.contains("\"coverage\":null"));
  }

  @Test
  void rejectsAnOutputPathOutsideTheWorkingDirectory() {
    Run run =
        execute(
            "compare",
            "--journey",
            "fixtures/journeys/cancel-o1.valid.json",
            "--left",
            "good",
            "--right",
            "good",
            "--output",
            "/tmp/behaviordiff-outside.json");
    assertEquals(3, run.code);
    assertTrue(run.err.startsWith("MALFORMED_INPUT:"));
    assertFalse(Files.exists(Path.of("/tmp/behaviordiff-outside.json")));
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
