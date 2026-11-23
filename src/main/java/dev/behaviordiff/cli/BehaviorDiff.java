package dev.behaviordiff.cli;

import picocli.CommandLine;

@CommandLine.Command(
    name = "behaviordiff",
    mixinStandardHelpOptions = true,
    version = "behaviordiff 0.1.0",
    description = "Compare database writes and in-process effects of two local demo-shop builds.",
    exitCodeOnInvalidInput = 3,
    exitCodeOnExecutionException = 2,
    subcommands = {ValidateCommand.class, CompareCommand.class})
public final class BehaviorDiff implements Runnable {
  @Override
  public void run() {
    CommandLine.usage(this, System.out);
  }

  public static void main(String[] args) {
    int code = new CommandLine(new BehaviorDiff()).execute(args);
    System.exit(code);
  }
}
