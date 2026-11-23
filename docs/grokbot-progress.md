# Grokbot progress

| Phase | Status | Next |
| --- | --- | --- |
| P01 | PASS | P02 |
| P02 | not started | |
| P03 | not started | |
| P04 | not started | |
| P05 | not started | |
| P06 | not started | |
| P07 | not started | |

## P01 — Research overlap and confirm the MVP

**Phase and status:** P01 PASS

**Files changed:** `docs/overlap.md`, `docs/project-contract.md`, `docs/research/p01_probe.py`, `docs/grokbot-progress.md`

**Commands and results:**

- `python3 docs/research/p01_probe.py` on Python 3.12.3 / SQLite 3.45.1. Exit 0, last line `PROBE_OK`. Hidden deletion: response-and-count finding null; step-aligned change count 1 for reservation `R-1`. Duplicate cancel: effect counts 1 and 2 for `ship-cancel:O-1`. Identical control change count 0 and byte-stable. Destination `dock-b` retained and `request_id` absent from that diff.
- `java -version` reports OpenJDK 21.0.13. `mvn` and `docker` are not on `PATH`, so Karate 2.1.4, WireMock 3.13.2, goldenbox, and abheeshtroy/BehaviorDiff were not executed.
- Maven metadata and GitHub repository metadata were read on 2026-11-23 for the versions and commits cited in `docs/overlap.md`.

**Artifact paths:** `docs/overlap.md`, `docs/project-contract.md`, `docs/research/p01_probe.py`

**Open issues:** traffic-diff's license file was not opened. abheeshtroy/BehaviorDiff was not run. Neither blocks the narrowed MVP, because the acceptance case is a local SQLite and in-process-sink report that those products do not ship.

**Next phase:** P02

Decision carried forward: proceed. Author the demo shop, schema, and scenarios in this repo. Pin Picocli, sqlite-jdbc, Jackson, and JUnit from Maven Central. Do not add Docker, Postgres, or a paid service.
