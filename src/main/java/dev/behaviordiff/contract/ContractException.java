package dev.behaviordiff.contract;

/** A fixture, build, or run that must not be reported as successful coverage. */
public final class ContractException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final ErrorCategory category;

  public ContractException(ErrorCategory category, String message) {
    super(message);
    this.category = category;
  }

  public ContractException(ErrorCategory category, String message, Throwable cause) {
    super(message, cause);
    this.category = category;
  }

  public ErrorCategory category() {
    return category;
  }

  public String format() {
    return category.name() + ": " + getMessage();
  }
}
