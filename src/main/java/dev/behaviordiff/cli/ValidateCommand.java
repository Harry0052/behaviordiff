package dev.behaviordiff.cli;

import dev.behaviordiff.contract.ContractException;
import dev.behaviordiff.contract.ErrorCategory;
import dev.behaviordiff.contract.Journey;
import dev.behaviordiff.contract.JourneyValidator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
    name = "validate",
    description = "Accept a journey fixture or reject it with an actionable contract error.")
public final class ValidateCommand implements Callable<Integer> {
  private final JourneyValidator validator = new JourneyValidator();

  @CommandLine.Spec CommandLine.Model.CommandSpec spec;

  @CommandLine.Parameters(index = "0", description = "Path to a journey JSON file.")
  Path fixture;

  @Override
  public Integer call() {
    try {
      byte[] bytes = readCapped(fixture);
      Journey journey = validator.parse(bytes);
      spec.commandLine()
          .getOut()
          .println(
              "accepted fixture "
                  + journey.name()
                  + " seed "
                  + journey.seed()
                  + " actions "
                  + journey.actions().size());
      spec.commandLine().getOut().flush();
      return 0;
    } catch (ContractException ex) {
      spec.commandLine().getErr().println(ex.format());
      spec.commandLine().getErr().flush();
      return 3;
    }
  }

  static byte[] readCapped(Path path) {
    if (path == null || !Files.isRegularFile(path)) {
      throw new ContractException(
          ErrorCategory.MALFORMED_INPUT,
          "cannot read fixture '"
              + path
              + "'. Pass the path of a journey JSON file, for example"
              + " fixtures/journeys/cancel-o1.valid.json.");
    }
    try (InputStream in = Files.newInputStream(path)) {
      byte[] bytes = in.readNBytes(JourneyValidator.MAX_BYTES + 1);
      if (bytes.length > JourneyValidator.MAX_BYTES) {
        throw new ContractException(
            ErrorCategory.OVERSIZED_INPUT,
            "fixture is larger than "
                + JourneyValidator.MAX_BYTES
                + " bytes. The validator stopped before parsing.");
      }
      return bytes;
    } catch (IOException ex) {
      throw new ContractException(
          ErrorCategory.MALFORMED_INPUT,
          "cannot read fixture '" + path + "': " + ex.getMessage(),
          ex);
    }
  }
}
