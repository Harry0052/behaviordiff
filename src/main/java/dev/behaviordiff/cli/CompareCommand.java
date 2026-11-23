package dev.behaviordiff.cli;

import dev.behaviordiff.compare.Comparison;
import dev.behaviordiff.compare.Report;
import dev.behaviordiff.contract.ContractException;
import dev.behaviordiff.contract.Journey;
import dev.behaviordiff.contract.JourneyValidator;
import dev.behaviordiff.report.CanonicalJson;
import dev.behaviordiff.report.TextSummary;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
    name = "compare",
    description = "Run one journey on two checked-in builds and write a canonical JSON report.")
public final class CompareCommand implements Callable<Integer> {
  @CommandLine.Spec CommandLine.Model.CommandSpec spec;

  @CommandLine.Option(names = "--journey", required = true, description = "Journey JSON file.")
  Path journey;

  @CommandLine.Option(names = "--left", required = true, description = "Checked-in build id.")
  String left;

  @CommandLine.Option(names = "--right", required = true, description = "Checked-in build id.")
  String right;

  @CommandLine.Option(
      names = "--output",
      required = true,
      description = "Canonical JSON path inside the working directory.")
  Path output;

  @CommandLine.Option(
      names = "--approve",
      description = "Approve one precise field change, such as orders.destination.",
      split = ",")
  List<String> approve = new ArrayList<>();

  @Override
  public Integer call() {
    Path target;
    try {
      target = OutputGuard.checked(output);
    } catch (ContractException ex) {
      spec.commandLine().getErr().println(ex.format());
      return 3;
    }
    try {
      byte[] bytes = ValidateCommand.readCapped(journey);
      Journey parsed = new JourneyValidator().parse(bytes);
      List<String> approvals = approve == null ? List.of() : approve;
      Report report = Comparison.run(parsed, left, right, approvals);
      write(target, report);
      spec.commandLine().getOut().print(TextSummary.format(report, journey.toString()));
      spec.commandLine().getOut().flush();
      return report.exitCode();
    } catch (ContractException ex) {
      spec.commandLine().getErr().println(ex.format());
      write(target, Report.rejected(ex));
      return 3;
    } catch (RuntimeException ex) {
      spec.commandLine().getErr().println("INCOMPLETE_EVIDENCE: " + ex.getMessage());
      return 2;
    }
  }

  private static void write(Path target, Report report) {
    try {
      if (target.getParent() != null) {
        Files.createDirectories(target.getParent());
      }
      Files.write(target, CanonicalJson.write(report.toMap()));
    } catch (java.io.IOException ex) {
      throw new ContractException(
          dev.behaviordiff.contract.ErrorCategory.INCOMPLETE_EVIDENCE,
          "cannot write report '" + target + "': " + ex.getMessage(),
          ex);
    }
  }
}
