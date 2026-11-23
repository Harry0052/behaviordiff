# Grokbot progress

| Phase | Status | Next |
| --- | --- | --- |
| P01 | PASS | P02 |
| P02 | PASS | P03 |
| P03 | PASS | P04 |
| P04 | PASS | P05 |
| P05 | PASS | P06 |
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

## P02 — Create the repository and data contracts

**Phase and status:** P02 PASS

**Files changed:** `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle.lockfile`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`, `gradlew`, `gradlew.bat`, `.gitignore`, `README.md`, `docs/data-contract.md`, `docs/project-contract.md`, `fixtures/manifest.json`, `fixtures/journeys/cancel-o1.valid.json`, `fixtures/journeys/unknown-action.invalid.json`, `fixtures/journeys/malformed.invalid.json`, `fixtures/oracle/cancel-o1.md`, `src/main/java/dev/behaviordiff/cli/BehaviorDiff.java`, `src/main/java/dev/behaviordiff/cli/ValidateCommand.java`, `src/main/java/dev/behaviordiff/contract/ContractException.java`, `src/main/java/dev/behaviordiff/contract/ErrorCategory.java`, `src/main/java/dev/behaviordiff/contract/Journey.java`, `src/main/java/dev/behaviordiff/contract/JourneyValidator.java`, `src/test/java/dev/behaviordiff/cli/ValidateCommandTest.java`, `src/test/java/dev/behaviordiff/contract/JourneyValidatorTest.java`, `docs/grokbot-progress.md`

**Commands and results:**

- `./gradlew test --write-locks --no-daemon` on Gradle 9.9.2 and OpenJDK 21.0.13. Exit 0. 10 tests passed, including valid fixture acceptance, `UNKNOWN_ACTION` for `teleport_order`, malformed JSON, oversized input, duplicate keys, and an embedded URL.
- `./gradlew installDist --no-daemon` exit 0.
- `./build/install/behaviordiff/bin/behaviordiff validate fixtures/journeys/cancel-o1.valid.json` exit 0. Stdout: `accepted fixture cancel-o1 seed seed-cancel-o1 actions 1`.
- `./build/install/behaviordiff/bin/behaviordiff validate fixtures/journeys/unknown-action.invalid.json` exit 3. Stderr begins `UNKNOWN_ACTION: action 'teleport_order'`.
- The same invalid file through `./gradlew run` prints that error and Gradle then exits 1 because the application exit is non-zero. The installed script is the public entry point.

**Artifact paths:** `fixtures/journeys/cancel-o1.valid.json`, `fixtures/journeys/unknown-action.invalid.json`, `fixtures/oracle/cancel-o1.md`, `fixtures/manifest.json`, `docs/data-contract.md`, `gradle.lockfile`

**Open issues:** none for this gate. Report JSON is specified and not produced yet.

**Next phase:** P03

## P03 — Build the working core

**Phase and status:** P03 PASS

**Files changed:** `src/main/java/dev/behaviordiff/shop/**`, `src/main/java/dev/behaviordiff/compare/**`, `src/main/java/dev/behaviordiff/report/**`, `src/main/java/dev/behaviordiff/cli/CompareCommand.java`, `src/main/java/dev/behaviordiff/cli/OutputGuard.java`, `src/main/java/dev/behaviordiff/cli/BehaviorDiff.java`, `src/main/resources/seed/seed-cancel-o1.sql`, `src/test/java/dev/behaviordiff/compare/DuplicateCancelTest.java`, `src/test/java/dev/behaviordiff/cli/CompareCommandTest.java`, `fixtures/oracle/duplicate-cancel.md`, `docs/examples/duplicate-cancel.json`, `docs/examples/identical.json`, `docs/data-contract.md`, `docs/grokbot-progress.md`

**Commands and results:**

- `./gradlew test --no-daemon` exit 0. 22 tests passed. The duplicate-cancel test checks effect counts 1 and 2 for `ship-cancel:O-1`, retained reservation `R-1`, and a byte-stable `good` versus `good` report with an empty change list. Hidden deletion, destination, amount, missing effect, null versus absent, external sink, unobserved table, and broken adapter are covered.
- `./build/install/behaviordiff/bin/behaviordiff compare --journey fixtures/journeys/cancel-o1.valid.json --left good --right duplicate-cancel --output docs/examples/duplicate-cancel.json` exit 1. Earliest step `cancel-order`, reservation `R-1` deleted on the left and retained on the right, effect cause `duplicate-idempotency-key`. The file does not contain `req-`.
- The same command with `--left good --right good --output docs/examples/identical.json` exit 0 and status `identical`.

**Artifact paths:** `docs/examples/duplicate-cancel.json`, `docs/examples/identical.json`, `fixtures/oracle/duplicate-cancel.md`

**Open issues:** none for this gate. The `demo` subcommand is the next workflow.

**Next phase:** P04

## P04 — Ship the user workflow and reports

**Phase and status:** P04 PASS

**Files changed:** `src/main/java/dev/behaviordiff/cli/DemoCommand.java`, `src/main/java/dev/behaviordiff/cli/Demos.java`, `src/main/java/dev/behaviordiff/cli/ReportFiles.java`, `src/main/java/dev/behaviordiff/cli/CompareCommand.java`, `src/main/java/dev/behaviordiff/cli/BehaviorDiff.java`, `src/test/java/dev/behaviordiff/cli/DemoCommandTest.java`, `README.md`, `docs/examples/demo-duplicate-cancel.txt`, `docs/examples/demo-approved.json`, `docs/examples/demo-partial.json`, `docs/grokbot-progress.md`

**Commands and results:**

- `./gradlew test --offline --no-daemon` exit 0. 27 tests passed, including demo inspectability, unknown case, partial tables, field approval, and help.
- `behaviordiff demo --case duplicate-cancel --output docs/examples/demo-duplicate-cancel.json` exit 1. Stdout is `docs/examples/demo-duplicate-cancel.txt`. The JSON bytes match `docs/examples/duplicate-cancel.json` (sha256 `39f4252950f23a59bf31dbdb1e653bec273d8dc9e4cdb726c8e34293939cb138`). The text names step `cancel-order`, fixture `fixtures/journeys/cancel-o1.valid.json`, and effect `ship-cancel:O-1`.
- `behaviordiff demo --case not-a-case` exit 3. Stderr starts `MALFORMED_INPUT:` and lists the known cases.
- `behaviordiff validate fixtures/journeys/malformed.invalid.json` exit 3. Stderr starts `MALFORMED_INPUT:` and says the fixture is not valid JSON.
- `behaviordiff demo --case partial-tables --output docs/examples/demo-partial.json` exit 3. `UNOBSERVED_TABLE` for `reservations`. The JSON has `"coverage": null`.
- `behaviordiff demo --case volatile-destination --approve orders.destination --output docs/examples/demo-approved.json` exit 0 and status `approved`.

**Artifact paths:** `docs/examples/demo-duplicate-cancel.txt`, `docs/examples/duplicate-cancel.json`, `docs/examples/demo-partial.json`, `docs/examples/demo-approved.json`

**Open issues:** none for this gate.

**Next phase:** P05

## P05 — Verify correctness, security, and performance

**Phase and status:** P05 PASS

**Files changed:** `build.gradle` (Spotless 8.12.0, google-java-format 1.25.2), formatted Java sources, `src/test/java/dev/behaviordiff/compare/SafeguardTest.java`, `src/test/java/dev/behaviordiff/report/CanonicalJsonTest.java`, `docs/validation.md`, `NOTICE`, `README.md`, `docs/grokbot-progress.md`

**Commands and results:**

- `./gradlew spotlessApply --no-daemon` exit 0 after pinning google-java-format 1.25.2. Version 1.37.0 failed inside Spotless on this JDK with `InvocationTargetException`.
- `./gradlew check --offline --no-daemon` exit 0. Formatter check passed. 29 tests passed, including truncation, HTML escaping, the 1000 ms comparison budget, the six mutations, the identical control, and the rejection gates.
- Three fresh `behaviordiff demo --case duplicate-cancel` runs exited 1 in 0.398 s, 0.378 s, and 0.378 s. The outputs were byte-identical to `docs/examples/duplicate-cancel.json` (sha256 `39f4252950f23a59bf31dbdb1e653bec273d8dc9e4cdb726c8e34293939cb138`). Budget is 2 seconds for that command.
- OSV query on 2026-11-23 returned zero vulnerabilities for the pinned Picocli, sqlite-jdbc, Jackson, and JUnit versions.

**Artifact paths:** `docs/validation.md`, `NOTICE`

**Open issues:** none for this gate. Hosted CI is still local-only until P07. No native sanitizer job, because this repo does not compile C.

**Next phase:** P06
