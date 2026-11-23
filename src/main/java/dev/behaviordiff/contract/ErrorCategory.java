package dev.behaviordiff.contract;

/** Stable tokens printed by the CLI. Do not rename without a schema bump. */
public enum ErrorCategory {
  MALFORMED_INPUT,
  OVERSIZED_INPUT,
  UNKNOWN_ACTION,
  UNKNOWN_SEED,
  UNKNOWN_BUILD,
  UNOBSERVED_TABLE,
  EXTERNAL_SINK,
  INCOMPLETE_EVIDENCE
}
