# Changelog

## 0.1.0

First release candidate.

- Compare two checked-in demo-shop builds on the synthetic `cancel-o1` journey.
- Report the earliest changed step, row before and after values, and effect
  multiplicity for in-process shipping and email sinks.
- Ignore volatile `requestId` values, reject unknown actions, unobserved
  tables, and external sinks, and keep canonical JSON byte-stable.
- `behaviordiff demo --case duplicate-cancel` is the supported fixture demo.
