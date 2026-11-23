package dev.behaviordiff.cli;

import dev.behaviordiff.compare.Report;
import dev.behaviordiff.contract.ContractException;
import dev.behaviordiff.contract.ErrorCategory;
import dev.behaviordiff.report.CanonicalJson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes only the one report file the caller already checked. */
public final class ReportFiles {
  private ReportFiles() {}

  public static void write(Path target, Report report) {
    try {
      if (target.getParent() != null) {
        Files.createDirectories(target.getParent());
      }
      Files.write(target, CanonicalJson.write(report.toMap()));
    } catch (IOException ex) {
      throw new ContractException(
          ErrorCategory.INCOMPLETE_EVIDENCE,
          "cannot write report '" + target + "': " + ex.getMessage(),
          ex);
    }
  }
}
