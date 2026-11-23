package dev.behaviordiff.cli;

import dev.behaviordiff.contract.ContractException;
import dev.behaviordiff.contract.ErrorCategory;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes are allowed only for one file inside the working directory. */
public final class OutputGuard {
  private OutputGuard() {}

  public static Path checked(Path output) {
    if (output == null) {
      throw new ContractException(
          ErrorCategory.MALFORMED_INPUT, "missing --output. Pass a file path under the working directory.");
    }
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path target = output.toAbsolutePath().normalize();
    if (!target.startsWith(cwd) || target.equals(cwd)) {
      throw new ContractException(
          ErrorCategory.MALFORMED_INPUT,
          "output '"
              + output
              + "' is outside the working directory. Choose a path under "
              + cwd
              + ".");
    }
    if (Files.isDirectory(target)) {
      throw new ContractException(
          ErrorCategory.MALFORMED_INPUT, "output '" + output + "' is a directory. Pass a file path.");
    }
    return target;
  }
}
