package dev.behaviordiff.cli;

import dev.behaviordiff.compare.Comparison;
import dev.behaviordiff.compare.Report;
import dev.behaviordiff.contract.ContractException;
import dev.behaviordiff.contract.Journey;
import dev.behaviordiff.contract.JourneyValidator;
import dev.behaviordiff.report.TextSummary;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
    name = "demo",
    description = "Run a named case, print the first changed step, and export canonical JSON.")
public final class DemoCommand implements Callable<Integer> {
  @CommandLine.Spec CommandLine.Model.CommandSpec spec;

  @CommandLine.Option(
      names = "--case",
      required = true,
      description = "Demo case. Try duplicate-cancel.")
  String caseName;

  @CommandLine.Option(
      names = "--output",
      description = "Canonical JSON path. Defaults to out/<case>.json.")
  Path output;

  @CommandLine.Option(
      names = "--approve",
      description = "Approve one precise field change, such as orders.destination.",
      split = ",")
  List<String> approve = new ArrayList<>();

  @Override
  public Integer call() {
    Demos.Case demo;
    try {
      demo = Demos.get(caseName);
    } catch (ContractException ex) {
      spec.commandLine().getErr().println(ex.format());
      return 3;
    }
    Path target = output == null ? Path.of("out", demo.name() + ".json") : output;
    try {
      target = OutputGuard.checked(target);
    } catch (ContractException ex) {
      spec.commandLine().getErr().println(ex.format());
      return 3;
    }
    try {
      Journey journey = new JourneyValidator().parse(ValidateCommand.readCapped(Path.of(Demos.JOURNEY)));
      List<String> approvals = approve == null ? List.of() : approve;
      Report report = Comparison.run(journey, demo.left(), demo.right(), approvals);
      ReportFiles.write(target, report);
      spec.commandLine()
          .getOut()
          .print(TextSummary.format(report, Demos.JOURNEY));
      spec.commandLine().getOut().println("export: " + target);
      spec.commandLine().getOut().flush();
      return report.exitCode();
    } catch (ContractException ex) {
      spec.commandLine().getErr().println(ex.format());
      ReportFiles.write(target, Report.rejected(ex));
      spec.commandLine().getOut().println("export: " + target);
      spec.commandLine()
          .getOut()
          .println("coverage was not claimed. Read the error, then fix the case or the fixture.");
      spec.commandLine().getOut().flush();
      return 3;
    }
  }
}
